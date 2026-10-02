<template>
  <!-- icon-only, so the warning must be reachable without a mouse -->
  <span
    v-if="isAtRisk"
    class="has_tooltip text-warning execution-acl-warning"
    data-testid="execution-acl-warning"
    tabindex="0"
    :aria-label="warningTitle"
    :title="warningTitle"
    data-toggle="tooltip"
    data-container="#section-content"
    data-placement="auto bottom"
  >
    <i class="glyphicon glyphicon-warning-sign" aria-hidden="true"></i>
  </span>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "JobExecutionAclWarningDisplay",
  props: {
    itemData: {
      type: Object,
      default: () => ({}),
    },
  },
  computed: {
    isAtRisk(): boolean {
      return this.itemData?.meta?.valid === false;
    },
    warningTitle(): string {
      // no saved user means there is nobody to grant access to, so the
      // grant-or-re-save wording would point at an impossible action
      const user = this.itemData?.meta?.user;
      if (!user) {
        return this.$t(
          "job.execution.acl.missing.owner.warning.title",
        ) as string;
      }
      return this.$t("job.execution.acl.invalid.warning.title", {
        user,
      }) as string;
    },
  },
});
</script>

<style scoped lang="scss">
.execution-acl-warning {
  margin-left: 4px;
}
</style>
