<template>
  <div>
    <options-editor
      :options-data="optionsData"
      v-if="optionsData"
      @changed="changed"
    />
    <json-embed
      :output-data="updatedData.options"
      field-name="jobOptionsJson"
    />
  </div>
</template>
<script lang="ts">
import { cloneDeep } from "lodash";
import * as _ from "lodash";
import OptionsEditor from "../../../components/job/options/OptionsEditor.vue";
import JsonEmbed from "./JsonEmbed.vue";
import { defineComponent } from "vue";

import { getRundeckContext } from "../../../../library";

import { EventBus } from "../../../../library/utilities/vueEventBus";

const eventBus = EventBus;

export default defineComponent({
  name: "OptionsEditorSection",
  components: { OptionsEditor, JsonEmbed },
  data() {
    return {
      optionsData: null,
      updatedData: {
        options: [],
      },
      subs: {},
    };
  },
  methods: {
    changed(data) {
      if (!_.isEqual(data, this.updatedData.options)) {
        this.updatedData.options = cloneDeep(data);
        //nb: hook to indicate job was editted, defined in jobedit.js
        //@ts-ignore
        if (
          window.hasOwnProperty("jobWasEdited") &&
          typeof window.jobWasEdited === "function"
        ) {
          //@ts-ignore
          window.jobWasEdited();
        }
      }
    },
  },
  async mounted() {
    if (getRundeckContext() && getRundeckContext().data) {
      this.optionsData = getRundeckContext().data.optionsData;
      this.updatedData = this.optionsData;
      this.subs["job-edit-schedules-changed"] = eventBus.on(
        "job-edit-schedules-changed",
        (data) => {
          this.optionsData.jobWasScheduled = data.scheduled;
        },
      );
    }
  },
  async beforeUnmount() {
    eventBus.off(
      "job-edit-schedules-changed",
      this.subs["job-edit-schedules-changed"],
    );
  },
});
</script>

<style scoped lang="scss"></style>
