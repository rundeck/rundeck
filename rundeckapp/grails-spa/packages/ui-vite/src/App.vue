<script setup lang="ts">
/**
 * https://github.com/vueuse/head
 * Allows you to manipulate the document head in any components they will be
 * rendered correctly in the html results with vite-ssg
 */
useHead({
  title: 'Pagerduty Process Automation',
  meta: [{ name: 'description', content: 'Opinionated Vite Starter Template' }],
})
</script>

<template>
  <router-view />
</template>
