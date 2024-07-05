<template>
  <execution-editor v-model="executionData" />
  <json-embed field-name="jobExecutionPluginsJSON" />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import ExecutionEditor from "../../../components/job/execution/ExecutionEditor.vue";
import JsonEmbed from "./JsonEmbed.vue";
import { getRundeckContext } from "../../../../library";

export default defineComponent({
  name: "ExecutionEditorSection",
  components: {
    ExecutionEditor,
    JsonEmbed,
  },
  data() {
    return {
      executionData: null,
    };
  },
  async mounted() {
    if (getRundeckContext() && getRundeckContext().data) {
      this.executionData = getRundeckContext().data.executionData;
      // this.updatedData = this.notificationData;
    }
  },
});
</script>
