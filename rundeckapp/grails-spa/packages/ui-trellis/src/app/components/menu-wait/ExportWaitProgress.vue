<template>
  <div class="panel panel-default panel-tab-content export-wait-progress">
    <div class="panel-heading">
      <i18n-t keypath="archive.request.exporting.title" tag="span">
        <template #project
          ><strong>{{ project }}</strong></template
        >
      </i18n-t>
    </div>
    <div class="panel-body">
      <div class="container">
        <div class="col-md-12 text-info progress-message">
          {{ $t("archive.request.please.wait") }}
        </div>
        <div class="col-md-6">
          <PtProgressBar :value="percentage" />
        </div>
      </div>
    </div>
    <div class="panel-footer form-inline">
      <a
        :href="refreshUrl"
        class="btn btn-default btn-sm reload_button"
        :data-loading="$t('menu.wait.loading')"
        data-testid="refresh-link"
      >
        <i class="glyphicon glyphicon-refresh"></i>
        {{ $t("refresh.this.page") }}
      </a>
      <div class="checkbox">
        <input
          id="dorefresh"
          type="checkbox"
          value="true"
          :checked="refresh"
          @change="onToggle"
        />
        <label for="dorefresh">{{ $t("refresh.every.5.seconds") }}</label>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import PtProgressBar from "../../../library/components/primeVue/PtProgressBar/PtProgressBar.vue";

/**
 * In-progress panel of the export wait page: progress bar, refresh link and
 * the auto refresh toggle.
 */
export default defineComponent({
  name: "ExportWaitProgress",
  components: { PtProgressBar },
  props: {
    project: { type: String, required: true },
    percentage: { type: Number, default: 0 },
    refreshUrl: { type: String, required: true },
    refresh: { type: Boolean, default: true },
  },
  emits: ["update:refresh"],
  methods: {
    /**
     * Emit the new state of the auto refresh checkbox.
     * @param event change event of the checkbox
     */
    onToggle(event: Event): void {
      this.$emit("update:refresh", (event.target as HTMLInputElement).checked);
    },
  },
});
</script>

<style scoped lang="scss">
.progress-message {
  margin-bottom: var(--spacing-text-gap);
}
</style>
