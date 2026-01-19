<template>
  <h2 v-if="conditionalEnabled" class="text-heading--lg smaller-margin">
    {{ $t("Workflow.jobSteps.label") }}
  </h2>

  <common-undo-redo-draggable-list
    ref="historyControls"
    v-model="model.commands"
    revert-all-enabled
    draggable-tag="ol"
    item-key="id"
    :button-label="$t('Workflow.addStep')"
    :loading="loadingWorflowSteps"
    :conditional-enabled="conditionalEnabled"
    @add-button-click="toggleAddStepModal"
  >
    <template #item="{ item: { element, index } }">
      <li>
        <i v-if="conditionalEnabled" class="pi pi-bars dragHandle"></i>
        <div class="step-list-item" :class="{'ea': conditionalEnabled}">
          <div class="step-item-display">
            <StepCard
              v-if="conditionalEnabled && !element.jobref && element.type !== 'conditional.logic' && editingStepId !== element.id"
              :plugin-details="getPluginDetails(element)"
              :config="element"
              :service-name="element.nodeStep ? ServiceType.WorkflowNodeStep : ServiceType.WorkflowStep"
              :log-filters="element.filters || []"
              :error-handler="element.errorhandler ? [element.errorhandler] : []"
              @add-log-filter="addLogFilterForIndex(element.id)"
              @add-error-handler="toggleAddErrorHandlerModal(index)"
              @update:log-filters="updateHistoryWithLogFiltersData(index, $event)"
              @edit-log-filter="handleEditLogFilterFromChip(element.id, $event)"
              @edit-error-handler="editStepByIndex(index, true)"
              @remove-error-handler="removeStep(index, true)"
              @delete="removeStep(index)"
              @duplicate="duplicateStep(index)"
              @edit="editStepByIndex(index)"
            />
            <!-- LogFilters component for EA mode - provides the modal for adding/editing filters -->
            <log-filters
              v-if="conditionalEnabled && !element.jobref && element.type !== 'conditional.logic' && editingStepId !== element.id"
              :model-value="element.filters"
              :title="$t('Workflow.logFilters')"
              :subtitle="stepTitle(element, index)"
              :add-event="'step-action:add-logfilter:' + element.id"
              :edit-event="'step-action:edit-logfilter:' + element.id"
              :conditional-enabled="true"
              @update:model-value="updateHistoryWithLogFiltersData(index, $event)"
            />
            <EditStepCard
              v-else-if="conditionalEnabled && !element.jobref && element.type !== 'conditional.logic' && editingStepId === element.id"
              v-model="editModel"
              :service-name="element.nodeStep ? ServiceType.WorkflowNodeStep : ServiceType.WorkflowStep"
              :validation="editModelValidation"
              :extra-autocomplete-vars="extraAutocompleteVars"
              @save="handleSaveStep(index)"
              @cancel="handleCancelEdit"
            />
            <EditConditionalStepCard
              v-else-if="conditionalEnabled && element.type === 'conditional.logic'"
              :model-value="element"
              :service-name="element.nodeStep ? ServiceType.WorkflowNodeStep : ServiceType.WorkflowStep"
              :validation="editModelValidation"
              :extra-autocomplete-vars="extraAutocompleteVars"
              @save="handleSaveConditionalStep(index)"
              @cancel="handleCancelEdit"
              @switch-step-type="handleSwitchStepType(index)"
            />
            <template v-else>
              <div class="step-item-row">
                <div
                    :id="`wfitem_${index}`"
                    class="step-item-config"
                    data-test="edit-step-item"
                    :title="$t('Workflow.clickToEdit')"
                    @click="editStepByIndex(index)"
                >
                  <plugin-config
                      v-if="!element.jobref && element.type !== 'conditional.logic'"
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
                  <job-ref-step v-else-if="element.jobref" :step="element"></job-ref-step>

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
                      <btn size="xs" data-role="trigger" :disabled="!!(element.errorhandler && element.jobref)">
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
                  v-if="!element.jobref"
                  :class="{'step-item-logfilters': element.filters?.length > 0 }"
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
                  data-test="error-handler-step"
              />
            </template>

          </div>
        </div>
      </li>
    </template>
    <template #empty>
      <p data-testid="no-steps">{{ $t("Workflow.noSteps") }}</p>
    </template>
    <template #extra>
      <component
        :is="chooseModalComponent"
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
            <p class="list-group-heading text-info text-strong" data-testid="error-handler-title">
              {{ $t(`framework.service.${service}.description`) }}
            </p>
          </div>
        </template>
        <span class="text-info" data-testid="selection-description">
          {{
            isErrorHandler
              ? $t("Workflow.errorHandlerDescription")
              : $t("Workflow.clickOnStepType")
          }}
        </span>
      </component>
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
                    data-testid="step-description"
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
import ChoosePluginsEAModal from "@/library/components/plugins/ChoosePluginsEAModal.vue";
import EditPluginModal from "@/library/components/plugins/EditPluginModal.vue";
import pluginConfig from "@/library/components/plugins/pluginConfig.vue";
import { createOptionVariables } from "@/library/stores/contextVariables";
import { useJobStore } from "@/library/stores/JobsStore";
import { ServiceType } from "@/library/stores/Plugins";
import JobRefStep from "@/app/components/job/workflow/JobRefStep.vue";
import { cloneDeep } from "lodash";
import { mapState } from "pinia";
import { defineComponent } from "vue";
import LogFilters from "./LogFilters.vue";
import CommonUndoRedoDraggableList from "@/app/components/common/CommonUndoRedoDraggableList.vue";
import { Operation } from "@/app/components/job/options/model/ChangeEvents";
import { validatePluginConfig } from "@/library/modules/pluginService";
import JobRefForm from "@/app/components/job/workflow/JobRefForm.vue";
import ErrorHandlerStep from "@/app/components/job/workflow/ErrorHandlerStep.vue";
import ConditionalCard from "@/library/components/primeVue/StepCards/ConditionalCard.vue";
import StepCard from "@/library/components/primeVue/StepCards/StepCard.vue";
import EditStepCard from "./EditStepCard.vue";
import EditConditionalStepCard from "./EditConditionalStepCard.vue";

const context = getRundeckContext();
const eventBus = context.eventBus;
export default defineComponent({
  name: "WorkflowSteps",
  components: {
    ConditionalCard,
    ErrorHandlerStep,
    JobRefForm,
    CommonUndoRedoDraggableList,
    EditPluginModal,
    pluginConfig,
    ChoosePluginModal,
    ChoosePluginsEAModal,
    JobRefStep,
    LogFilters,
    StepCard,
    EditStepCard,
    EditConditionalStepCard,
  },
  props: {
    modelValue: {
      type: Object,
      required: true,
      default: () => ({}) as StepsData,
    },
    conditionalEnabled: {
      type: Boolean,
      default: false,
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
      editingStepId: null as string | null,
    };
  },
  computed: {
    chooseModalComponent() {
      return this.conditionalEnabled ? "ChoosePluginsEAModal" : "ChoosePluginModal";
    },
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
          extraAutocompleteVars: createOptionVariables(
            this.jobDefinition?.options || [],
          ),
        };
      } else if (this.editJobRefModal) {
        return {
          "data-test-id": "jobref-modal",
          modalActive: this.editJobRefModal,
          "onUpdate:modalActive": this.toggleModalActive,
          extraAutocompleteVars: createOptionVariables(
              this.jobDefinition?.options || [],
          ),
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
    ...mapState(useJobStore, ["jobDefinition"]),
    extraAutocompleteVars() {
      return createOptionVariables(this.jobDefinition?.options || []);
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
    getPluginDetails(element) {
      const plugins = element.nodeStep ? this.workflowNodeStepPlugins : this.workflowStepPlugins;
      const plugin = plugins.find(p => p.name === element.type);

      if (plugin) {
        return {
          title: plugin.title || element.description || element.type,
          description: plugin.description || '',
          iconUrl: plugin.iconUrl || '',
          tooltip: plugin.description || '',
        };
      }

      return {
        title: element.description || element.type,
        description: '',
        iconUrl: '',
        tooltip: '',
      };
    },
    async chooseProviderAdd({
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
      } else if (provider === "conditional.logic") {
        const newStep = {
          type: provider,
          config: {},
          nodeStep: service === ServiceType.WorkflowNodeStep,
          description: "",
          id: mkid(),
        };
        this.editModel = newStep;
        this.editIndex = this.model.commands.length;
        this.editingStepId = newStep.id;
        // Add the step to the list immediately so EditConditionalStepCard shows
        this.model.commands.push(newStep);
      } else {
        // Check if this is an error handler - use modal flow
        if (this.isErrorHandler) {
          this.editModel = {
            type: provider,
            config: {},
            nodeStep: service === ServiceType.WorkflowNodeStep,
          };
          this.editStepModal = true;
          return;
        }

        // Regular new step - use inline EditStepCard flow
        const newStep = {
          type: provider,
          config: {},
          nodeStep: service === ServiceType.WorkflowNodeStep,
          id: mkid(),
        };
        this.editModel = newStep;
        this.editIndex = this.model.commands.length;
        this.editingStepId = newStep.id;
        // Add the step to the list immediately so EditStepCard shows
        this.model.commands.push(newStep);
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
      this.editingStepId = null;
    },
    handleCancelEdit() {
      // If we're canceling a new step that was just added, remove it
      if (this.editIndex >= 0 && this.editIndex >= this.model.commands.length - 1) {
        // Check if this is a newly added step (at the end of the list)
        const lastStep = this.model.commands[this.editIndex];
        if (lastStep && lastStep.id === this.editingStepId && (!lastStep.config || Object.keys(lastStep.config || {}).length === 0)) {
          // Remove the newly added step
          this.model.commands.splice(this.editIndex, 1);
        }
      }
      this.cancelEditStep();
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
    handleEditLogFilterFromChip(stepId: string, data: { filter: any; index: number }) {
      // Emit event on global eventBus to trigger LogFilters editFilterByIndex
      // The event data should include the filter index
      eventBus.emit("step-action:edit-logfilter:" + stepId, data.index);
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
      if (isErrorHandler) {
        this.editIndex = index;
        const command = this.model.commands[index];
        // Guard: check if error handler exists
        if (!command.errorhandler) {
          console.warn("Cannot edit error handler: error handler does not exist");
          return;
        }
        this.editExtra = cloneDeep(command);
        this.isErrorHandler = true;
        this.editModel = cloneDeep(command.errorhandler);
        const nodeStep = command.errorhandler.nodeStep || (command.errorhandler.jobref?.nodeStep);
        this.editService = nodeStep
          ? ServiceType.WorkflowNodeStep
          : ServiceType.WorkflowStep;
        if (command.errorhandler.jobref) {
          this.editJobRefModal = true;
        } else {
          this.editStepModal = true;
        }
        return;
      }

      const command = this.model.commands[index];
      this.editIndex = index;
      this.editingStepId = command.id;
      this.editExtra = cloneDeep(command);
      this.editModel = cloneDeep(command);
      this.editService = command.nodeStep
        ? ServiceType.WorkflowNodeStep
        : ServiceType.WorkflowStep;
      // Clear any previous validation errors when starting to edit
      this.editModelValidation = { errors: [], valid: true };

      // In EA mode, show EditStepCard for regular steps (not jobref, not conditional.logic)
      // Error handlers and log filters always use modals (handled separately)
      if (this.conditionalEnabled && !command.jobref && command.type !== 'conditional.logic') {
        // Ensure modals are closed when showing EditStepCard
        this.editStepModal = false;
        this.editJobRefModal = false;
        // Don't open modal - EditStepCard will render based on editingStepId
        return;
      }

      // For jobref steps or non-EA mode, use modal
      // Ensure editingStepId is cleared when using modal
      this.editingStepId = null;
      if (command.jobref) {
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
          if (!this.isErrorHandler) {
            saveData.filters = [];
          }

          // For error handlers, use editModel.type/config (what user edited in modal); for regular steps, use step.type/config
          // The original pattern uses || but that would use step.type for error handlers, which is incorrect
          const typeToValidate = this.isErrorHandler 
            ? this.editModel.type  // Use editModel directly since that's what was edited in the modal
            : saveData.type;
          const configToValidate = this.isErrorHandler 
            ? this.editModel.config  // Use editModel directly since that's what was edited in the modal
            : saveData.config;

          const response = await validatePluginConfig(
            this.editService,
            typeToValidate,
            configToValidate || {},
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
      this.editModel = {};
      this.editExtra = {};
      this.editModelValidation = null;
      this.editIndex = -1;
      this.editingStepId = null;
    },
    async handleSaveStep(index: number) {
      try {
        const saveData = cloneDeep(this.editModel);
        if (!saveData.id) {
          saveData.id = this.model.commands[index]?.id || mkid();
        }
        if (saveData.nodeStep === undefined) {
          saveData.nodeStep = this.editService === ServiceType.WorkflowNodeStep;
        }

        if (!saveData.jobref && saveData.type !== "conditional.logic") {
          if (!saveData.filters) {
            saveData.filters = [];
          }

          const response = await validatePluginConfig(
            this.editService!,
            saveData.type,
            saveData.config || {},
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
          this.handleSuccessOnValidation(saveData);
        }
      } catch (e) {
        console.log(e);
      }
    },
    async handleSaveConditionalStep(index: number) {
      try {
        const saveData = cloneDeep(this.editModel);
        if (!saveData.id) {
          saveData.id = this.model.commands[index]?.id || mkid();
        }
        if (saveData.nodeStep === undefined) {
          saveData.nodeStep = this.editService === ServiceType.WorkflowNodeStep;
        }

        const response = await validatePluginConfig(
          this.editService!,
          saveData.type,
          saveData.config || {},
        );

        if (
          response.valid &&
          Object.keys(response.errors || {}).length === 0
        ) {
          this.handleSuccessOnValidation(saveData);
        } else {
          this.editModelValidation = response;
        }
      } catch (e) {
        console.log(e);
      }
    },
    handleSwitchStepType(index: number) {
      const command = this.model.commands[index];
      if (command && command.type === "conditional.logic") {
        // Toggle nodeStep property
        const updatedCommand = {
          ...command,
          nodeStep: !command.nodeStep,
        };
        
        // Update the command in the model
        this.$refs.historyControls.operationModify(index, updatedCommand);
        
        // Update editModel and editService to reflect the change
        this.editModel = cloneDeep(updatedCommand);
        this.editService = updatedCommand.nodeStep
          ? ServiceType.WorkflowNodeStep
          : ServiceType.WorkflowStep;
        
        // Emit change event for history tracking
        this.$refs.historyControls.changeEvent({
          index,
          dest: -1,
          orig: command,
          value: updatedCommand,
          operation: Operation.Modify,
          undo: Operation.Modify,
        });
      }
    },
  },
});
</script>
<style lang="scss">
h2.text-heading--lg {
  margin: 0 0 24px 0;

  &.smaller-margin {
    margin: 0 0 12px 0;
  }
}

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

  &.ea {
    padding: 0 !important;
    border: none;
  }

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

.ea-mode {

  ol[data-testid='draggable-container'] {
    display: flex;
    flex-direction: column;
    gap: 20px;
    list-style: none;
    position: relative;
    counter-reset: list;

    li {
      position: relative;

      &:before {
        counter-increment: list;
        content: counter(list)'.';
        display: block;
        font-size: 16px;
        font-family: Inter, var(--fonts-body) !important;
        left: -18px;
        position: absolute;
        top: 1rem;
      }

      i.pi-bars {
        position: absolute;
        cursor: grab;
        left: -40px;
        top: 17px !important;
      }
    }
  }

  .dragging * {
    cursor: grabbing !important;
  }
}
</style>
