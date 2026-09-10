<template>
  <!-- eslint-disable-next-line vue/no-v-html -- sanitizedRules is passed through DOMPurify.sanitize() in the computed below -->
  <pre v-html="sanitizedRules" />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import DOMPurify from "dompurify";

export default defineComponent({
  props: {
    rules: { type: String, default: "" },
  },
  computed: {
    sanitizedRules(): string {
      return DOMPurify.sanitize(this.rules || "");
    },
  },
});
</script>
