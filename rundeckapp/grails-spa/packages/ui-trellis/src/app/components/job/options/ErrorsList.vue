<template>
  <template v-if="typeof(errors)==='string'">
    {{ errors }}
  </template>
  <template v-else-if="errors.length===1">
    {{ errors[0] }}
  </template>
  <template v-else-if="errors.length>1">
    <ul>
      <li v-for="err in errors">{{ err }}</li>
    </ul>
  </template>
</template>
<script lang="ts">
export default {
  name: 'ErrorsList',
  props: {
    errors: {
      type: Array,
      required: true,
    }
  }
}
</script>
<style scoped lang="scss">
.flow > * + * {
  margin-top: var(--spacing-2);
}

.flow-h > * + * {
  margin-left: var(--spacing-2);
}
</style>