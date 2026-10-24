<template>
  <span :class="cssForIcon(attributes)" :style="styleForIcon(attributes)">
    <i
      v-if="attributes['ui:icon:name']"
      :class="glyphiconForName(attributes['ui:icon:name'])"
    ></i>

    <i v-if="!attributes['ui:icon:name']" :class="iconCss"></i>
  </span>
</template>
<script lang="ts">
import { defineComponent, type PropType } from "vue";
import {
  cssForIcon,
  styleForIcon,
  glyphiconForName,
} from "../../../utilities/nodeUi";
import type { Node, NodeAttributes } from "./types/nodeTypes";

export default defineComponent({
  name: "NodeIcon",
  props: {
    node: {
      type: Object as PropType<Node>,
      required: true,
    },
    defaultIconCss: {
      type: String,
      required: false,
      default: "fas fa-hdd",
    },
  },
  computed: {
    attributes(): NodeAttributes {
      return this.node.attributes ?? {};
    },
    iconCss(): string {
      if (!this.defaultIconCss) {
        return "far fa-circle";
      }
      return this.defaultIconCss;
    },
  },
  methods: {
    cssForIcon(attrs: NodeAttributes) {
      return cssForIcon(attrs);
    },
    styleForIcon(attrs: NodeAttributes) {
      return styleForIcon(attrs);
    },
    glyphiconForName(name: string) {
      return glyphiconForName(name);
    },
  },
});
</script>
