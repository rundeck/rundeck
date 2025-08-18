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
import { EventBus } from "../../../../library/utilities/vueEventBus";

const eventBus = EventBus;
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
      eventBus,
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
          this.eventBus.emit("job-edit-details-changed", this.updatedData);
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
