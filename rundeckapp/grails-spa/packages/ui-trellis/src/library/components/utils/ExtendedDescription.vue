<template>
  <span>
    <details v-if="extraDescription" class="more-info" :class="extendedCss">
      <summary>
        <span :class="descriptionCss">{{ shortDescription }}</span>
        <span class="more-indicator-verbiage btn-link btn-xs"
          >More &hellip;
        </span>
        <span class="less-indicator-verbiage btn-link btn-xs">Less </span>
      </summary>
      <div class="more-info-content">
        <VMarkdownView
          class="markdown-body"
          mode=""
          :content="extraDescription"
        />
      </div>
    </details>
    <span v-else :class="basicCss">{{ text }}</span>
  </span>
</template>
<script lang="ts">
import { VMarkdownView } from "vue3-markdown";
import { defineComponent } from "vue";

export default defineComponent({
  name: "ExtendedDescription",
  components: { VMarkdownView },
  props: {
    text: {
      type: String,
      required: false,
      default: "",
    },
    extendedCss: {
      type: String,
      required: false,
      default: "",
    },
    descriptionCss: {
      type: String,
      required: false,
      default: "",
    },
    basicCss: {
      type: String,
      required: false,
      default: "",
    },
  },
  computed: {
    shortDescription(): string {
      const desc = this.text;
      if (desc && desc.indexOf("\n") > 0) {
        return desc.substring(0, desc.indexOf("\n"));
      }
      return desc;
    },
    extraDescription(): string | null {
      const desc = this.text;
      if (desc && desc.indexOf("\n") > 0) {
        return desc.substring(desc.indexOf("\n") + 1);
      }
      return null;
    },
  },
});
</script>
<!-- vue3-markdown's base stylesheet must load as real top-level CSS to be
     inlined/resolved correctly; nesting the @import inside a selector (even
     under :deep()) produces literal, unresolved, invalid CSS (verified in
     the built output). .markdown-body is already treated as a global class
     elsewhere in the theme (see _code.scss), so this stays unscoped too. -->
<!-- eslint-disable-next-line vue/enforce-style-attribute -->
<style lang="scss">
@import "~vue3-markdown/dist/vue3-markdown.css";
</style>
