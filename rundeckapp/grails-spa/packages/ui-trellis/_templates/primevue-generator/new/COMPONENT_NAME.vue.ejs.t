---
to: <%= h.path.join(`src/library/components/primeVue/${componentName}`, `${componentName}.vue`) %>
unless_exists: true
---

<template>
  <<%= h.changeCase.pascal(componentName) %>></<%= h.changeCase.pascal(componentName) %>>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import <%= h.changeCase.pascal(componentName) %> from "primevue/<%= h.changeCase.lower(componentName).replace('pt','') %>";

export default defineComponent({
  name: "<%= h.changeCase.pascal(componentName) %>",
  // eslint-disable-next-line vue/no-reserved-component-names
  components: { <%= h.changeCase.pascal(componentName) %> },
  props: {
  },
});
</script>

<style lang="scss">
</style>
