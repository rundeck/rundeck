<template>
  <div>
    <DynamicFormPluginProp
      :fields="fields"
      :options="options"
      :has-options="hasOptions"
      :element="element"
      :name="name"
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
};
</script>

<style></style>
