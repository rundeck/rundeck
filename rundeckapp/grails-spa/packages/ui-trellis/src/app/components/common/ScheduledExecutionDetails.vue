<template>
  <div v-if="allowHtml && !firstLineOnly">
    <div v-if="remainingLine.trim()" data-testid="details-container">
      <template v-if="mode === 'collapsed' || mode === 'expanded'">
        <details class="more-info" :open="mode === 'expanded'">
          <summary>
            <span :class="textCss">
              {{ firstLine }}
            </span>
            <span
              class="btn-link btn-xs more-indicator-verbiage"
              data-testid="see-more-button"
            >
              {{ moreText.length ? moreText : $t("more") }}
            </span>
            <span
              class="btn-link btn-xs less-indicator-verbiage"
              data-testid="see-less-button"
            >
              {{ $t("less") }}
            </span>
          </summary>
          <div
            class="more-info-content"
            :class="markdownCss"
            data-testid="more-content"
          >
            <VMarkdownView mode="" :content="remainingLine" />
          </div>
        </details>
      </template>
      <span v-else-if="mode !== 'hidden'" :class="markdownCss">
        <VMarkdownView mode="" :content="remainingLine" />
      </span>
    </div>
    <span v-else :class="textCss" data-testid="first-line-html-allowed">
      {{ firstLine }}
    </span>
  </div>
  <span v-else :class="textCss" data-testid="first-line-only">
    {{ firstLine }}
  </span>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { VMarkdownView } from "vue3-markdown";

export default defineComponent({
  name: "ScheduledExecutionDetails",
  components: {
    VMarkdownView,
  },
  props: {
    allowHtml: {
      type: Boolean,
      default: false,
    },
    firstLineOnly: {
      type: Boolean,
      default: false,
    },
    description: {
      type: String,
      required: true,
    },
    mode: {
      type: String,
      required: true,
    },
    markdownCss: {
      type: String,
      default: "",
    },
    textCss: {
      type: String,
      default: "",
    },
    cutoffMarker: {
      type: String,
      default: "",
    },
    moreText: {
      type: String,
      default: "",
    },
  },
  computed: {
    splitText() {
      const tempArray = this.description.split(/(\r\n?|\n)/, 2);
      tempArray[1] = this.description.substring(tempArray[0].length);
      return tempArray;
    },
    firstLine() {
      if (this.splitText.length > 0) {
        return this.splitText[0];
      }
      return this.description;
    },
    remainingLine() {
      // if there is a cutoffMarker, remaining line should be the remaining before the line that contains the marker
      if (this.cutoffMarker.length > 0 && this.splitText[1]) {
        const remainingLinesMassagedMarker = this.splitText[1].split(
          new RegExp(
            "(^|\n|\r\n)" +
              this.splitText[1].replace(/[.*+?^${}()|[\]\\]/g, "\\$&") +
              "(\n|\r\n)",
          ),
          2,
        );
        return remainingLinesMassagedMarker.length > 0
          ? remainingLinesMassagedMarker[0]
          : this.splitText[1];
      }
      return this.splitText.length === 2 ? this.splitText[1] : "";
    },
  },
});
</script>

<style scoped lang="scss"></style>
