<template>
  <div class="inner-step-list">
    <draggable
      v-model="commands"
      tag="ol"
      item-key="id"
      handle=".inner-drag-handle"
      @end="emitUpdate"
    >
      <template #item="{ element, index }">
        <li>
          <i class="pi pi-bars inner-drag-handle"></i>
          <div class="inner-step-item">
            <StepCard
              v-if="editingStepId !== element.id && element.type !== 'conditional.logic'"
              :plugin-details="getPluginDetails(element)"
              :config="element"
              :service-name="targetService"
              :show-toggle="true"
              :log-filters="element.jobref ? [] : (element.filters || [])"
              :error-handler="element.errorhandler ? [element.errorhandler] : []"
              @add-log-filter="!element.jobref && addLogFilter(element.id)"
              @add-error-handler="toggleAddErrorHandlerModal(index)"
              @update:log-filters="!element.jobref && updateLogFilters(index, $event)"
              @edit-log-filter="!element.jobref && handleEditLogFilterFromChip(element.id, $event)"
              @edit-error-handler="editErrorHandler(index)"
              @remove-error-handler="removeErrorHandler(index)"
              @delete="removeStep(index)"
              @duplicate="duplicateStep(index)"
              @edit="editStep(index)"
            />
            <log-filters
              v-if="editingStepId !== element.id && !element.jobref && element.type !== 'conditional.logic'"
              :model-value="element.filters || []"
              :title="$t('Workflow.logFilters')"
              :subtitle="stepTitle(element, index)"
              :add-event="'inner-step-action:add-logfilter:' + element.id"
              :edit-event="'inner-step-action:edit-logfilter:' + element.id"
              :conditional-enabled="true"
              @update:model-value="updateLogFilters(index, $event)"
            />
            <ConditionalCard
              v-if="editingStepId !== element.id && element.type === 'conditional.logic'"
              :plugin-details="getPluginDetails(element)"
              :config="element"
              :service-name="targetService"
              :show-toggle="true"
              @delete="removeStep(index)"
              @duplicate="duplicateStep(index)"
              @edit="editStep(index)"
            />
            <EditStepCard
              v-if="editingStepId === element.id"
              v-model="editModel"
              :plugin-details="getPluginDetails(editModel)"
              :service-name="targetService"
              :validation="editModelValidation"
              :extra-autocomplete-vars="extraAutocompleteVars"
              :show-navigation="true"
              :depth="depth"
              @save="handleSaveStep(index)"
              @cancel="handleCancelEdit"
            />
          </div>
        </li>
      </template>
    </draggable>

    <PtButton
      outlined
      severity="secondary"
      icon="pi pi-plus"
      :label="$t('editConditionalStep.addConditionStep')"
      class="btn-add-inner-step"
      @click="openAddStepModal"
    />

    <ChoosePluginsEAModal
      v-model="addStepModal"
      :title="isErrorHandler
        ? $t('Workflow.addErrorHandler')
        : $t('editConditionalStep.addConditionStep')"
      :services="[targetService]"
      :tab-names="[targetService === ServiceType.WorkflowNodeStep
        ? $t('plugin.type.WorkflowNodeStep.title.plural')
        : $t('plugin.type.WorkflowStep.title.plural')]"
      :prevent-service-swap="true"
      :target-service="targetService"
      :exclude-providers="excludedProviders"
      show-search
      show-divider
      @cancel="cancelProviderAdd"
      @selected="chooseProviderAdd"
    />

    <EditPluginModal
      v-if="editErrorHandlerModal"
      v-model="errorHandlerEditModel"
      :title="$t('Workflow.editErrorHandler')"
      :service-name="targetService"
      :validation="editModelValidation"
      :modal-active="editErrorHandlerModal"
      :extra-autocomplete-vars="extraAutocompleteVars"
      @update:modal-active="editErrorHandlerModal = $event"
      @cancel="cancelErrorHandlerEdit"
      @save="saveErrorHandler"
    >
      <template #extra>
        <div class="presentation checkbox">
          <input
            id="innerKeepgoingOnSuccess"
            v-model="errorHandlerExtra.keepgoingOnSuccess"
            type="checkbox"
          />
          <label for="innerKeepgoingOnSuccess">
            {{ $t("Workflow.stepErrorHandler.keepgoingOnSuccess.label") }}
            <span>
              {{ $t("Workflow.stepErrorHandler.keepgoingOnSuccess.description") }}
            </span>
          </label>
        </div>
      </template>
    </EditPluginModal>
  </div>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";
import draggable from "vuedraggable";
import { cloneDeep } from "lodash";
import StepCard from "@/library/components/primeVue/StepCards/StepCard.vue";
import ConditionalCard from "@/library/components/primeVue/StepCards/ConditionalCard.vue";
import EditStepCard from "./EditStepCard.vue";
import LogFilters from "./LogFilters.vue";
import ChoosePluginsEAModal from "@/library/components/plugins/ChoosePluginsEAModal.vue";
import EditPluginModal from "@/library/components/plugins/EditPluginModal.vue";
import PtButton from "@/library/components/primeVue/PtButton/PtButton.vue";
import { ServiceType } from "@/library/stores/Plugins";
import { getRundeckContext } from "@/library";
import { mkid } from "./types/workflowFuncs";
import type { EditStepData } from "./types/workflowTypes";
import type { ContextVariable } from "@/library/stores/contextVariables";
import {
  createStepFromProvider,
  getPluginDetailsForStep,
  validateStepForSave,
} from "./stepEditorUtils";
import { validatePluginConfig } from "@/library/modules/pluginService";

const eventBus = getRundeckContext().eventBus;

export default defineComponent({
  name: "InnerStepList",
  components: {
    draggable,
    StepCard,
    ConditionalCard,
    EditStepCard,
    LogFilters,
    ChoosePluginsEAModal,
    EditPluginModal,
    PtButton,
  },
  props: {
    modelValue: {
      type: Array as PropType<EditStepData[]>,
      required: true,
      default: () => [],
    },
    targetService: {
      type: String,
      required: true,
    },
    depth: {
      type: Number,
      default: 1,
    },
    extraAutocompleteVars: {
      type: Array as PropType<ContextVariable[]>,
      required: false,
      default: () => [],
    },
  },
  emits: ["update:modelValue"],
  data() {
    return {
      ServiceType,
      commands: [] as EditStepData[],
      addStepModal: false,
      isErrorHandler: false,
      editingStepId: null as string | null,
      editModel: {} as EditStepData,
      editModelValidation: { errors: {}, valid: true } as any,
      editIndex: -1,
      // Error handler editing
      editErrorHandlerModal: false,
      errorHandlerEditModel: {} as any,
      errorHandlerExtra: { keepgoingOnSuccess: false },
      errorHandlerIndex: -1,
    };
  },
  computed: {
    excludedProviders(): string[] {
      if (this.depth >= 2 || this.isErrorHandler) {
        return ["conditional.logic"];
      }
      return [];
    },
  },
  watch: {
    modelValue: {
      handler(val) {
        if (val) {
          this.commands = val;
        }
      },
      immediate: true,
      deep: true,
    },
  },
  methods: {
    getPluginDetails(element: EditStepData) {
      return getPluginDetailsForStep(element, this.targetService);
    },
    stepTitle(step: EditStepData, index: number) {
      if (step.description) {
        return `Step ${index + 1} "` + step.description + '"';
      }
      return `Step ${index + 1}`;
    },

    // --- Add step flow ---
    openAddStepModal() {
      this.isErrorHandler = false;
      this.addStepModal = true;
    },
    cancelProviderAdd() {
      this.addStepModal = false;
      this.isErrorHandler = false;
    },
    chooseProviderAdd({ service, provider }: { service: string; provider: string }) {
      this.addStepModal = false;

      if (this.isErrorHandler) {
        this.handleErrorHandlerProviderSelected(service, provider);
        return;
      }

      const newStep = createStepFromProvider(this.targetService, provider);
      this.editModel = newStep;
      this.editIndex = this.commands.length;
      this.editingStepId = newStep.id;
      this.commands.push(newStep);
      this.emitUpdate();
    },

    // --- Edit step flow ---
    editStep(index: number) {
      const command = this.commands[index];
      this.editIndex = index;
      this.editingStepId = command.id;
      this.editModel = cloneDeep(command);
      this.editModelValidation = { errors: {}, valid: true };
    },

    async handleSaveStep(index: number) {
      try {
        const saveData = cloneDeep(this.editModel);
        if (!saveData.id) {
          saveData.id = this.commands[index]?.id || mkid();
        }

        const result = await validateStepForSave(saveData, this.targetService);

        if (result.valid) {
          this.commands[index] = saveData;
          this.clearEdit();
          this.emitUpdate();
        } else {
          if (result.errors.jobref) {
            this.editModelValidation = {
              valid: false,
              errors: { jobref: this.$t(result.errors.jobref) },
            };
          } else {
            this.editModelValidation = {
              valid: false,
              errors: result.errors,
            };
          }
        }
      } catch (e) {
        console.error("Error saving inner step:", e);
      }
    },

    handleCancelEdit() {
      if (this.editIndex >= 0 && this.editIndex === this.commands.length - 1) {
        const lastStep = this.commands[this.editIndex];
        if (lastStep && lastStep.id === this.editingStepId) {
          const isEmpty = lastStep.type === "conditional.logic"
            ? (!lastStep.config || !lastStep.config.conditionSets || lastStep.config.conditionSets.length === 0)
            : lastStep.jobref
              ? !lastStep.jobref.name && !lastStep.jobref.uuid
              : (!lastStep.config || Object.keys(lastStep.config || {}).length === 0);

          if (isEmpty) {
            this.commands.splice(this.editIndex, 1);
            this.emitUpdate();
          }
        }
      }
      this.clearEdit();
    },

    clearEdit() {
      this.editingStepId = null;
      this.editModel = {} as EditStepData;
      this.editModelValidation = { errors: {}, valid: true };
      this.editIndex = -1;
    },

    // --- Delete / Duplicate ---
    removeStep(index: number) {
      this.commands.splice(index, 1);
      this.emitUpdate();
    },

    duplicateStep(index: number) {
      const command = cloneDeep(this.commands[index]);
      command.id = mkid();
      command.nodeStep = this.targetService === ServiceType.WorkflowNodeStep;
      this.commands.splice(index + 1, 0, command);
      this.emitUpdate();
    },

    // --- Log Filters ---
    addLogFilter(stepId: string) {
      eventBus.emit("inner-step-action:add-logfilter:" + stepId);
    },

    handleEditLogFilterFromChip(stepId: string, data: { filter: any; index: number }) {
      eventBus.emit("inner-step-action:edit-logfilter:" + stepId, data.index);
    },

    updateLogFilters(index: number, data: any) {
      const command = cloneDeep(this.commands[index]);
      command.filters = cloneDeep(data);
      this.commands[index] = command;
      this.emitUpdate();
    },

    // --- Error Handler ---
    toggleAddErrorHandlerModal(index: number) {
      this.isErrorHandler = true;
      this.errorHandlerIndex = index;
      this.addStepModal = true;
    },

    handleErrorHandlerProviderSelected(service: string, provider: string) {
      this.isErrorHandler = false;
      this.errorHandlerEditModel = {
        type: provider,
        config: {},
        nodeStep: service === ServiceType.WorkflowNodeStep,
      };
      this.errorHandlerExtra = { keepgoingOnSuccess: false };
      this.editErrorHandlerModal = true;
    },

    editErrorHandler(index: number) {
      const command = this.commands[index];
      if (!command.errorhandler) {
        console.warn("Cannot edit error handler: it does not exist");
        return;
      }
      this.errorHandlerIndex = index;
      this.errorHandlerEditModel = cloneDeep(command.errorhandler);
      this.errorHandlerExtra = {
        keepgoingOnSuccess: command.errorhandler.keepgoingOnSuccess || false,
      };
      this.editErrorHandlerModal = true;
    },

    removeErrorHandler(index: number) {
      const command = cloneDeep(this.commands[index]);
      delete command.errorhandler;
      this.commands[index] = command;
      this.emitUpdate();
    },

    async saveErrorHandler() {
      try {
        const handlerData = {
          ...this.errorHandlerEditModel,
          keepgoingOnSuccess: this.errorHandlerExtra.keepgoingOnSuccess,
          id: mkid(),
        };

        if (!handlerData.jobref) {
          const response = await validatePluginConfig(
            this.targetService,
            handlerData.type,
            handlerData.config || {},
          );

          if (!response.valid || Object.keys(response.errors || {}).length > 0) {
            this.editModelValidation = response;
            return;
          }
        }

        const command = cloneDeep(this.commands[this.errorHandlerIndex]);
        command.errorhandler = handlerData;
        this.commands[this.errorHandlerIndex] = command;
        this.cancelErrorHandlerEdit();
        this.emitUpdate();
      } catch (e) {
        console.error("Error saving error handler:", e);
      }
    },

    cancelErrorHandlerEdit() {
      this.editErrorHandlerModal = false;
      this.errorHandlerEditModel = {};
      this.errorHandlerExtra = { keepgoingOnSuccess: false };
      this.errorHandlerIndex = -1;
      this.editModelValidation = { errors: {}, valid: true };
    },

    // --- Emit changes ---
    emitUpdate() {
      this.$emit("update:modelValue", this.commands);
    },
  },
});
</script>

<style lang="scss" scoped>
.inner-step-list {
  display: flex;
  flex-direction: column;
  gap: var(--sizes-4);

  ol {
    display: flex;
    flex-direction: column;
    gap: var(--sizes-4);
    list-style: none;
    padding: 0;
    margin: 0;
    position: relative;
    counter-reset: inner-list;

    li {
      position: relative;
      padding-left: var(--sizes-6);

      &::before {
        counter-increment: inner-list;
        content: counter(inner-list) ".";
        display: block;
        font-size: 16px;
        font-family: Inter, var(--fonts-body);
        left: 0;
        position: absolute;
        top: 14px;
      }

      .inner-drag-handle {
        position: absolute;
        cursor: grab;
        left: -18px;
        top: 16px;
        color: var(--colors-gray-400);
        font-size: 14px;
      }
    }
  }

  .inner-step-item {
    width: 100%;
  }

  .btn-add-inner-step {
    align-self: flex-start;
    padding: 5px 9px;
    font-size: 12px;
    border-color: var(--colors-gray-600);
    color: var(--colors-gray-800);

    &:hover {
      background: var(--colors-gray-100);
      border-color: var(--colors-gray-800);
    }

    :deep(.pi) {
      font-size: 12px;
    }
  }
}
</style>
