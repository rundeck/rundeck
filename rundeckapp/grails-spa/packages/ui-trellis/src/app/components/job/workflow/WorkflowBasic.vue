<template>
  <div :class="conditionalEnabled ? 'conditional-enabled' : ''">
    <h2 v-if="conditionalEnabled" class="text-heading--lg">
      {{ $t("Workflow.property.executionBehavior.label") }}
    </h2>
    <div v-if="conditionalEnabled" class="workflow-section">
      <h3 class="text-heading--md">
        {{ $t("Workflow.property.stepFailureBehavior.label") }}
      </h3>
      <p class="text-body workflow-prompt">
        {{ $t("Workflow.property.keepgoing.prompt") }}
      </p>
    </div>
    <div v-if="!conditionalEnabled">{{ $t("Workflow.property.keepgoing.prompt") }}</div>
    <div :class="conditionalEnabled ? 'radio workflow-radio' : 'radio radio-inline'">
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
    <div :class="conditionalEnabled ? 'radio' : 'radio radio-inline'">
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
    conditionalEnabled: {
      type: Boolean,
      default: false,
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

<style scoped lang="scss">
.conditional-enabled {
h2 {
  margin: 0;
}

h3 {
  margin: 0 0 16px 0;
}

.workflow-section {
  display: flex;
  flex-direction: column;
  margin-top: 24px;
}

.workflow-prompt {
  margin-bottom: 0;
}

.workflow-radio {
  margin-top: 16px;
}
}
</style>
