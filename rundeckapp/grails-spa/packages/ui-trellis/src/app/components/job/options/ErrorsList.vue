<template>
  <template v-if="typeof errors === 'string'">
    {{ errors }}
  </template>
  <template v-else-if="errors.length === 1">
    {{ errors[0] }}
  </template>
  <template v-else-if="errors.length > 1">
    <ul>
      <li v-for="err in errors">{{ err }}</li>
    </ul>
  </template>
</template>
<script lang="ts">

import {PropType} from "vue";

export default {
  name: "ErrorsList",
  props: {
    errors: {
      type: [String, Array as PropType<String[]>],
      required: true,
    },
  },
};
</script>
<style scoped lang="scss">
.flow > * + * {
  margin-top: var(--spacing-2);
}

.flow-h > * + * {
  margin-left: var(--spacing-2);
}
</style>
