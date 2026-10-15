<template>
  <ul data-testid="acl-validation-report">
    <li
      v-for="entry in validationEntries"
      :key="entry.document"
      data-testid="acl-validation-document"
    >
      <code data-testid="acl-validation-document-name">{{
        entry.document
      }}</code>
      <ol>
        <li
          v-for="message in entry.messages"
          :key="message"
          data-testid="acl-validation-message"
        >
          <code>{{ message }}</code>
        </li>
      </ol>
    </li>
  </ul>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";

import type { AclValidation } from "./types/aclFileTypes";

export default defineComponent({
  name: "AclValidationReport",
  props: {
    validation: {
      type: Object as PropType<AclValidation>,
      required: true,
    },
  },
  computed: {
    validationEntries(): Array<{ document: string; messages: string[] }> {
      return Object.keys(this.validation.errors)
        .sort()
        .map((document) => ({
          document,
          messages: this.validation.errors[document] || [],
        }));
    },
  },
});
</script>

<style scoped lang="scss">
ul,
ol {
  margin-bottom: var(--sizes-2);
}
</style>
