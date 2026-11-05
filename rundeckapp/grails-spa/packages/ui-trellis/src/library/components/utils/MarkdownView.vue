<template>
  <div class="markdown-body" data-testid="markdown-view" v-html="html" />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import MarkdownIt from "markdown-it";
import DOMPurify from "dompurify";

/**
 * Renders markdown with the DOMPurify already declared by this package.
 * Raw HTML in the source is not interpreted, and the result is sanitized
 * before it is inserted.
 */
const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: false,
});

export default defineComponent({
  name: "MarkdownView",
  props: {
    content: {
      type: String,
      default: "",
    },
  },
  computed: {
    html(): string {
      return DOMPurify.sanitize(markdown.render(this.content || ""));
    },
  },
});
</script>
