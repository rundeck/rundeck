<template>
  <AutoComplete
    v-model="value"
    :suggestions="suggestions"
    @complete="search"
  ></AutoComplete>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import AutoComplete, { AutoCompleteCompleteEvent } from "primevue/autocomplete";

export default defineComponent({
  name: "PtAutoComplete",
  // eslint-disable-next-line vue/no-reserved-component-names
  components: { AutoComplete },
  props: {
    modelValue: {
      type: String,
      required: true,
    },
    suggestions: {
      type: Array,
      default: () => [],
    },
  },
  emits: ["search"],
  data() {
    return {
      value: this.modelValue,
    };
  },
  watch: {
    modelValue(newVal) {
      this.value = newVal;
    },
  },
  methods: {
    search(event: AutoCompleteCompleteEvent) {
      this.$emit("search", event);
    },
  },
});
</script>

<style lang="scss">
.p-inputtext {
  background: var(--color-white);
}
</style>
