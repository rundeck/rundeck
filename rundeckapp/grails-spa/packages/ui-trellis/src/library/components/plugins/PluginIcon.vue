<template>
  <span :class="iconClass">
    <img
      v-if="iconUrl"
      data-testid="plugin-icon-image"
      class="plugin-icon"
      :src="iconUrl"
      alt=""
    />
    <i
      v-else-if="glyphicon"
      data-testid="plugin-icon-glyphicon"
      :class="'glyphicon glyphicon-' + glyphicon"
    ></i>
    <i
      v-else-if="faicon"
      data-testid="plugin-icon-faicon"
      :class="'fas fa-' + faicon"
    ></i>
    <i
      v-else-if="fabicon"
      data-testid="plugin-icon-fabicon"
      :class="'fab fa-' + fabicon"
    ></i>
    <i
      v-else
      data-testid="plugin-icon-default"
      class="rdicon icon-small plugin"
    ></i>
  </span>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";
import type { Plugin } from "../../stores/Plugins";

export default defineComponent({
  name: "PluginIcon",
  props: {
    detail: {
      type: Object as PropType<Partial<Plugin>>,
      required: true,
    },
    iconClass: {
      type: String,
      default: "plugin-icon-wrapper",
      required: false,
    },
  },
  computed: {
    providerMeta(): NonNullable<Plugin["providerMetadata"]> {
      return (this.detail && this.detail.providerMetadata) || {};
    },
    iconUrl(): string | undefined {
      return this.detail?.iconUrl;
    },
    glyphicon(): string | undefined {
      return this.providerMeta.glyphicon;
    },
    faicon(): string | undefined {
      return this.providerMeta.faicon;
    },
    fabicon(): string | undefined {
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
