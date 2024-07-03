<template>
  <details-editor
    v-if="updatedData"
    v-model="updatedData"
    :allow-html="allowHtml"
  />
  <json-embed :output-data="updatedData" field-name="jobDetailsJson" />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import JsonEmbed from "@/app/pages/job/editor/JsonEmbed.vue";
import DetailsEditor from "@/app/components/job/details/DetailsEditor.vue";
import { cloneDeep, isEqual } from "lodash";
import { getRundeckContext } from "@/library";

export default defineComponent({
  name: "DetailsEditorSection",
  components: { DetailsEditor, JsonEmbed },
  props: {
    allowHtml: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      updatedData: null,
      outputData: null,
    };
  },
  watch: {
    updatedData: {
      deep: true,
      handler() {
        if (!isEqual(this.updatedData, this.outputData)) {
          // @ts-ignore
          window.jobWasEdited();
        }
      },
    },
  },
  mounted() {
    const rundeckContext = getRundeckContext();
    if (rundeckContext.data) {
      this.outputData = rundeckContext.data.detailsData;
      this.updatedData = Object.assign({}, this.outputData);
    }
  },
});
</script>
