<template>
  <span>
    <slot></slot>
    <span v-if="showIcon">
      <img v-if="iconUrl" class="plugin-icon" :src="iconUrl" />
      <i v-else-if="glyphicon" :class="'glyphicon glyphicon-' + glyphicon"></i>
      <i v-else-if="faicon" :class="'fas fa-' + faicon"></i>
      <i v-else-if="fabicon" :class="'fab fa-' + fabicon"></i>
      <i v-else class="rdicon icon-small plugin"></i>
    </span>
    <span v-if="showTitle" :class="titleCss" style="margin-left: 5px">
      {{ title }}
    </span>
    <slot name="description">
      <PluginDetails
        :show-description="showDescription"
        :show-extended="showExtended"
        :description="description"
        :description-css="descriptionCss"
        :extended-css="extendedCss"
      />
    </slot>

    <slot name="suffix"></slot>
  </span>
</template>
<script lang="ts">
import { defineComponent } from "vue";
import PluginDetails from "./PluginDetails.vue";

export default defineComponent({
  name: "PluginInfo",
  components: {
    PluginDetails,
  },
  props: {
    showIcon: {
      type: Boolean,
      default: true,
      required: false,
    },
    showTitle: {
      type: Boolean,
      default: true,
      required: false,
    },
    titleCss: {
      type: String,
      default: "text-strong",
      required: false,
    },
    showDescription: {
      type: Boolean,
      default: true,
      required: false,
    },
    descriptionCss: {
      type: String,
      default: "",
      required: false,
    },
    showExtended: {
      type: Boolean,
      default: true,
      required: false,
    },
    extendedCss: {
      type: String,
      default: "text-muted",
      required: false,
    },
    detail: {
      type: Object,
      required: true,
    },
  },
  data: function () {
    return {
      toggleExtended: false,
    };
  },
  computed: {
    description(): string {
      return this.detail.description || this.detail.desc;
    },
    title(): string {
      return this.detail.title;
    },
    providerMeta(): any {
      return (this.detail && this.detail.providerMetadata) || {};
    },
    iconUrl(): string {
      return this.detail.iconUrl;
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
.plugin-icon {
  width: 16px;
  height: 16px;
  border-radius: 2px;
}
</style>
