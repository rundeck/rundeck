<template>
  <template v-if="exportSynchState && exportAuthz">
    <li class="divider"></li>
    <li class="dropdown-header">{{ $t("scm.export.plugin") }}</li>

    <li v-for="action in exportActions">
      <a :title="action.description" :href="scmActionHref(action.id, 'export')">
        {{ action.title }}
      </a>
    </li>
    <li v-if="exportSynchState !== 'CREATE_NEEDED'">
      <a :href="scmDiffHref('export')">
        <job-scm-status-badge
          :notext="true"
          icon="glyphicon-eye-open"
          :exportStatus="exportSynchState"
        />
        <template v-if="exportSynchState === 'CLEAN'">
          {{ $t("scm.action.diff.clean.button.label") }}
        </template>
        <template v-else>
          {{ $t("scm.action.diff.button.label") }}
        </template>
      </a>
    </li>
  </template>
  <template v-if="importSynchState && importAuthz">
    <li class="divider"></li>
    <li class="dropdown-header">{{ $t("scm.import.plugin") }}</li>

    <li v-for="action in importActions">
      <a :title="action.description" :href="scmActionHref(action.id, 'import')">
        {{ action.title }}
      </a>
    </li>
  </template>
</template>

<script lang="ts">
import JobScmStatusBadge from "@/app/pages/job/browse/tree/JobScmStatusBadge.vue";
import { getRundeckContext } from "@/library";
import {
  JobPageStore,
  JobPageStoreInjectionKey,
} from "@/library/stores/JobPageStore";
import { JobBrowseItem, JobBrowseMeta } from "@/library/types/jobs/JobBrowse";
import { defineComponent, inject } from "vue";

const context = getRundeckContext();
export default defineComponent({
  name: "JobScmActions",
  components: { JobScmStatusBadge },
  props: {
    job: {
      type: Object as () => JobBrowseItem,
      required: true,
    },
  },
  setup() {
    return {
      jobPageStore: inject(JobPageStoreInjectionKey) as JobPageStore,
    };
  },
  methods: {
    scmHrefBase(integration: string): string {
      return `${context.rdBase}/project/${context.projectName}/job/${this.job.id}/scm/${integration}`;
    },
    scmActionHref(id: string, integration: string): string {
      return `${this.scmHrefBase(integration)}/performAction?actionId=${id}`;
    },
    scmDiffHref(integration: string): string {
      return `${this.scmHrefBase(integration)}/diff`;
    },
  },
  computed: {
    exportAuthz(): boolean {
      return (
        this.jobPageStore.projAuthz["export"] ||
        this.jobPageStore.projAuthz["scm_export"]
      );
    },
    importAuthz(): boolean {
      return (
        this.jobPageStore.projAuthz["import"] ||
        this.jobPageStore.projAuthz["scm_import"]
      );
    },
    exportActions(): any[] | undefined {
      return this.scmExport?.jobState?.actions;
    },
    importActions(): any[] | undefined {
      return this.scmImport?.jobState?.actions;
    },
    exportSynchState(): String | undefined {
      return this.scmExport?.jobState?.synchState;
    },
    importSynchState(): String | undefined {
      return this.scmImport?.jobState?.synchState;
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
  },
});
</script>

<style scoped lang="scss"></style>
