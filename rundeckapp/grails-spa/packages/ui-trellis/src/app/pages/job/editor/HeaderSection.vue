<script lang="ts">
import { getRundeckContext } from "@/library";
import { getProjectMeta } from "@/library/services/jobBrowse";
import { defineComponent } from "vue";

import { EventBus } from "../../../../library/utilities/vueEventBus";

const rundeckContext = getRundeckContext();
const eventBus = EventBus;

export default defineComponent({
  name: "HeaderSection",
  data() {
    return {
      detailsData: {},
      otherData: {},
      subs: {},
      projAuthz: null as any,
    };
  },
  computed: {
    createMode(): boolean {
      return !this.uuid;
    },
    editMode(): boolean {
      return !this.createMode;
    },
    jobName() {
      return this.detailsData.jobName || "";
    },
    groupPath() {
      return this.detailsData.groupPath || "";
    },
    uuid() {
      return this.otherData.uuid || "";
    },
    href() {
      return this.detailsData.href;
    },
    jobCreateAuthz() {
      return this.projAuthz?.types?.job?.create || false;
    },
    uploadHref() {
      return `${rundeckContext.rdBase}project/${rundeckContext.projectName}/job/upload`;
    },
  },
  async mounted() {
    if (rundeckContext.data) {
      this.detailsData = rundeckContext.data.detailsData;
      this.otherData = rundeckContext.data.otherData;
    }

    const result = await getProjectMeta(rundeckContext.projectName, "authz");
    this.projAuthz = result.find((meta) => meta.name === "authz")?.data || {};

    this.subs["job-edit-details-changed"] = eventBus.on(
      "job-edit-details-changed",
      (data) => {
        this.detailsData.jobName = data.jobName;
        this.detailsData.groupPath = data.groupPath;
      },
    );
  },
  async beforeUnmount() {
    eventBus.off(
      "job-edit-details-changed",
      this.subs["job-edit-details-changed"],
    );
  },
});
</script>

<template>
  <div class="row">
    <h4 class="col-sm-10 card-title flow-h">
      <span :class="{ 'text-secondary colon-after': jobName }">{{
        $t(
          editMode
            ? "ScheduledExecution.page.edit.title"
            : "ScheduledExecution.page.create.title",
        )
      }}</span>
      <a
        v-if="editMode"
        class="link-quiet text-strong"
        :title="groupPath"
        :href="href"
      >
        {{ jobName }}
      </a>
      <span v-else :title="groupPath">{{ jobName }}</span>
      <span v-if="uuid" class="text-muted">{{ uuid }}</span>
    </h4>

    <div v-if="jobCreateAuthz" class="col-sm-2">
      <a :href="uploadHref" class="btn btn-secondary btn-sm float-right">
        <i class="glyphicon glyphicon-upload"></i>
        {{ $t("upload.definition.button.label") }}
      </a>
    </div>
  </div>
</template>

<style scoped lang="scss">
.flow-h > * + * {
  margin-left: var(--spacing-2);
}
</style>
