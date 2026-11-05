<template>
  <div>
    <DynamicFormPluginProp
      :fields="fields"
      :options="options"
      :has-options="hasOptions"
      :element="element"
      :name="name"
      @update:model-value="onModelValueUpdate"
    ></DynamicFormPluginProp>
  </div>
</template>

<script>
import DynamicFormPluginProp from "../../../library/components/plugins/DynamicFormPluginProp.vue";

export default {
  name: "App",
  components: {
    DynamicFormPluginProp,
  },
  props: ["fields", "options", "element", "hasOptions", "name"],
  emits: ["update:modelValue"],
  methods: {
    onModelValueUpdate(value) {
      this.$emit("update:modelValue", value);
    },
  },
};
</script>
