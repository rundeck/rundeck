<template>
  <span v-if="!dataReady">
    <i class="fas fa-spinner fa-pulse"></i>
    <span v-if="showText">{{ $t("job.scm.status.loading.message") }}</span>
  </span>
  <template v-if="jobSynchState">
    <span :title="jobText" class="scm_status">
      <span :class="jobClass">
        <i :class="jobIcon" class="glyphicon"></i>
        {{ displayText }}
      </span>
    </span>
  </template>
</template>

<script lang="ts">
import {
  JobPageStore,
  JobPageStoreInjectionKey,
} from "@/library/stores/JobPageStore";
import { JobBrowseItem, JobBrowseMeta } from "@/library/types/jobs/JobBrowse";
import { defineComponent, inject } from "vue";
import { ScmTextUtilities } from "@/library/utilities/scm/scmTextUtilities";

export default defineComponent({
  name: "JobScmStatus",
  props: {
    itemData: {
      type: Object,
      default: () => {},
    },
    showText: {
      type: Boolean,
      default: false,
    },
    showClean: {
      type: Boolean,
      default: false,
    },
    showExport: {
      type: Boolean,
      default: true,
    },
    dataReady: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      scmUtilities: new ScmTextUtilities(this.$t),
    };
  },
  setup() {
    return {
      jobPageStore: inject(JobPageStoreInjectionKey) as JobPageStore,
    };
  },
  computed: {
    jobSynchState(): string | undefined {
      return this.exportSynchState || this.importSynchState;
    },
    exportSynchState(): string | undefined {
      const state = this.scmExport?.jobState?.synchState;
      return state && (this.showClean || state !== "CLEAN") ? state : undefined;
    },
    importSynchState(): string | undefined {
      const state = this.scmImport?.jobState?.synchState;
      return state && (this.showClean || state !== "CLEAN") ? state : undefined;
    },
    scmExport(): JobBrowseMeta | undefined {
      return this.job.meta?.find(
        (meta: JobBrowseMeta) => meta.name === "scmExport",
      )?.data;
    },
    scmImport(): JobBrowseMeta | undefined {
      return this.job.meta?.find(
        (meta: JobBrowseMeta) => meta.name === "scmImport",
      )?.data;
    },
    job(): JobBrowseItem | undefined {
      return this.itemData?.job;
    },
    displayText() {
      if (!this.showText) {
        return "";
      }
      if (this.exportSynchState && (this.showClean || this.exportSynchState))
        return this.scmUtilities.exportDisplayText(this.exportSynchState);

      return this.scmUtilities.importDisplayText(this.importSynchState);
    },
    jobClass() {
      return this.scmUtilities.jobScmStatusIconClass(this.jobSynchState);
    },
    jobIcon() {
      return this.scmUtilities.jobScmStatusIcon(this.jobSynchState);
    },
    jobText() {
      return this.scmUtilities.jobScmDescription(
        this.exportSynchState,
        this.importSynchState,
      );
    },
  },
  methods: {},
});
</script>

<style scoped lang="scss">
.scm_status {
  margin-right: var(--spacing-2);
}
</style>
