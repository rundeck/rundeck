<template>
  <span class="btn-group">
    <btn
      size="sm"
      class="autohilite"
      style="border-right: 0"
      title="Click to Edit Filter"
      @click="$emit('editFilter')"
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
      title="Remove Filter"
      @click="$emit('removeFilter')"
    >
      <i class="glyphicon glyphicon-remove"></i
    ></btn>
  </span>
</template>
<script lang="ts">
import pluginInfo from "../../../../library/components/plugins/PluginInfo.vue";
import { defineComponent } from "vue";

export default defineComponent({
  name: "LogFilterButton",
  components: {
    pluginInfo,
  },
  props: {
    pluginDescription: {
      type: Object,
      required: true,
    },
  },
  emits: ["editFilter", "removeFilter"],
});
</script>
<style scoped lang="scss">
.btn-group {
  display: inline-flex;

  & > .btn > span {
    display: inline-flex;
    align-items: center;
  }
}
</style>
