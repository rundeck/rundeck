<template>
  <section id="workflowContent2" class="section-separator section-space-lg">
    <div class="form-group">
      <div class="col-sm-2 control-label text-form-label">
        {{ $t("Workflow.label") }}
      </div>
      <div v-if="loaded" class="col-sm-10" style="padding-top: 1em">
        <workflow-basic v-model="basicData" />
        <WorkflowStrategy v-model="strategyData" />
        <hr />
        <WorkflowGlobalLogFilters v-model="logFiltersData" />
        <hr />
        <WorkflowSteps v-model="stepsData" />
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
  GlobalLogFiltersData,
  StepsData,
  StrategyData,
  WorkflowData,
} from "@/app/components/job/workflow/types/workflowTypes";
import WorkflowBasic from "@/app/components/job/workflow/WorkflowBasic.vue";
import WorkflowGlobalLogFilters from "@/app/components/job/workflow/WorkflowGlobalLogFilters.vue";
import WorkflowSteps from "@/app/components/job/workflow/WorkflowSteps.vue";
import WorkflowStrategy from "@/app/components/job/workflow/WorkflowStrategy.vue";
import { defineComponent } from "vue";

export default defineComponent({
  name: "WorkflowEditor",
  components: {
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
      strategyData: {} as StrategyData,
      logFiltersData: {} as GlobalLogFiltersData,
      stepsData: {} as StepsData,
      loaded: false,
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
  mounted() {
    this.basicData = createBasicData(this.modelValue);
    this.strategyData = createStrategyData(this.modelValue);
    this.logFiltersData = createLogFiltersData(this.modelValue);
    this.stepsData = createStepsData(this.modelValue);
    this.loaded = true;
  },
  methods: {
    modified() {
      console.log(
        "🔄 Emitting update:modelValue with stepsData:",
        JSON.stringify(this.stepsData, null, 2),
      );
      this.$emit("update:modelValue", {
        ...this.modelValue,
        ...this.basicData,
        ...this.strategyData,
        ...{ pluginConfig: this.logFiltersData },
        ...this.stepsData,
      });
    },
  },
});
</script>

<style scoped lang="scss"></style>
