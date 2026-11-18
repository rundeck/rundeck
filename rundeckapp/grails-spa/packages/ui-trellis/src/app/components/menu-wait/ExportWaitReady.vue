<template>
  <div class="panel panel-default panel-tab-content export-wait-ready">
    <div class="panel-heading">
      <template v-if="instance">
        <i18n-t keypath="export.another.instance.process" tag="span">
          <template #project
            ><b>{{ project }}</b></template
          >
          <template #instance
            ><b>{{ instance }}</b></template
          >
        </i18n-t>
      </template>
      <template v-else>
        <i18n-t keypath="archive.request.download.title" tag="span">
          <template #project
            ><strong>{{ project }}</strong></template
          >
        </i18n-t>
      </template>
    </div>
    <div class="panel-body">
      <template v-if="instance">
        <div class="alert alert-info">
          {{ $t("export.another.instance.success") }}
        </div>
        <a
          v-if="instanceLink"
          :href="instanceLink"
          data-testid="instance-link"
          >{{ $t("export.another.instance.goto.link") }}</a
        >
      </template>
      <template v-else>
        <a
          id="export-download-btn"
          class="btn btn-cta"
          :href="downloadUrl"
          data-testid="download-link"
        >
          <i class="glyphicon glyphicon-download-alt"></i>
          {{ project }}.rdproject.jar
        </a>
        <div class="text-info expire-note">
          {{ $t("archive.request.will.expire") }}
        </div>
      </template>
    </div>
    <div class="panel-footer">
      <a
        class="btn btn-default btn-sm"
        :href="returnUrl"
        data-testid="return-link"
        >{{ $t("return.to.configuration") }}</a
      >
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { buildInstanceLink } from "./services/instanceLink";

/**
 * Ready panel of the export wait page: download link (or instance export
 * success message) and the return link.
 */
export default defineComponent({
  name: "ExportWaitReady",
  props: {
    project: { type: String, required: true },
    instance: { type: String, default: null },
    iproject: { type: String, default: null },
    downloadUrl: { type: String, required: true },
    returnUrl: { type: String, required: true },
  },
  computed: {
    /**
     * Link to the target project on the other instance.
     * @returns the href, or null when the instance is not an http(s) URL
     */
    instanceLink(): string | null {
      return buildInstanceLink(this.instance, this.iproject);
    },
  },
});
</script>

<style scoped lang="scss">
.expire-note {
  margin-top: var(--spacing-text-gap);
}
</style>
