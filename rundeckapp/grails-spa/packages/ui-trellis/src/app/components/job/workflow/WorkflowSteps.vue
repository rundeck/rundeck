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
            <div class="step-item-row">
              <div
                :id="`wfitem_${index}`"
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
                    <i class="fas fa-hdd node-icon"></i>
                  </template>
                </plugin-config>
                <job-ref-step v-else :step="element"></job-ref-step>
                <div v-if="element.description" class="wfstep-description">
                  {{ element.description }}
                </div>
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
                    <btn size="xs" data-role="trigger">
                      <i class="glyphicon glyphicon-cog"></i>
                      <span class="caret"></span>
                    </btn>
                    <template #dropdown>
                      <li v-if="!element.errorhandler">
                        <a
                          role="button"
                          data-test="add-error-handler"
                          @click="toggleAddErrorHandlerModal(index)"
                        >
                          {{ $t("Workflow.addErrorHandler") }}
                        </a>
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
                  <i class="glyphicon glyphicon-resize-vertical" />
                </span>
              </div>
            </div>

            <div
              v-if="!element.jobref && element.filters.length > 0"
              class="step-item-logfilters"
            >
              <log-filters
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
            <error-handler-step
              v-if="element.errorhandler"
              :step="element"
              @edit="editStepByIndex(index, true)"
              @removeHandler="removeStep(index, true)"
            />
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
        :title="
          isErrorHandler
            ? $t('Workflow.addErrorHandler')
            : $t('Workflow.addStep')
        "
        :services="[ServiceType.WorkflowNodeStep, ServiceType.WorkflowStep]"
        :tab-names="[
          $t('plugin.type.WorkflowNodeStep.title.plural'),
          $t('plugin.type.WorkflowStep.title.plural'),
        ]"
        show-search
        show-divider
        @cancel="cancelProviderAdd"
        @selected="chooseProviderAdd"
      >
        <template v-if="isErrorHandler" #listHeader="{ service }">
          <div class="list-group-item">
            <p class="list-group-heading text-info text-strong">
              {{ $t(`framework.service.${service}.description`) }}
            </p>
          </div>
        </template>
        <span class="text-info">
          {{
            isErrorHandler
              ? $t("Workflow.errorHandlerDescription")
              : $t("Workflow.clickOnStepType")
          }}
        </span>
      </choose-plugin-modal>
      <component
        v-if="editStepModal || (editJobRefModal && modalComponent)"
        :is="modalComponent"
        v-model="editModel"
        v-bind="modalAttributes"
        @cancel="cancelEditStep"
        @save="saveEditStep"
      >
        <template #extra>
          <template v-if="!isErrorHandler">
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
          <template v-else>
            <div class="presentation checkbox">
              <input
                name="keepgoingOnSuccess"
                id="keepgoingOnSuccess"
                v-model="editExtra.errorhandler.keepgoingOnSuccess"
                type="checkbox"
              />
              <label for="keepgoingOnSuccess">
                {{ $t("Workflow.stepErrorHandler.keepgoingOnSuccess.label") }}

                <span>
                  {{
                    $t(
                      "Workflow.stepErrorHandler.keepgoingOnSuccess.description",
                    )
                  }}
                </span>
              </label>
            </div>
          </template>
        </template>
      </component>
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
import ErrorHandlerStep from "@/app/components/job/workflow/ErrorHandlerStep.vue";

const context = getRundeckContext();
const eventBus = context.eventBus;
export default defineComponent({
  name: "WorkflowSteps",
  components: {
    ErrorHandlerStep,
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
      isErrorHandler: false,
      editJobRefModal: false,
      editModel: {} as EditStepData,
      editExtra: {} as EditStepData,
      editModelValidation: { errors: [], valid: true },
      editService: null,
      editIndex: -1,
      loadingWorflowSteps: false,
    };
  },
  computed: {
    modalAttributes() {
      if (this.editStepModal) {
        return {
          title: this.isErrorHandler
            ? this.$t("Workflow.editErrorHandler")
            : this.$t("Workflow.editStep"),
          serviceName: this.editService,
          validation: this.editModelValidation,
          "data-testid": "extra-edit-modal",
          modalActive: this.editStepModal,
          "onUpdate:modalActive": this.toggleModalActive,
        };
      } else if (this.editJobRefModal) {
        return {
          "data-test-id": "jobref-modal",
          modalActive: this.editJobRefModal,
          "onUpdate:modalActive": this.toggleModalActive,
        };
      }
      return {};
    },
    modalComponent() {
      if (!this.editJobRefModal && !this.editStepModal) {
        return "";
      }
      return this.editStepModal ? "edit-plugin-modal" : "job-ref-form";
    },
  },
  watch: {
    model: {
      handler() {
        this.$emit("update:modelValue", editCommandsToStepsData(this.model));
        this.notify();
      },
      deep: true,
    },
  },
  async mounted() {
    try {
      this.loadingWorflowSteps = true;
      await this.getStepPlugins();
      this.model = commandsToEditData(this.modelValue);
      eventBus.on("workflow-editor-workflowsteps-request", this.notify);
      this.notify();
    } finally {
      this.loadingWorflowSteps = false;
    }
  },
  methods: {
    notify() {
      eventBus.emit("workflow-editor-workflowsteps-updated", this.model);
    },
    chooseProviderAdd({
      service,
      provider,
    }: {
      service: string;
      provider: string;
    }) {
      this.addStepModal = false;
      this.editExtra = {};
      if (!this.isErrorHandler) {
        this.editIndex = -1;
      }
      this.editService = service;

      if (this.isErrorHandler) {
        this.editExtra = {
          errorhandler: {
            config: {},
            keepgoingOnSuccess: false,
          },
        };
      }

      if (provider === "job.reference") {
        this.editModel = {
          type: provider,
          description: "",
          jobref: {
            nodeStep: service === ServiceType.WorkflowNodeStep,
          },
        };
        this.editJobRefModal = true;
      } else {
        this.editModel = {
          type: provider,
          config: {},
          nodeStep: service === ServiceType.WorkflowNodeStep,
        };

        this.editStepModal = true;
      }
    },
    cancelProviderAdd() {
      this.addStepModal = false;
      this.isErrorHandler = false;
    },
    cancelEditStep() {
      this.editStepModal = false;
      this.editJobRefModal = false;
      this.editModel = {};
      this.editExtra = {};
      this.editModelValidation = null;
      this.editIndex = -1;
      this.isErrorHandler = false;
    },
    removeStep(index: number, removeErrorHandler: boolean = false) {
      const originalData = cloneDeep(this.model.commands[index]);
      const commandToRemove = cloneDeep(this.model.commands[index]);

      let dataForUpdatingHistory = {
        operation: Operation.Remove,
        undo: Operation.Insert,
        orig: undefined,
      };

      if (removeErrorHandler) {
        delete commandToRemove.errorhandler;
        this.$refs.historyControls.operationModify(index, commandToRemove);
        dataForUpdatingHistory.operation = Operation.Modify;
        dataForUpdatingHistory.undo = Operation.Modify;
        dataForUpdatingHistory.orig = originalData;
      } else {
        this.$refs.historyControls.operationRemove(index);
      }

      this.$refs.historyControls.changeEvent({
        index,
        dest: -1,
        value: commandToRemove,
        ...dataForUpdatingHistory,
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
    editStepByIndex(index: number, isErrorHandler: boolean = false) {
      this.editIndex = index;

      const command = this.model.commands[index];
      this.editExtra = cloneDeep(command);
      let nodeStep = command.nodeStep;
      this.isErrorHandler = isErrorHandler;
      if(!isErrorHandler) {
        this.editModel = cloneDeep(command);
      } else {
        this.editModel = cloneDeep(command.errorhandler);
        nodeStep = command.errorhandler.nodeStep;
        if(command.errorhandler.jobref) {
          nodeStep = command.errorhandler.jobref.nodeStep;
        }
      }

      this.editService = nodeStep
          ? ServiceType.WorkflowNodeStep
          : ServiceType.WorkflowStep;

      if ((!isErrorHandler && command.jobref) || (isErrorHandler && command.errorhandler.jobref)) {
        this.editJobRefModal = true;
      } else {
        this.editStepModal = true;
      }
    },
    async saveEditStep() {
      try {
        const saveData = cloneDeep(this.editExtra);
        if (this.isErrorHandler) {
          saveData.errorhandler = {
            ...saveData.errorhandler,
            config: this.editModel.config,
            jobref: this.editModel.jobref,
            type: this.editModel.type,
            nodeStep: this.editService === ServiceType.WorkflowNodeStep,
            id: mkid()
          };
        } else {
          saveData.type = this.editModel.type;
          saveData.config = this.editModel.config;
          saveData.id = mkid();
          saveData.nodeStep = this.editService === ServiceType.WorkflowNodeStep;
          saveData.jobref = this.editModel.jobref;
        }

        if (!saveData.jobref && !this.editModel.jobref) {
          saveData.filters = [];

          const response = await validatePluginConfig(
            this.editService,
            saveData.type || saveData.errorhandler.type,
            saveData.config || saveData.errorhandler.config,
          );

          if (
            response.valid &&
            Object.keys(response.errors || {}).length === 0
          ) {
            this.handleSuccessOnValidation(saveData);
          } else {
            this.editModelValidation = response;
          }
        } else {
          saveData.description = this.editModel.description;
          this.handleSuccessOnValidation(saveData);
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
    toggleModalActive(val: boolean) {
      if (this.modalComponent === "edit-plugin-modal") {
        this.editStepModal = val;
      } else if (this.modalComponent === "job-ref-form") {
        this.editJobRefModal = val;
      }
    },
    toggleAddStepModal() {
      this.addStepModal = !this.addStepModal;
    },
    toggleAddErrorHandlerModal(index: number) {
      this.isErrorHandler = true;
      this.editIndex = index;
      this.addStepModal = true;
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
    handleSuccessOnValidation(saveData: any) {
      let dataForUpdatingHistory = {
        index: this.model.commands.length,
        operation: Operation.Insert,
        undo: Operation.Remove,
        orig: undefined,
      };
      let newData = cloneDeep(saveData);

      if (this.editIndex >= 0) {
        const originalData = this.model.commands[this.editIndex];
        if (this.isErrorHandler) {
          newData = { ...originalData, ...saveData };
        }
        this.$refs.historyControls.operationModify(this.editIndex, newData);
        dataForUpdatingHistory.index = this.editIndex;
        dataForUpdatingHistory.operation = Operation.Modify;
        dataForUpdatingHistory.undo = Operation.Modify;
        dataForUpdatingHistory.orig = originalData;
      } else {
        this.$refs.historyControls.operationInsert(
          this.model.commands.length,
          newData,
        );
      }

      this.$refs.historyControls.changeEvent({
        dest: -1,
        value: newData,
        ...dataForUpdatingHistory,
      });

      this.editStepModal = false;
      this.isErrorHandler = false;
      this.editJobRefModal = false;
      this.isErrorHandler = false;
      this.editModel = {};
      this.editExtra = {};
      this.editModelValidation = null;
      this.editIndex = -1;
    },
  },
});
</script>
<style lang="scss">
@media (min-width: 1280px) {
  .modal-dialog {
    min-width: 1024px;
  }
}

@media (min-width: 1440px) {
  .modal-dialog {
    width: 1280px;
  }
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
  //overflow: hidden;

  .step-item-display {
    width: 100%;

    .node-icon {
      margin-left: 5px;
    }

    .step-item-config {
      border-width: 1px;
      border-style: dotted;
      border-color: transparent;
      padding: 5px;
      flex-grow: 1;
      overflow: hidden;

      &:hover {
        cursor: pointer;
        background-color: var(--light-gray);
        border-color: #68b3c8;
      }

      .configpair {
        max-width: 700px;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;

        > span {
          display: flex;
          gap: 5px;
        }
      }

      .argString {
        max-width: 700px;
        overflow: hidden;
        display: flex;
        align-items: center;

        .optvalue {
          margin-top: 0;
        }
      }

      .optvalue,
      .text-success {
        max-width: calc(90% - 100px);
        text-overflow: ellipsis;
        white-space: nowrap;
        overflow: hidden;
      }

      @media (max-width: 700px) {
        .optvalue,
        .text-success {
          max-width: 80%;
          white-space: wrap;
        }
      }
    }

    :deep(.node-icon) {
      display: inline-block;
      margin: 0 5px;
    }
  }

  .step-item-row {
    display: flex;
    justify-content: stretch;
    gap: 10px;

    .step-item-controls {
      margin-top: 5px;
      flex-shrink: 0;

      .btn-group {
        display: inline-flex;
      }
    }
  }

  .step-item-logfilters {
    margin: 10px 0 10px 5px;
  }

  .error-handler-section {
    margin-top: 10px;
  }
}
</style>
