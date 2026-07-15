<template>
  <pre v-html="rules" />
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  props: {
    rules: { default: "" },
  },
});
</script>
