<template>
  <AutoComplete
    v-model="value"
    :suggestions="suggestions"
    :optionLabel="optionLabel"
    :name="name"
    @complete="onComplete"
    @change="onChange"
    @input="updateValue"
  ></AutoComplete>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import AutoComplete, {
  AutoCompleteCompleteEvent,
  AutoCompleteChangeEvent,
} from "primevue/autocomplete";


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
    defaultValue: {
      type: String,
      default: "",
    },
    name: {
      type: String,
      default: "",
    },
    optionLabel: {
      type: String,
      default: "label",
    },
  },
  emits: ["update:modelValue", "onChange", "onComplete"],
  data() {
    return {
      value: this.modelValue || this.defaultValue,
    };
  },
  watch: {
    modelValue(newVal) {
      this.value = newVal;
    },
  },
  methods: {
    onComplete(event: AutoCompleteCompleteEvent) {
      this.$emit("onComplete", event);
    },
    onChange(event: AutoCompleteChangeEvent) {
      this.$emit("onChange", event);
    },
    updateValue() {
      this.$emit("update:modelValue", this.value);
    },
  },
});
</script>

<style lang="scss">
.p-inputtext {
  background: var(--color-white);
}
</style>
