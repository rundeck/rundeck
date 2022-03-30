<script setup lang="ts">
const { t } = useI18n()
</script>

<template>
  <div>
    {{ t('not-found') }}
  </div>
</template>

<route lang="yaml">
meta:
  layout: NotFoundLayout
</route>
