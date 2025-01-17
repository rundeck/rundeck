<template>
  <common-undo-redo-draggable-list
    ref="historyControls"
    v-model="model.commands"
    revert-all-enabled
    draggable-tag="ol"
    item-key="id"
    :button-label="$t('Workflow.addStep')"
    :loading="loadingWorflowSteps"
    @add-button-click="toggleAddStepModal"
  >
    <template #item="{ item: { element, index } }">
      <li>
        <div class="step-list-item">
          <div class="step-item-display">
            <div
              class="step-item-config"
              data-test="edit-step-item"
              :title="$t('Workflow.clickToEdit')"
              @click="editStepByIndex(index)"
            >
              <plugin-config
                v-if="!element.jobref"
                :service-name="
                  element.nodeStep
                    ? ServiceType.WorkflowNodeStep
                    : ServiceType.WorkflowStep
                "
                :provider="element.type"
                :config="element.config"
                :read-only="true"
                :show-title="true"
                :show-icon="true"
                :show-description="true"
                mode="show"
              >
                <template v-if="element.nodeStep" #iconSuffix>
                  <i class="fas fa-hdd"></i>
                </template>
              </plugin-config>
              <job-ref-step v-else :step="element"></job-ref-step>
            </div>

            <div v-if="element.description" class="wfstep-description">
              {{ element.description }}
            </div>

            <log-filters
              v-if="!element.jobref"
              :model-value="element.filters"
              :title="$t('Workflow.logFilters')"
              :subtitle="stepTitle(element, index)"
              :add-event="'step-action:add-logfilter:' + element.id"
              mode="inline"
              @update:model-value="
                updateHistoryWithLogFiltersData(index, $event)
              "
            />
          </div>
          <div class="step-item-controls">
            <div
              class="btn-group"
              role="group"
              :aria-label="$t('Workflow.itemControls')"
            >
              <btn
                size="xs"
                style="min-height: 21px"
                @click.stop="editStepByIndex(index)"
              >
                <i class="fas fa-edit"></i>
                {{ $t("Workflow.edit") }}
              </btn>
              <btn
                size="xs"
                :title="$t('Workflow.duplicateStep')"
                @click.stop="duplicateStep(index)"
              >
                <i class="glyphicon glyphicon-duplicate"></i>
              </btn>
              <dropdown menu-right>
                <btn size="xs" data-role="trigger"
                  ><i class="glyphicon glyphicon-cog"></i>
                  <span class="caret"></span
                ></btn>
                <template #dropdown>
                  <li>
                    <a role="button"> {{ $t("Workflow.addErrorHandler") }}</a>
                  </li>
                  <li v-if="!element.jobref">
                    <a
                      role="button"
                      data-test="add-log-filter"
                      @click="addLogFilterForIndex(element.id)"
                    >
                      {{ $t("Workflow.addLogFilter") }}
                    </a>
                  </li>
                </template>
              </dropdown>
              <btn
                size="xs"
                type="danger"
                :title="$t('Workflow.deleteThisStep')"
                data-test="remove-step"
                @click.prevent="removeStep(index)"
              >
                <i class="glyphicon glyphicon-remove"></i>
              </btn>
            </div>

            <span
              class="btn btn-xs dragHandle"
              :title="$t('Workflow.dragToReorder')"
            >
              <i class="glyphicon glyphicon-resize-vertical"></i>
            </span>
          </div>
        </div>
      </li>
    </template>
    <template #empty>
      <p data-testid="no-steps">{{ $t("Workflow.noSteps") }}</p>
    </template>
    <template #extra>
      <choose-plugin-modal
        v-model="addStepModal"
        :title="$t('Workflow.addStep')"
        :services="[ServiceType.WorkflowNodeStep, ServiceType.WorkflowStep]"
        :tab-names="[
          $t('plugin.type.WorkflowNodeStep.title.plural'),
          $t('plugin.type.WorkflowStep.title.plural'),
        ]"
        show-search
        show-divider
        @cancel="addStepModal = false"
        @selected="chooseProviderAdd"
      >
        <span class="text-info">{{ $t("Workflow.clickOnStepType") }}</span>
      </choose-plugin-modal>
      <edit-plugin-modal
        v-if="editStepModal"
        v-model:modal-active="editStepModal"
        v-model="editModel"
        data-test-id="extra-edit-modal"
        :validation="editModelValidation"
        :service-name="editService"
        :title="$t('Workflow.editStep')"
        @cancel="cancelEditStep"
        @save="saveEditStep"
      >
        <template #extra>
          <hr />
          <div class="form-horizontal">
            <div class="form-group">
              <label
                class="col-sm-2 control-label input-sm"
                for="stepDescription"
              >
                {{ $t("Workflow.stepLabel") }}
              </label>
              <div class="col-sm-10">
                <input
                  id="stepDescription"
                  v-model="editExtra.description"
                  type="text"
                  class="form-control"
                />
              </div>
            </div>
          </div>
        </template>
      </edit-plugin-modal>
      <job-ref-form
        v-if="editJobRefModal"
        v-model="editModel"
        v-model:modal-active="editJobRefModal"
        @cancel="cancelEditStep"
      />
    </template>
  </common-undo-redo-draggable-list>
</template>
<script lang="ts">
import {
  commandsToEditData,
  editCommandsToStepsData,
  mkid,
} from "@/app/components/job/workflow/types/workflowFuncs";
import {
  EditStepData,
  StepsData,
  StepsEditData,
} from "@/app/components/job/workflow/types/workflowTypes";
import { getRundeckContext } from "@/library";
import ChoosePluginModal from "@/library/components/plugins/ChoosePluginModal.vue";
import EditPluginModal from "@/library/components/plugins/EditPluginModal.vue";
import pluginConfig from "@/library/components/plugins/pluginConfig.vue";
import { ServiceType } from "@/library/stores/Plugins";
import JobRefStep from "@/app/components/job/workflow/JobRefStep.vue";
import { cloneDeep } from "lodash";
import { defineComponent } from "vue";
import LogFilters from "./LogFilters.vue";
import CommonUndoRedoDraggableList from "@/app/components/common/CommonUndoRedoDraggableList.vue";
import { Operation } from "@/app/components/job/options/model/ChangeEvents";
import { validatePluginConfig } from "@/library/modules/pluginService";
import JobRefForm from "@/app/components/job/workflow/JobRefForm.vue";

const context = getRundeckContext();
const eventBus = context.eventBus;
export default defineComponent({
  name: "WorkflowSteps",
  components: {
    JobRefForm,
    CommonUndoRedoDraggableList,
    EditPluginModal,
    pluginConfig,
    ChoosePluginModal,
    JobRefStep,
    LogFilters,
  },
  props: {
    modelValue: {
      type: Object,
      required: true,
      default: () => ({}) as StepsData,
    },
  },
  emits: ["update:modelValue"],
  data() {
    return {
      ServiceType,
      eventBus,
      model: {} as StepsEditData,
      workflowStepPlugins: [],
      workflowNodeStepPlugins: [],
      addStepModal: false,
      editStepModal: false,
      editJobRefModal: false,
      editModel: {} as EditStepData,
      editExtra: {} as EditStepData,
      editModelValidation: { errors: [], valid: true },
      editService: null,
      editIndex: -1,
      loadingWorflowSteps: false,
    };
  },
  watch: {
    model: {
      handler() {
        this.$emit("update:modelValue", editCommandsToStepsData(this.model));
      },
      deep: true,
    },
  },
  async mounted() {
    try {
      this.loadingWorflowSteps = true;
      await this.getStepPlugins();
      this.model = commandsToEditData(this.modelValue);
    } finally {
      this.loadingWorflowSteps = false;
    }
  },
  methods: {
    chooseProviderAdd({
      service,
      provider,
    }: {
      service: string;
      provider: string;
    }) {
      this.addStepModal = false;
      this.editModel = {
        type: provider,
        config: {},
        nodeStep: service === ServiceType.WorkflowNodeStep,
      };
      this.editExtra = {};
      this.editIndex = -1;
      this.editService = service;
      this.editStepModal = true;
    },
    cancelEditStep() {
      this.editStepModal = false;
      this.editJobRefModal = false;
      this.editModel = {};
      this.editExtra = {};
      this.editModelValidation = null;
      this.editIndex = -1;
    },
    removeStep(index: number) {
      const commandToDelete = cloneDeep(this.model.commands[index]);
      this.$refs.historyControls.operationRemove(index);
      this.$refs.historyControls.changeEvent({
        index,
        dest: -1,
        value: commandToDelete,
        operation: Operation.Remove,
        undo: Operation.Insert,
      });
    },
    async addLogFilterForIndex(id: string) {
      eventBus.emit("step-action:add-logfilter:" + id);
    },
    duplicateStep(index: number) {
      const command = cloneDeep(this.model.commands[index]);
      command.id = mkid();
      this.$refs.historyControls.operationInsert(index + 1, command);
      this.$refs.historyControls.changeEvent({
        index: 0,
        dest: -1,
        value: command,
        operation: Operation.Insert,
        undo: Operation.Remove,
      });
    },
    editStepByIndex(index: number) {
      this.editIndex = index;
      const command = this.model.commands[index];
      this.editModel = cloneDeep(command);
      this.editExtra = cloneDeep(command);

      this.editService = command.nodeStep
        ? ServiceType.WorkflowNodeStep
        : ServiceType.WorkflowStep;

      if (command.jobref) {
        this.editJobRefModal = true;
      } else {
        this.editStepModal = true;
      }
    },
    async saveEditStep() {
      try {
        const saveData = cloneDeep(this.editExtra);
        saveData.type = this.editModel.type;
        saveData.config = this.editModel.config;
        saveData.id = mkid();
        saveData.nodeStep = this.editService === ServiceType.WorkflowNodeStep;
        if (!saveData.jobref) {
          saveData.filters = [];
        }

        const response = await validatePluginConfig(
          ServiceType.WorkflowNodeStep,
          saveData.type,
          saveData.config,
        );

        if (response.valid && Object.keys(response.errors || {}).length === 0) {
          const dataForUpdatingHistory = {
            index: this.model.commands.length,
            operation: Operation.Insert,
            undo: Operation.Remove,
            orig: undefined,
          };
          if (this.editIndex >= 0) {
            const originalData = this.model.commands[this.editIndex];
            this.$refs.historyControls.operationModify(
              this.editIndex,
              saveData,
            );
            dataForUpdatingHistory.index = this.editIndex;
            dataForUpdatingHistory.operation = Operation.Modify;
            dataForUpdatingHistory.undo = Operation.Modify;
            dataForUpdatingHistory.orig = originalData;
          } else {
            this.$refs.historyControls.operationInsert(
              this.model.commands.length,
              saveData,
            );
          }

          this.$refs.historyControls.changeEvent({
            dest: -1,
            value: saveData,
            ...dataForUpdatingHistory,
          });

          this.editStepModal = false;
          this.editJobRefModal = false;
          this.editModel = {};
          this.editExtra = {};
          this.editModelValidation = null;
          this.editIndex = -1;
        } else {
          this.editModelValidation = response;
        }
      } catch (e) {
        console.log(e);
      }
    },
    async getStepPlugins() {
      await context.rootStore.plugins.load(ServiceType.WorkflowNodeStep);
      await context.rootStore.plugins.load(ServiceType.WorkflowStep);
      this.workflowStepPlugins = context.rootStore.plugins.getServicePlugins(
        ServiceType.WorkflowStep,
      );
      this.workflowNodeStepPlugins =
        context.rootStore.plugins.getServicePlugins(
          ServiceType.WorkflowNodeStep,
        );
    },
    stepTitle(step: StepsEditData, index: number) {
      if (step.description) {
        return `Step ${index + 1} "` + step.description + '"';
      }
      return `Step ${index + 1}`;
    },
    toggleAddStepModal() {
      this.addStepModal = !this.addStepModal;
    },
    updateHistoryWithLogFiltersData(index: number, data: any) {
      const command = cloneDeep(this.model.commands[index]);
      command.filters = cloneDeep(data);
      const orig = this.$refs.historyControls.operationModify(index, command);

      this.$refs.historyControls.changeEvent({
        index: index,
        dest: -1,
        orig: orig,
        value: command,
        operation: Operation.Modify,
        undo: Operation.Modify,
      });
    },
  },
});
</script>
<style lang="scss">
.mb-10 {
  margin-bottom: 10px;
}
.step-list-item {
  background: var(--card-default-background-color);
  border: 1px solid var(--list-item-border-color);
  padding: 10px !important;
  margin-bottom: 10px;
  border-radius: 5px;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: flex-start;

  .step-item-display {
    flex-grow: 1;
    padding: 5px;
    .step-item-config {
      border-width: 1px;
      border-style: dotted;
      border-color: transparent;

      &:hover {
        cursor: pointer;
        background-color: var(--light-gray);
        border-color: #68b3c8;
      }

      .fas {
        margin-left: 5px;
      }

      .configpair > span {
        display: flex;
        gap: 5px;
      }

      span[data-testid="block-description"] {
        margin-left: 5px;
      }
    }
  }
}
</style>
