<template>
  <div>
    <div class="">{{ $t("Workflow.property.keepgoing.prompt") }}</div>
    <div class="radio radio-inline">
      <input
        type="radio"
        id="workflowKeepGoingFail_"
        name="workflow_keepgoing"
        :value="false"
        v-model="data.keepgoing"
      />
      <label for="workflowKeepGoingFail_">
        {{ $t("Workflow.property.keepgoing.false.description") }}
      </label>
    </div>
    <div class="radio radio-inline">
      <input
        type="radio"
        id="workflowKeepGoingRemainingSteps_"
        name="workflow_keepgoing"
        :value="true"
        v-model="data.keepgoing"
      />
      <label for="workflowKeepGoingRemainingSteps_">
        {{ $t("Workflow.property.keepgoing.true.description") }}
      </label>
    </div>
  </div>
</template>

<script lang="ts">
import {
  BasicData,
  createBasicData,
} from "@/app/components/job/workflow/types/workflowTypes";
import { defineComponent } from "vue";

export default defineComponent({
  name: "WorkflowBasic",
  emits: ["update:modelValue"],
  props: {
    modelValue: {
      type: Object,
      required: true,
      default: () => ({}) as BasicData,
    },
  },
  data(props) {
    return {
      data: {},
    };
  },
  watch: {
    // modelValue: {
    //   handler() {
    //     this.data = createBasicData(this.modelValue);
    //   },
    //   deep: true,
    // },
    data: {
      handler() {
        this.$emit("update:modelValue", this.data);
      },
      deep: true,
    },
  },
  mounted() {
    this.data = createBasicData(this.modelValue);
  },
});
</script>

<style scoped lang="scss"></style>
