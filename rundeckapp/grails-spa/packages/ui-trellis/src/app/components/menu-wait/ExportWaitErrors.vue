<template>
  <div class="export-wait-errors">
    <div
      v-if="notFound"
      class="panel panel-danger panel-tab-content"
      data-testid="not-found-panel"
    >
      <div class="panel-heading">
        {{ $t("request.error.notfound.title") }}
      </div>
      <div class="panel-body">
        <i18n-t keypath="archive.request.token.not.found" tag="span">
          <template #token
            ><code>{{ token }}</code></template
          >
        </i18n-t>
      </div>
    </div>

    <div
      v-if="errorMessage"
      class="panel panel-danger panel-tab-content"
      data-testid="error-message-panel"
    >
      <div class="panel-heading">
        {{ $t("archive.request.error") }}
      </div>
      <div class="panel-body">{{ errorMessage }}</div>
    </div>

    <div
      v-if="errors.length > 0"
      class="panel panel-danger panel-tab-content"
      data-testid="errors-panel"
    >
      <div class="panel-heading">
        <i18n-t keypath="export.another.instance.process" tag="span">
          <template #project
            ><b>{{ project }}</b></template
          >
          <template #instance
            ><b>{{ instance }}</b></template
          >
        </i18n-t>
      </div>
      <div class="panel-body">
        <ul class="list-unstyled">
          <li v-for="(err, idx) in errors" :key="idx">
            <pre>{{ err }}</pre>
          </li>
        </ul>
        <a
          v-if="instanceLink"
          :href="instanceLink"
          data-testid="instance-link"
          >{{ $t("export.another.instance.goto") }}</a
        >
      </div>
      <div class="panel-footer">
        <a class="btn btn-default btn-sm" :href="returnUrl">{{
          $t("return.to.configuration")
        }}</a>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { buildInstanceLink } from "./services/instanceLink";

/**
 * Danger panels of the export wait page: token not found, request error
 * message, and instance export errors.
 */
export default defineComponent({
  name: "ExportWaitErrors",
  props: {
    project: { type: String, required: true },
    token: { type: String, default: "" },
    instance: { type: String, default: null },
    iproject: { type: String, default: null },
    returnUrl: { type: String, required: true },
    notFound: { type: Boolean, default: false },
    errorMessage: { type: String, default: null },
    errors: { type: Array as PropType<string[]>, default: () => [] },
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
