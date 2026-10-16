<template>
  <div>
    <div class="nodes-embed ansicolor-on matchednodes embed embed_clean">
      <div class="row">
        <template v-for="(node, i) in nodes" :key="`node-${i}`">
          <node-show-embed
            :node="node"
            :class="cssForNode(node.attributes)"
            :show-exclude-filter-links="showExcludeFilterLinks"
            @filter="filterClick"
          />
        </template>
      </div>
    </div>
    <slot></slot>
  </div>
</template>
<script lang="ts">
import NodeShowEmbed from "../../job/resources/NodeShowEmbed.vue";

import { defineComponent } from "vue";
import type { PropType } from "vue";
import { cssForNode, styleForNode } from "../../../utilities/nodeUi";

export default defineComponent({
  name: "NodeListEmbed",
  components: {
    NodeShowEmbed,
  },
  props: {
    nodes: {
      type: Array as PropType<any[]>,
      required: true,
    },
    tagsummary: {
      type: Object,
      required: false,
      default: () => {},
    },
    showExcludeFilterLinks: {
      type: Boolean,
      required: false,
      default: false,
    },
  },
  emits: ["filter"],
  methods: {
    cssForNode(node: any) {
      return cssForNode(node, this.nodes);
    },
    styleForNode(node: any) {
      return styleForNode(node);
    },
    filterClick(filter: any) {
      this.$emit("filter", filter);
    },
  },
});
</script>
