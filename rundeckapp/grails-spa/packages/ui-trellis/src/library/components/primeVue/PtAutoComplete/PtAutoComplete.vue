<template>
  <AutoComplete></AutoComplete>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import AutoComplete from "primevue/autocomplete";

export default defineComponent({
  name: "PtAutoComplete",
  // eslint-disable-next-line vue/no-reserved-component-names
  components: { AutoComplete },
  props: {},
});
</script>

<style lang="scss">
.p-inputtext {
  background: var(--color-white);
}
</style>
