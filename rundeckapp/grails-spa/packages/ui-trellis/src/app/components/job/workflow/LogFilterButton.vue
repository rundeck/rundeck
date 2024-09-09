<template>
  <span class="btn-group">
    <btn
      size="sm"
      class="autohilite"
      style="border-right: 0"
      @click="$emit('editFilter')"
      title="Click to Edit Filter"
    >
      <plugin-info
        :detail="pluginDescription"
        :show-description="false"
        :show-extended="false"
      />
    </btn>
    <btn
      size="sm"
      type="danger"
      @click="$emit('removeFilter')"
      title="Remove Filter"
    >
      <i class="glyphicon glyphicon-remove"></i
    ></btn>
  </span>
</template>
<script lang="ts">
import pluginInfo from "@/library/components/plugins/PluginInfo.vue";
import { defineComponent } from "vue";

export default defineComponent({
  name: "LogFilterButton",
  emits: ["editFilter", "removeFilter"],
  components: {
    pluginInfo,
  },
  props: {
    pluginDescription: {
      type: Object,
      required: true,
    },
  },
});
</script>
<style scoped lang="scss"></style>
