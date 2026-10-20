<template>
  <options-editor
    v-if="optionsData"
    :options-data="optionsData"
    :features="features"
    @changed="changed"
  />
  <json-embed
    v-if="updatedData"
    :output-data="updatedData.options"
    field-name="jobOptionsJson"
  />
</template>
<script lang="ts">
import { useJobStore } from "../../../../library/stores/JobsStore";
import { cloneDeep } from "lodash";
import * as _ from "lodash";
import { mapActions, mapState } from "pinia";
import OptionsEditor from "../../../components/job/options/OptionsEditor.vue";
import JsonEmbed from "./JsonEmbed.vue";
import { defineComponent } from "vue";
import type {
  JobOptionEdit,
  JobOptionsData,
} from "../../../../library/types/jobs/JobEdit";
import type { JobDefinition } from "../../../../library/types/jobs/JobDefinition";

import { getRundeckContext } from "../../../../library";

import { EventBus } from "../../../../library/utilities/vueEventBus";

const eventBus = EventBus;

export default defineComponent({
  name: "OptionsEditorSection",
  components: { OptionsEditor, JsonEmbed },
  data() {
    return {
      features: {},
      optionsData: null as JobOptionsData | null,
      updatedData: null as JobOptionsData | null,
      subs: {} as Record<string, ReturnType<typeof eventBus.on>>,
      uuid: "",
    };
  },
  async mounted() {
    const data = getRundeckContext().data;
    if (data) {
      this.uuid = data.otherData?.uuid || "!new";

      this.features = data.features ?? {};
      if (data.optionsData) {
        this.optionsData = data.optionsData;
        this.updatedData = this.optionsData;
      }
      this.subs["job-edit-schedules-changed"] = eventBus.on(
        "job-edit-schedules-changed",
        (data) => {
          if (this.optionsData)
            this.optionsData.jobWasScheduled = data.scheduled;
        },
      );
      if (!this.hasJob(this.uuid)) {
        this.initializeJobPlaceholder();
      }

      await this.updateStore();
    }
  },
  async beforeUnmount() {
    eventBus.off(
      "job-edit-schedules-changed",
      this.subs["job-edit-schedules-changed"],
    );
  },
  computed: {
    ...mapState(useJobStore, ["hasJob"]),
  },
  methods: {
    async updateStore() {
      await this.setActiveId(this.uuid);
      await this.updateJobDefinition(
        {
          options: this.updatedData?.options || [],
        } as JobDefinition,
        this.uuid,
      );
    },
    async changed(data: JobOptionEdit[]) {
      if (!_.isEqual(data, this.updatedData?.options)) {
        if (this.updatedData) {
          this.updatedData.options = cloneDeep(data);
        }

        await this.updateStore();
        //nb: hook to indicate job was editted, defined in jobedit.js
        const win = window as Window & { jobWasEdited?: () => void };
        if (win.jobWasEdited && typeof win.jobWasEdited === "function") {
          win.jobWasEdited();
        }
      }
    },
    ...mapActions(useJobStore, [
      "updateJobDefinition",
      "setActiveId",
      "initializeJobPlaceholder",
    ]),
  },
});
</script>

<style scoped lang="scss"></style>
