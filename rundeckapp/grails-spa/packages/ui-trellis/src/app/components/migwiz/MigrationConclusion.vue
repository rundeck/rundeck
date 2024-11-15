<template>
  <div class="flex flex-col step-container">
    <div>
      <p>{{ $t("migwiz.congratulations") }}</p>
      <p>{{ $t("migwiz.reminder") }}</p>
    </div>
    <a :href="instanceUrl" target="_blank" class="btn btn-cta">{{
      $t("migwiz.accessYourTrial")
    }}</a>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "MigrationConclusion",
  props: {
    instanceUrl: {
      type: String,
      required: true,
    },
  },
});
</script>

<style scoped lang="scss">
.step-container {
  gap: 40px;
}

.btn {
  align-self: flex-end;
}
</style>
