<template>
  <!-- icon-only, so the warning must be reachable without a mouse -->
  <span
    v-if="isAtRisk"
    v-tooltip.focus="warningTitle"
    class="text-warning execution-acl-warning"
    data-testid="execution-acl-warning"
    tabindex="0"
    role="img"
    :aria-label="warningTitle"
    :title="warningTitle"
  >
    <i class="glyphicon glyphicon-warning-sign" aria-hidden="true"></i>
  </span>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";

/** The `executionAclValid` meta entry, as the job metadata component emits it. */
interface ExecutionAclMeta {
  /** whether the job's saved owner is still authorized to run it */
  valid?: boolean;
  /** the saved owner; absent when the job has none */
  user?: string;
  /** whether the trigger now skips the run, rather than only warning about it */
  enforced?: boolean;
}

/** The socket-data the job browse row passes to widgets at this location. */
interface JobBrowseItemData {
  job?: Record<string, unknown>;
  meta?: ExecutionAclMeta;
}

export default defineComponent({
  name: "JobExecutionAclWarningDisplay",
  props: {
    itemData: {
      type: Object as PropType<JobBrowseItemData>,
      default: () => ({}),
    },
  },
  computed: {
    isAtRisk(): boolean {
      return this.itemData?.meta?.valid === false;
    },
    warningTitle(): string {
      // enforced means the run is already being skipped, so the wording says so
      // rather than warning about something that might happen later
      const enforced = this.itemData?.meta?.enforced === true;
      // no saved user means there is nobody to grant access to, so the
      // grant-or-re-save wording would point at an impossible action
      const user = this.itemData?.meta?.user;
      if (!user) {
        return this.$t(
          enforced
            ? "job.execution.acl.missing.owner.blocked.warning.title"
            : "job.execution.acl.missing.owner.warning.title",
        ) as string;
      }
      return this.$t(
        enforced
          ? "job.execution.acl.invalid.blocked.warning.title"
          : "job.execution.acl.invalid.warning.title",
        { user },
      ) as string;
    },
  },
});
</script>

<style scoped lang="scss">
.execution-acl-warning {
  margin-left: 4px;
}
</style>
