<template>
  <span>
    <slot></slot>
    <PluginIcon v-if="showIcon" :detail="detail" />
    <slot name="titleprefix"> </slot>
    <span v-if="showTitle" :class="titleCss" style="margin-left: 5px">
      {{ title }}
    </span>
    <slot name="descriptionprefix"> </slot>
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
import PluginIcon from "./PluginIcon.vue";

export default defineComponent({
  name: "PluginInfo",
  components: {
    PluginDetails,
    PluginIcon,
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
  },
});
</script>

<style scoped lang="scss">
:deep(.plugin-icon) {
  width: 16px;
  height: 16px;
  border-radius: 2px;
}
</style>
