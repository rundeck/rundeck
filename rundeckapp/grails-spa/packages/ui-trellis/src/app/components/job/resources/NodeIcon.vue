<template>
  <span
    :class="cssForIcon(node.attributes)"
    :style="styleForIcon(node.attributes)"
  >
    <i
      v-if="node.attributes['ui:icon:name']"
      :class="glyphiconForName(node.attributes['ui:icon:name'])"
    ></i>

    <i v-if="!node.attributes['ui:icon:name']" :class="iconCss"></i>
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
