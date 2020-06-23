<template>
  <code>{{returnedString}}</code>
</template>
<script>
export default {
  name: "ConfigureFrameworkString",
  props: ["serviceName", "provider", "prop"],
  computed: {
    returnedString: function() {
      let propName = this.provider.fwkMapping[this.prop.name] || `framework.plugin.${this.serviceName}.${this.provider.name}.${this.prop.name}`
      return `${propName}=${this.prop.defaultValue || 'value'}`;
    }
  }
};
</script>
