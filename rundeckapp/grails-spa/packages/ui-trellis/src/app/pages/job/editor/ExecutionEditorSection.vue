<template>
  <execution-editor v-model="updatedData" :initial-data="retrievedData" />
  <json-embed
    v-if="updatedData"
    :output-data="updatedData"
    field-name="jobExecutionPluginsJSON"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import ExecutionEditor from "../../../components/job/execution/ExecutionEditor.vue";
import JsonEmbed from "./JsonEmbed.vue";
import { getRundeckContext } from "../../../../library";
import { isEqual } from "lodash";

export default defineComponent({
  name: "ExecutionEditorSection",
  components: {
    ExecutionEditor,
    JsonEmbed,
  },
  data() {
    return {
      executionData: null,
      updatedData: null,
      retrievedData: null,
    };
  },
  watch: {
    updatedData: {
      deep: true,
      handler() {
        if (
          !isEqual(
            this.updatedData.ExecutionLifecycle,
            this.retrievedData.ExecutionLifecycle,
          )
        ) {
          // @ts-ignore
          window.jobWasEdited();
        }
      },
    },
  },
  async mounted() {
    if (getRundeckContext() && getRundeckContext().data) {
      this.retrievedData = getRundeckContext().data.executionData;
    }
  },
});
</script>
