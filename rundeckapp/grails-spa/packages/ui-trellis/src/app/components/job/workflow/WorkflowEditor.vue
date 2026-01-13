<template>
  <section id="optionsContent" class="section-space-lg">
    <div class="form-group">
      <div class="col-sm-2 control-label text-form-label">
        <span id="optsload"></span>{{ $t("options.label") }}
      </div>
      <div class="col-sm-10" style="margin-top: 0.9em">
        <options-editor-section />
      </div>
    </div>
  </section>
  <section id="workflowContent" class="section-separator section-space-lg">
    <div class="form-group">
      <div v-if="!conditionalEnabled" class="col-sm-2 control-label text-form-label">
        {{ $t("Workflow.label") }}
      </div>
      <div v-if="loaded" :class="[conditionalEnabled? 'col-sm-12': 'col-sm-10']" style="padding-top: 1em">
        <workflow-basic v-model="basicData" :conditional-enabled="conditionalEnabled"  />
        <workflow-strategy v-model="strategyData" :conditional-enabled="conditionalEnabled" />
        <hr />
        <workflow-global-log-filters v-model="logFiltersData" />
        <hr />
        <workflow-steps v-model="stepsData" :conditional-enabled="conditionalEnabled" />
      </div>
    </div>
  </section>
</template>
<script lang="ts">
import {
  BasicData,
  createBasicData,
  createLogFiltersData,
  createStepsData,
  createStrategyData,
  exportPluginData,
  GlobalLogFiltersData,
  StepsData,
  WorkflowData,
} from "@/app/components/job/workflow/types/workflowTypes";
import WorkflowBasic from "@/app/components/job/workflow/WorkflowBasic.vue";
import WorkflowGlobalLogFilters from "@/app/components/job/workflow/WorkflowGlobalLogFilters.vue";
import WorkflowSteps from "@/app/components/job/workflow/WorkflowSteps.vue";
import WorkflowStrategy from "@/app/components/job/workflow/WorkflowStrategy.vue";
import { PluginConfig } from "@/library/interfaces/PluginConfig";
import { defineComponent } from "vue";
import OptionsEditorSection from "@/app/pages/job/editor/OptionsEditorSection.vue";
import { getFeatureEnabled } from "@/library/services/feature";

export default defineComponent({
  name: "WorkflowEditor",
  components: {
    OptionsEditorSection,
    WorkflowBasic,
    WorkflowSteps,
    WorkflowGlobalLogFilters,
    WorkflowStrategy,
  },
  props: {
    modelValue: {
      type: Object,
      required: true,
      default: () => ({}) as WorkflowData,
    },
  },
  emits: ["update:modelValue"],
  data() {
    return {
      basicData: {} as BasicData,
      strategyData: {} as PluginConfig,
      logFiltersData: {} as GlobalLogFiltersData,
      stepsData: {} as StepsData,
      loaded: false,
      conditionalEnabled: false,
    };
  },
  watch: {
    basicData: {
      handler() {
        this.modified();
      },
      deep: true,
    },
    strategyData: {
      handler() {
        this.modified();
      },
      deep: true,
    },
    logFiltersData: {
      handler() {
        this.modified();
      },
      deep: true,
    },
    stepsData: {
      handler() {
        this.modified();
      },
      deep: true,
    },
  },
  async mounted() {
    this.conditionalEnabled = await getFeatureEnabled("earlyAccessJobConditional");
    this.basicData = createBasicData(this.modelValue);
    this.strategyData = createStrategyData(this.modelValue);
    this.logFiltersData = createLogFiltersData(this.modelValue);
    this.stepsData = createStepsData(this.modelValue);
    this.loaded = true;
  },
  methods: {
    modified() {
      this.$emit("update:modelValue", {
        ...this.modelValue,
        ...this.basicData,
        ...exportPluginData(this.strategyData, this.logFiltersData),
        ...this.stepsData,
      });
    },
  },
});
</script>
