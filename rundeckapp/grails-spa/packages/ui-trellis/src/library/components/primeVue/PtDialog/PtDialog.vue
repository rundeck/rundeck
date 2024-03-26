<template>
  <Dialog></Dialog>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import Dialog from "primevue/dialog";

export default defineComponent({
  name: "PtDialog",
  // eslint-disable-next-line vue/no-reserved-component-names
  components: { Dialog },
  props: {},
});
</script>

<style lang="scss"></style>
