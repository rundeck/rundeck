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
      <span
        class="has_tooltip text-info"
        data-placement="bottom"
        data-container="body"
        :title="$t('acl.validation.error.sourceIdentity.help')"
        :aria-label="$t('acl.validation.error.sourceIdentity.help')"
        role="img"
        data-testid="acl-validation-identity-help"
      >
        <i class="glyphicon glyphicon-question-sign" aria-hidden="true"></i>
      </span>
      <ol>
        <li
          v-for="(message, index) in entry.messages"
          :key="`${entry.document}-message-${index}`"
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

import type { AclValidation, AclValidationEntry } from "./types/aclFileTypes";

export default defineComponent({
  name: "AclValidationReport",
  props: {
    validation: {
      type: Object as PropType<AclValidation>,
      required: true,
    },
  },
  computed: {
    validationEntries(): AclValidationEntry[] {
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
