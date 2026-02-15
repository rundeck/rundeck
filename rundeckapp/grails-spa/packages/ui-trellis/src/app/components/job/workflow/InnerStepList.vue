<template>
  <div class="inner-step-list">
    <draggable
      v-model="commands"
      tag="ol"
      item-key="id"
      handle=".inner-drag-handle"
      :disabled="!!editingStepId"
      @end="emitUpdate"
    >
      <template #item="{ element, index }">
        <li>
          <i class="pi pi-bars inner-drag-handle"></i>
          <div
            class="inner-step-item"
            :class="{ 'edit-lock-disabled': editingStepId && editingStepId !== element.id }"
          >
            <StepCard
              v-if="editingStepId !== element.id && element.type !== 'conditional.logic'"
              :plugin-details="getPluginDetails(element)"
              :config="element"
              :service-name="targetService"
              :show-toggle="true"
              :initially-expanded="false"
              :log-filters="element.jobref ? [] : (element.filters || [])"
              :error-handler="element.errorhandler ? [element.errorhandler] : []"
              :disabled="!!editingStepId"
              @add-log-filter="!element.jobref && addLogFilter($event || element.id)"
              @add-error-handler="toggleAddErrorHandlerModal($event || element.id)"
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
              :add-event="logFilterAddEvent(element.id)"
              :edit-event="logFilterEditEvent(element.id)"
              :conditional-enabled="true"
              @update:model-value="updateLogFilters(index, $event)"
            />
            <ConditionalCard
              v-if="editingStepId !== element.id && element.type === 'conditional.logic'"
              :plugin-details="getPluginDetails(element)"
              :config="element"
              :service-name="targetService"
              :show-toggle="true"
              :initially-expanded="false"
              :disabled="!!editingStepId"
              @add-log-filter="addLogFilter($event)"
              @add-error-handler="toggleAddErrorHandlerModalForNestedStep($event, index)"
              @edit-log-filter="handleEditLogFilterFromChip($event.stepId, $event)"
              @edit-error-handler="editErrorHandlerByStepId($event, index)"
              @remove-error-handler="removeErrorHandlerByStepId($event, index)"
              @update:log-filters="updateLogFiltersForNestedStep($event, index)"
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
      :label="addStepLabel"
      class="btn-add-inner-step"
      :disabled="!!editingStepId"
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

    <Teleport to="body">
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
    </Teleport>
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
  emits: ["update:modelValue", "update:editing"],
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
      errorHandlerStepId: null as string | null,
      editingNestedStepPath: null as {
        conditionalIndex: number;
        stepIndex: number;
        nestedStepIndex?: number;
      } | null,
    };
  },
  computed: {
    excludedProviders(): string[] {
      if (this.depth >= 2 || this.isErrorHandler) {
        return ["conditional.logic"];
      }
      return [];
    },
    addStepLabel(): string {
      return this.depth >= 2
        ? this.$t("editConditionalStep.addNestedConditionStep")
        : this.$t("editConditionalStep.addConditionStep");
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
    editingStepId: {
      handler(val) {
        this.$emit("update:editing", !!val);
      },
      immediate: true,
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
      if (this.editingStepId) return;

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
      if (this.editingStepId) return;

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
      if (this.editingStepId) return;

      this.commands.splice(index, 1);
      this.emitUpdate();
    },

    duplicateStep(index: number) {
      if (this.editingStepId) return;

      const command = cloneDeep(this.commands[index]);
      command.id = mkid();
      command.nodeStep = this.targetService === ServiceType.WorkflowNodeStep;
      this.commands.splice(index + 1, 0, command);
      this.emitUpdate();
    },

    // --- Log Filters ---
    logFilterAddEvent(stepId: string): string {
      return `inner-step-action:add-logfilter:${this.depth}:${stepId}`;
    },
    logFilterEditEvent(stepId: string): string {
      return `inner-step-action:edit-logfilter:${this.depth}:${stepId}`;
    },
    addLogFilter(stepId: string) {
      if (this.editingStepId) return;

      eventBus.emit(this.logFilterAddEvent(stepId));
    },

    handleEditLogFilterFromChip(stepId: string, data: { filter: any; index: number }) {
      if (this.editingStepId) return;

      eventBus.emit(this.logFilterEditEvent(stepId), data.index);
    },

    updateLogFilters(index: number, data: any) {
      if (this.editingStepId) return;

      const command = cloneDeep(this.commands[index]);
      command.filters = cloneDeep(data);
      this.commands[index] = command;
      this.emitUpdate();
    },

    // --- Error Handler ---
    toggleAddErrorHandlerModal(stepId: string) {
      if (this.editingStepId) return;

      this.editingNestedStepPath = null;
      this.isErrorHandler = true;
      this.errorHandlerStepId = stepId;
      this.addStepModal = true;
    },

    toggleAddErrorHandlerModalForNestedStep(stepId: string, conditionalIndex: number) {
      if (this.editingStepId) return;

      const path = this.findNestedStepPath(conditionalIndex, stepId);
      if (!path) return;

      const conditional = this.commands[path.conditionalIndex];
      const step = path.nestedStepIndex !== undefined
        ? conditional.config.commands[path.stepIndex].config.commands[path.nestedStepIndex]
        : conditional.config.commands[path.stepIndex];

      this.editingNestedStepPath = path;
      this.isErrorHandler = true;
      this.errorHandlerStepId = stepId;
      this.editExtra = cloneDeep(step);
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
      this.editingNestedStepPath = null;
      this.errorHandlerStepId = command.id;
      this.errorHandlerEditModel = cloneDeep(command.errorhandler);
      this.errorHandlerExtra = {
        keepgoingOnSuccess: command.errorhandler.keepgoingOnSuccess || false,
      };
      this.editErrorHandlerModal = true;
    },

    findNestedStepPath(
      conditionalIndex: number,
      stepId: string,
    ): { conditionalIndex: number; stepIndex: number; nestedStepIndex?: number } | null {
      const conditional = this.commands[conditionalIndex];
      if (!conditional?.config?.commands) return null;

      const directIndex = conditional.config.commands.findIndex(
        (c: EditStepData) => c.id === stepId,
      );
      if (directIndex >= 0) {
        return { conditionalIndex, stepIndex: directIndex };
      }

      for (let i = 0; i < conditional.config.commands.length; i++) {
        const step = conditional.config.commands[i];
        if (step.type === "conditional.logic" && step.config?.commands) {
          const nestedIndex = step.config.commands.findIndex(
            (c: EditStepData) => c.id === stepId,
          );
          if (nestedIndex >= 0) {
            return { conditionalIndex, stepIndex: i, nestedStepIndex: nestedIndex };
          }
        }
      }
      return null;
    },

    editErrorHandlerByStepId(stepId: string, conditionalIndex: number) {
      const path = this.findNestedStepPath(conditionalIndex, stepId);
      if (!path) return;

      const conditional = this.commands[path.conditionalIndex];
      const step = path.nestedStepIndex !== undefined
        ? conditional.config.commands[path.stepIndex].config.commands[path.nestedStepIndex]
        : conditional.config.commands[path.stepIndex];

      if (!step.errorhandler) return;

      this.editingNestedStepPath = path;
      this.errorHandlerStepId = stepId;
      this.errorHandlerEditModel = cloneDeep(step.errorhandler);
      this.errorHandlerExtra = {
        keepgoingOnSuccess: step.errorhandler.keepgoingOnSuccess || false,
      };
      this.editErrorHandlerModal = true;
    },

    removeErrorHandlerByStepId(stepId: string, conditionalIndex: number) {
      const path = this.findNestedStepPath(conditionalIndex, stepId);
      if (!path) return;

      const conditional = cloneDeep(this.commands[path.conditionalIndex]);
      const step = path.nestedStepIndex !== undefined
        ? conditional.config.commands[path.stepIndex].config.commands[path.nestedStepIndex]
        : conditional.config.commands[path.stepIndex];

      delete step.errorhandler;
      this.commands[path.conditionalIndex] = conditional;
      this.emitUpdate();
    },

    updateLogFiltersForNestedStep(
      payload: { stepId: string; filters: any },
      conditionalIndex: number,
    ) {
      const path = this.findNestedStepPath(conditionalIndex, payload.stepId);
      if (!path) return;

      const conditional = cloneDeep(this.commands[path.conditionalIndex]);
      const step = path.nestedStepIndex !== undefined
        ? conditional.config.commands[path.stepIndex].config.commands[path.nestedStepIndex]
        : conditional.config.commands[path.stepIndex];

      step.filters = cloneDeep(payload.filters);
      this.commands[path.conditionalIndex] = conditional;
      this.emitUpdate();
    },

    removeErrorHandler(index: number) {
      const command = cloneDeep(this.commands[index]);
      delete command.errorhandler;
      this.commands[index] = command;
      this.emitUpdate();
    },

    async saveErrorHandler() {
      try {
        if (!this.errorHandlerStepId) {
          console.error("Cannot save error handler: no step ID");
          return;
        }

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

        if (this.editingNestedStepPath) {
          const path = this.editingNestedStepPath;
          const conditional = cloneDeep(this.commands[path.conditionalIndex]);
          const step = path.nestedStepIndex !== undefined
            ? conditional.config.commands[path.stepIndex].config.commands[path.nestedStepIndex]
            : conditional.config.commands[path.stepIndex];

          step.errorhandler = handlerData;
          this.commands[path.conditionalIndex] = conditional;
        } else {
          const index = this.commands.findIndex((c) => c.id === this.errorHandlerStepId);
          if (index === -1) {
            console.error("Cannot save error handler: step not found", this.errorHandlerStepId);
            this.cancelErrorHandlerEdit();
            return;
          }

          const command = cloneDeep(this.commands[index]);
          command.errorhandler = handlerData;
          this.commands[index] = command;
        }

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
      this.errorHandlerStepId = null;
      this.editingNestedStepPath = null;
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
    margin-left: 47px;
    position: relative;
    counter-reset: inner-step-counter 0 !important;

    li {
      position: relative;
      //padding-left: var(--sizes-6);

      &::before {
        counter-increment: inner-step-counter !important;
        content: counter(inner-step-counter) "." !important;
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

    &.edit-lock-disabled {
      filter: grayscale(0.3);
    }
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
