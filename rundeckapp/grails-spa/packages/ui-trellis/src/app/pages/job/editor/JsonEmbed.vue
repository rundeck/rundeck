<template>
  <div>
    <input v-model="json" type="hidden" :name="fieldName" />
  </div>
</template>
<script>
import { defineComponent } from "vue";

export default defineComponent({
  name: "JsonEmbed",
  props: ["fieldName", "outputData"],
  computed: {
    json() {
      return JSON.stringify(this.outputData);
    },
  },
});
</script>
