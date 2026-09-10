<template>
  <div class="log-line">
    <span class="gutter">{{ entry.node }}</span>
    <!-- eslint-disable-next-line vue/no-v-html -- sanitizedLogHtml is passed through DOMPurify.sanitize() in the computed below -->
    <span v-if="entry.logHtml" v-html="sanitizedLogHtml" />
    <span v-if="!entry.logHtml">{{ entry.log }}</span>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import type { PropType } from "vue";
import DOMPurify from "dompurify";
import { ExecutionOutputEntry } from "../../stores/ExecutionOutput";

export default defineComponent({
  props: {
    entry: {
      type: Object as PropType<ExecutionOutputEntry>,
      required: true,
    },
  },
  computed: {
    sanitizedLogHtml(): string {
      return DOMPurify.sanitize(this.entry.logHtml || "");
    },
  },
});
</script>

<style lang="scss" scoped>
.log-line {
  display: block;
  width: 100%;
  font-family: monospace;
}

.gutter {
  user-select: none;
}
</style>
