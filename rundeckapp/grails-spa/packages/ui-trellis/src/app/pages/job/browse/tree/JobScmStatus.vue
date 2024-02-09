<template>
  <template v-if="jobSynchState">
    <span :title="jobText" class="scm_status">
      <span :class="jobClass">
        <i :class="jobIcon" class="glyphicon"></i>
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

export default defineComponent({
  name: "JobScmStatus",
  props: {
    itemData: {
      type: Object,
      default: () => {},
    },
  },
  setup() {
    return {
      jobPageStore: inject(JobPageStoreInjectionKey) as JobPageStore,
    };
  },
  methods: {},
  computed: {
    jobSynchState(): String | undefined {
      return this.exportSynchState || this.importSynchState;
    },
    exportSynchState(): String | undefined {
      const state = this.scmExport?.jobState?.synchState;
      return state && state !== "CLEAN" ? state : undefined;
    },
    importSynchState(): String | undefined {
      const state = this.scmImport?.jobState?.synchState;

      return state && state !== "CLEAN" ? state : undefined;
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
    jobClass() {
      switch (this.jobSynchState) {
        case "EXPORT_NEEDED":
          return "text-info";
        case "CREATE_NEEDED":
          return "text-success";
        case "UNKNOWN":
          return "text-primary";
        case "IMPORT_NEEDED":
        case "REFRESH_NEEDED":
        case "LOADING":
          return "text-warning";
        case "DELETED":
          return "text-danger";
        case "CLEAN":
          return "text-primary";
      }
      return "text-primary";
    },
    jobIcon() {
      switch (this.jobSynchState) {
        case "EXPORT_NEEDED":
        case "CREATE_NEEDED":
          return "glyphicon-exclamation-sign";
        case "UNKNOWN":
          return "glyphicon-question-sign";
        case "IMPORT_NEEDED":
        case "REFRESH_NEEDED":
          return "glyphicon-exclamation-sign";
        case "DELETED":
          return "glyphicon-minus-sign";
        case "CLEAN":
          return "glyphicon-ok";
        case "LOADING":
          return "glyphicon-refresh";
      }
      return "glyphicon-plus";
    },
    jobText() {
      let exportStatus = null;
      let importStatus = null;
      let text = null;
      if (this.exportSynchState) {
        exportStatus = this.exportSynchState;
        switch (exportStatus) {
          case "EXPORT_NEEDED":
            text = this.$t("scm.export.status.EXPORT_NEEDED.description");
            break;
          case "CREATE_NEEDED":
            text = this.$t("scm.export.status.CREATE_NEEDED.description");
            break;
          case "CLEAN":
            text = this.$t("scm.export.status.CLEAN.description");
            break;
          case "LOADING":
            text = this.$t("scm.export.status.LOADING.description");
            break;
          default:
            text = exportStatus;
        }
      }
      if (this.importSynchState) {
        if (text) {
          text += ", ";
        } else {
          text = "";
        }
        importStatus = this.importSynchState;
        switch (importStatus) {
          case "IMPORT_NEEDED":
            text += this.$t("scm.import.status.IMPORT_NEEDED.description");
            break;
          case "DELETE_NEEDED":
            text += this.$t("scm.import.status.DELETE_NEEDED.description");
            break;
          case "CLEAN":
            text += this.$t("scm.import.status.CLEAN.description");
            break;
          case "REFRESH_NEEDED":
            text += this.$t("scm.import.status.REFRESH_NEEDED.description");
            break;
          case "UNKNOWN":
            text += this.$t("scm.import.status.UNKNOWN.description");
            break;
          case "LOADING":
            text = this.$t("scm.import.status.LOADING.description");
            break;
          default:
            text += importStatus;
        }
      }
      return text;
    },
  },
});
</script>

<style scoped lang="scss">
.scm_status {
  margin-right: var(--spacing-2);
}
</style>
