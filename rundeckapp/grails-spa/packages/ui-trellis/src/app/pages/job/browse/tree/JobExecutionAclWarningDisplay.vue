<template>
  <span
    v-if="isAtRisk"
    class="has_tooltip text-warning execution-acl-warning"
    data-testid="execution-acl-warning"
    :title="warningTitle"
    data-toggle="tooltip"
    data-container="#section-content"
    data-placement="auto bottom"
  >
    <i class="glyphicon glyphicon-warning-sign"></i>
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
      return this.$t("job.execution.acl.invalid.warning.title", {
        user: this.itemData?.meta?.user,
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
