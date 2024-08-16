<template>
  <span
    v-if="showDescription && !inlineDescription"
    :class="descriptionCss"
    style="margin-left: 5px"
    data-testid="block-description"
  >
    {{ shortDescription }}
  </span>
  <details
    v-if="showDescription && showExtended && extraDescription"
    class="more-info"
    :class="extendedCss"
  >
    <summary>
      <span
        v-if="showDescription && inlineDescription"
        :class="descriptionCss"
        data-testid="inline-description"
      >
        {{ shortDescription }}
      </span>
      <span class="more-indicator-verbiage more-info-icon">
        {{ $t("more") }}
        <i class="glyphicon glyphicon-chevron-right" />
      </span>
      <span class="less-indicator-verbiage more-info-icon">
        {{ $t("less") }}
        <i class="glyphicon glyphicon-chevron-down" />
      </span>
    </summary>
    <div
      v-if="allowHtml"
      class="more-info-content"
      :class="markdownContainerCss"
      data-testid="markdown-container"
    >
      <VMarkdownView mode="" :content="extraDescription" />
    </div>

    <slot v-else name="extraDescriptionText"> {{ extraDescription }}</slot>
  </details>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { VMarkdownView } from "vue3-markdown";

export default defineComponent({
  name: "PluginDetails",
  components: { VMarkdownView },
  props: {
    showDescription: {
      type: Boolean,
      default: true,
      required: false,
    },
    showExtended: {
      type: Boolean,
      default: true,
      required: false,
    },
    description: {
      type: String,
      default: "",
      required: false,
    },
    descriptionCss: {
      type: String,
      default: "",
      required: false,
    },
    extendedCss: {
      type: String,
      default: "text-muted details-reset",
      required: false,
    },
    markdownContainerCss: {
      type: String,
      default: "",
      required: false,
    },
    inlineDescription: {
      type: Boolean,
      default: false,
    },
    cutoffMarker: {
      type: String,
      default: "",
    },
    allowHtml: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    shortDescription(): string {
      const desc = this.description;
      if (desc && desc.indexOf("\n") > 0) {
        return desc.substring(0, desc.indexOf("\n"));
      }
      return desc;
    },
    splitText(): string {
      return this.description.substring(this.description.indexOf("\n") + 1);
    },
    extraDescription(): string | null {
      const desc = this.description;

      if (this.cutoffMarker.length > 0) {
        const remainingLinesMassagedMarker = this.splitText.split(
          new RegExp(
            "(^|\n|\r\n)" +
              this.splitText.replace(/[.*+?^${}()|[\]\\]/g, "\\$&") +
              "(\n|\r\n)",
          ),
          2,
        );
        return remainingLinesMassagedMarker.length > 0
          ? remainingLinesMassagedMarker[0]
          : this.splitText;
      }

      if (desc && desc.indexOf("\n") > 0) {
        return this.splitText;
      }

      return "";
    },
  },
});
</script>
<style scoped>
.m-0 {
  margin: 0 !important;
}
.p-0 {
  padding: 0 !important;
}
.more-info-icon {
  margin-left: 5px;
}
</style>
