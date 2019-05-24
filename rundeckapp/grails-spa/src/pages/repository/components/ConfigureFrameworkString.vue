<template>
  <code>{{returnedString}}</code>
</template>
<script>
export default {
  name: "ConfigureFrameworkString",
  props: ["serviceName", "provider", "prop"],
  computed: {
    returnedString: function() {
      if (this.prop.defaultValue) {
        return `framework.plugin.${this.serviceName}.${this.provider.name}.${
          this.prop.name
        }=${this.prop.defaultValue}`;
      } else {
        return `framework.plugin.${this.serviceName}.${this.provider.name}.${
          this.prop.name
        }`;
      }
    }
  }
};
</script>
