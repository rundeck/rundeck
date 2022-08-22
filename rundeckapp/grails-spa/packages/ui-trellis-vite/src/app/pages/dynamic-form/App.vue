<template>
  <div>
    <DynamicFormPluginProp :fields="fields" :options="options" :hasOptions="hasOptions" :element="element" :name="name"></DynamicFormPluginProp>
  </div>
</template>

<script>
import DynamicFormPluginProp from "@/library/components/plugins/DynamicFormPluginProp.vue";

export default {
  name: 'App',
  props: ["fields", "options","element","hasOptions","name"],
  components: {
    DynamicFormPluginProp
  }
}
</script>

<style>
</style>
