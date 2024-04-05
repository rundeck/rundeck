<template>
  <div v-if="loaded">
    <workflow-editor v-model="updatedData" />
    <json-embed
      :output-data="updatedData"
      field-name="jobWorkflowJson"
      v-if="loaded"
    />
    <div>{{ JSON.stringify(updatedData) }}</div>
  </div>
</template>

<script lang="ts">
import WorkflowSteps from "@/app/components/job/workflow/WorkflowSteps.vue";
import { getRundeckContext } from "@/library";
import { cloneDeep } from "lodash";
import { defineComponent } from "vue";
import JsonEmbed from "@/app/pages/job/editor/JsonEmbed.vue";
import WorkflowEditor from "@/app/components/job/workflow/WorkflowEditor.vue";

export default defineComponent({
  name: "WorkflowEditorSection",
  components: {
    WorkflowSteps,
    JsonEmbed,
    WorkflowEditor,
  },
  data() {
    return {
      workflowData: {},
      updatedData: {},
      loaded: false,
    };
  },
  async mounted() {
    const rundeck = getRundeckContext();
    if (rundeck && rundeck.data) {
      //todo: convert to expected format
      let cloned = cloneDeep(rundeck.data.workflowData);
      if (
        cloned.sequence &&
        cloned.sequence.pluginConfig &&
        cloned.sequence.pluginConfig.LogFilters
      ) {
        cloned.logFilters = cloned.sequence.pluginConfig.LogFilters;
      }
      this.workflowData = cloned;
      //todo: convert to expected format
      this.updatedData = this.workflowData;
    }
    this.loaded = true;
  },
});
</script>

<style scoped lang="scss"></style>
