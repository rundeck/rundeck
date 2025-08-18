<template>
  <div>
    <div class="">{{ $t("Workflow.property.keepgoing.prompt") }}</div>
    <div class="radio radio-inline">
      <input
        id="workflowKeepGoingFail"
        v-model="data.keepgoing"
        type="radio"
        name="workflow_keepgoing"
        :value="false"
      />
      <label for="workflowKeepGoingFail">
        {{ $t("Workflow.property.keepgoing.false.description") }}
      </label>
    </div>
    <div class="radio radio-inline">
      <input
        id="workflowKeepGoingRemainingSteps"
        v-model="data.keepgoing"
        type="radio"
        name="workflow_keepgoing"
        :value="true"
      />
      <label for="workflowKeepGoingRemainingSteps">
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
  props: {
    modelValue: {
      type: Object,
      required: true,
      default: () => ({}) as BasicData,
    },
  },
  emits: ["update:modelValue"],
  data(props) {
    return {
      data: {} as BasicData,
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
