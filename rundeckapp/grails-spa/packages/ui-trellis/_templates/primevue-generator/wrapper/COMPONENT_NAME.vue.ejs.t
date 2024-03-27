---
to: <%= h.path.join(`src/library/components/primeVue/${componentName}`, `${componentName}.vue`) %>
unless_exists: true
---

<template>
  <<%= h.changeCase.pascal(componentName).replace('Pt','') %>></<%= h.changeCase.pascal(componentName).replace('Pt','') %>>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import <%= h.changeCase.pascal(componentName).replace('Pt','') %> from "primevue/<%= h.changeCase.lower(componentName).replace('pt','') %>";

export default defineComponent({
  name: "<%= h.changeCase.pascal(componentName) %>",
  // eslint-disable-next-line vue/no-reserved-component-names
  components: { <%= h.changeCase.pascal(componentName).replace('Pt','') %> },
  props: {},
});
</script>

<style lang="scss">
</style>
