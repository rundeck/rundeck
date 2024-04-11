<template>
  <div>TODO: undo-redo</div>
  <div>
    <div
      v-for="(step, index) in model.commands"
      :key="step.id"
      class="step-list-item"
    >
      <div
        class="step-item-display"
        @click="editStepByIndex(index)"
        title="Click to edit"
      >
        <plugin-config
          :service-name="
            step.nodeStep
              ? ServiceType.WorkflowNodeStep
              : ServiceType.WorkflowStep
          "
          :provider="step.type"
          :config="step.config"
          :read-only="true"
          :show-title="true"
          :show-icon="true"
          :show-description="true"
          mode="show"
          v-if="!step.jobref"
        >
          <template #iconSuffix v-if="step.nodeStep">
            <i class="fas fa-hdd"></i>
          </template>
        </plugin-config>
        <job-ref-step :step="step" v-else></job-ref-step>

        <div v-if="step.description" class="wfstep-description">
          {{ step.description }}
        </div>
        <template v-if="!step.jobref">
          <log-filters
            v-model="step.filters"
            title="Log Filters"
            subtitle="This step"
            :add-event="'step-action:add-logfilter:' + index"
          />
        </template>
      </div>
      <div class="step-item-controls">
        <div class="btn-group" role="group" aria-label="item controls">
          <btn @click="editStepByIndex(index)" size="xs">
            <i class="fas fa-edit"></i>
            Edit
          </btn>
          <btn
            size="xs"
            title="Duplicate this step"
            @click="duplicateStep(index)"
          >
            <i class="glyphicon glyphicon-duplicate"></i>
          </btn>
          <dropdown menu-right>
            <btn size="xs" data-role="trigger"
              ><i class="glyphicon glyphicon-cog"></i>
              <span class="caret"></span
            ></btn>
            <template #dropdown>
              <li><a role="button">Add Error Handler</a></li>
              <li v-if="!step.jobref">
                <a role="button" @click="addLogFilterForIndex(index)"
                  >Add Log Filter</a
                >
              </li>
            </template>
          </dropdown>
          <btn
            size="xs"
            type="danger"
            title="Delete this step"
            @click="removeStep(index)"
          >
            <i class="glyphicon glyphicon-remove"></i>
          </btn>
        </div>

        <span class="btn btn-xs dragHandle" title="Drag to reorder"
          ><i class="glyphicon glyphicon-resize-vertical"></i
        ></span>
      </div>
    </div>
    <choose-plugin-modal
      v-model="addStepModal"
      title="Add a Step"
      :services="[ServiceType.WorkflowNodeStep, ServiceType.WorkflowStep]"
      :tab-names="[
        $t('plugin.type.WorkflowNodeStep.title.plural'),
        $t('plugin.type.WorkflowStep.title.plural'),
      ]"
      @cancel="addStepModal = false"
      @selected="chooseProviderAdd"
    >
      <span class="text-info">Click on a Step type to add.</span>
    </choose-plugin-modal>

    <edit-plugin-modal
      v-if="editStepModal"
      v-model:modal-active="editStepModal"
      v-model="editModel"
      :validation="editModelValidation"
      :service-name="editService"
      title="Edit Step"
      @cancel="cancelEditStep"
      @save="saveEditStep"
    ></edit-plugin-modal>

    <btn @click="addStepModal = true" size="sm">
      <i class="fas fa-plus"></i>
      Add a step
    </btn>
  </div>
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
import { PluginConfig } from "@/library/interfaces/PluginConfig";
import { ServiceType } from "@/library/stores/Plugins";
import JobRefStep from "@/app/components/job/workflow/JobRefStep.vue";
import { cloneDeep } from "lodash";
import Vue, { defineComponent, nextTick } from "vue";
import LogFilters from "./LogFilters.vue";

const context = getRundeckContext();
const eventBus = context.eventBus;
export default defineComponent({
  name: "WorkflowSteps",
  emits: ["update:modelValue"],
  components: {
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
  data() {
    return {
      ServiceType,
      eventBus,
      model: {} as StepsEditData,
      workflowStepPlugins: [],
      workflowNodeStepPlugins: [],
      addStepModal: false,
      editStepModal: false,
      editModel: {} as EditStepData,
      editModelValidation: null,
      editService: null,
      editIndex: -1,
    };
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
      this.editIndex = -1;
      this.editService = service;
      this.editStepModal = true;
    },
    cancelEditStep() {
      this.editStepModal = false;
      this.editModel = {};
      this.editModelValidation = null;
      this.editIndex = -1;
    },
    removeStep(index: number) {
      this.model.commands.splice(index, 1);
      this.wasChanged();
    },
    async addLogFilterForIndex(index: number) {
      eventBus.emit("step-action:add-logfilter:" + index);
    },
    duplicateStep(index: number) {
      debugger;
      let command = cloneDeep(this.model.commands[index]);
      command.id = mkid();
      this.model.commands.splice(index + 1, 0, command);
      this.wasChanged();
    },
    editStepByIndex(index: number) {
      this.editIndex = index;
      let command = this.model.commands[index];
      this.editModel = cloneDeep(command);
      //todo: jobref
      this.editService = command.nodeStep
        ? ServiceType.WorkflowNodeStep
        : ServiceType.WorkflowStep;
      this.editStepModal = true;
    },
    saveEditStep() {
      let saveData = cloneDeep(this.editModel);
      saveData.id = mkid();
      saveData.nodeStep = this.editService === ServiceType.WorkflowNodeStep;
      if (this.editIndex >= 0) {
        this.model.commands[this.editIndex] = saveData;
      } else {
        this.model.commands.push(saveData);
      }
      this.editStepModal = false;
      this.editModel = {};
      this.editModelValidation = null;
      this.editIndex = -1;
      this.wasChanged();
    },
    wasChanged() {
      this.$emit("update:modelValue", editCommandsToStepsData(this.model));
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
  },
  async mounted() {
    await this.getStepPlugins();
    this.model = commandsToEditData(this.modelValue);
  },
});
</script>
<style lang="scss">
.step-list-item {
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
    border-width: 1px;
    border-style: dotted;
    border-color: transparent;
    &:hover {
      cursor: pointer;
      background-color: var(--light-gray);
      border-color: #68b3c8;
    }
  }
}
</style>
