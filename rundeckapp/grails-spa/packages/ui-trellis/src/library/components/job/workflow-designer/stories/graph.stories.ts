import type { Meta, StoryFn } from "@storybook/vue3";

import WorkflowGraph from "../WorkflowGraph.vue";

export default {
  title: "Graph Components",
  component: WorkflowGraph,
  argTypes: {
    modelValue: {},
  },
} as Meta<typeof WorkflowGraph>;

export const graph: StoryFn<typeof WorkflowGraph> = (args) => ({
  setup() {
    return { args };
  },
  mounted: function () {
    document.documentElement.dataset.colorTheme = "light";
    const el = this.$el as any;
    el.parentNode.style.height = "100vh";
  },

  template:
    '<div style="width: 100%;height: 100vh;"><WorkflowGraph v-bind="args" :interactive="true"/></div>',
  components: {
    WorkflowGraph,
  },
});
graph.args = {
  modelValue: "[4] run-after:2",
  nodes: [
    {
      identifier: "1",
      label: "Step A With Super Long Label That Will Not Fit Properly",
      icon: { class: "rdicon shell icon-small" },
    },
    {
      identifier: "2",
      label: "Step B",
      title: "PagerDuty / Incident / Add Responders",
      icon: {
        image: "/pub-pd/icon.png",
      },
    },
    {
      identifier: "3",
      label: "Step C",
      icon: { class: "rdicon shell icon-small icon-med" },
    },
    {
      identifier: "4",
      label: "Step D",
      icon: {
        image: "/pub-pd/icon.png",
      },
    },
    {
      identifier: "5",
      label: "Step E",
      icon: { class: "rdicon shell icon-small icon-med" },
    },
  ],
};
