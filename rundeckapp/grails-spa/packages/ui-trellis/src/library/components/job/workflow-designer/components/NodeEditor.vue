<template>
  <div v-if="node">
    <h3>{{ title }}</h3>
    {{ node.rules }}
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";

import { GNode } from "../Graph";

export default defineComponent({
  name: "NodeEditor",
  props: {
    node: {
      type: Object as PropType<GNode>,
      required: true,
    },
  },
  computed: {
    conditions(): string {
      return "Foo";
    },
    title(): string {
      return this.node ? this.node.label || this.node.identifier : "Node Edit";
    },
  },
});
</script>
