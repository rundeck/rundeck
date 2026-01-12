<template>
  <span :class="iconClass">
    <img v-if="iconUrl" class="plugin-icon" :src="iconUrl" />
    <i v-else-if="glyphicon" :class="'glyphicon glyphicon-' + glyphicon"></i>
    <i v-else-if="faicon" :class="'fas fa-' + faicon"></i>
    <i v-else-if="fabicon" :class="'fab fa-' + fabicon"></i>
    <i v-else class="rdicon icon-small plugin"></i>
  </span>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "PluginIcon",
  props: {
    detail: {
      type: Object,
      required: true,
    },
    iconClass: {
      type: String,
      default: "plugin-icon-wrapper",
      required: false,
    },
  },
  computed: {
    providerMeta(): any {
      return (this.detail && this.detail.providerMetadata) || {};
    },
    iconUrl(): string {
      return this.detail?.iconUrl;
    },
    glyphicon(): string {
      return this.providerMeta.glyphicon;
    },
    faicon(): string {
      return this.providerMeta.faicon;
    },
    fabicon(): string {
      return this.providerMeta.fabicon;
    },
  },
});
</script>

<style scoped lang="scss">
.plugin-icon-wrapper {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
}

.plugin-icon {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
</style>
