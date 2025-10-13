<template>
  <options-editor
    v-if="optionsData"
    :options-data="optionsData"
    @changed="changed"
  />
  <json-embed :output-data="updatedData.options" field-name="jobOptionsJson" />
</template>
<script lang="ts">
import { useJobStore } from "@/library/stores/JobsStore";
import { cloneDeep } from "lodash";
import * as _ from "lodash";
import { mapActions } from "pinia";
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
      uuid: "",
    };
  },
  async mounted() {
    if (getRundeckContext() && getRundeckContext().data) {
      this.uuid = getRundeckContext().data.otherData?.uuid;

      this.optionsData = getRundeckContext().data.optionsData;
      this.updatedData = this.optionsData;
      this.subs["job-edit-schedules-changed"] = eventBus.on(
        "job-edit-schedules-changed",
        (data) => {
          this.optionsData.jobWasScheduled = data.scheduled;
        },
      );
      await this.updateStore();
    }
  },
  async beforeUnmount() {
    eventBus.off(
      "job-edit-schedules-changed",
      this.subs["job-edit-schedules-changed"],
    );
  },
  methods: {
    async updateStore() {
      await this.setActiveId(this.uuid);
      await this.updateJobDefinition(
        {
          options: this.updatedData.options,
        },
        this.uuid,
      );
    },
    async changed(data) {
      if (!_.isEqual(data, this.updatedData.options)) {
        this.updatedData.options = cloneDeep(data);

        await this.updateStore();
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
    ...mapActions(useJobStore, ["updateJobDefinition", "setActiveId"]),
  },
});
</script>

<style scoped lang="scss"></style>
