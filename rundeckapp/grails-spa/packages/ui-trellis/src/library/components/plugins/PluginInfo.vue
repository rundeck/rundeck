<template>
  <span>
    <slot></slot>
    <span v-if="showIcon">
      <img class="plugin-icon" :src="iconUrl" v-if="iconUrl" />
      <i :class="'glyphicon glyphicon-' + glyphicon" v-else-if="glyphicon"></i>
      <i :class="'fas fa-' + faicon" v-else-if="faicon"></i>
      <i :class="'fab fa-' + fabicon" v-else-if="fabicon"></i>
      <i class="rdicon icon-small plugin" v-else></i>
    </span>
    <span :class="titleCss" v-if="showTitle" style="margin-left: 5px">{{
      title
    }}</span>
    <PluginDetails
      :showDescription="showDescription"
      :showExtended="showExtended"
      :description="description"
      :descriptionCss="descriptionCss"
      :extendedCss="extendedCss"
    ></PluginDetails>

    <slot name="suffix"></slot>
  </span>
</template>
<script lang="ts">
import Vue from "vue";
import PluginDetails from "./PluginDetails.vue";

export default Vue.extend({
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
