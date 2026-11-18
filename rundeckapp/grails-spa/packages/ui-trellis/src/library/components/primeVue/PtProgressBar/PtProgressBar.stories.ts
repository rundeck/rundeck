import type { Meta, StoryObj } from "@storybook/vue3";

import PtProgressBar from "./PtProgressBar.vue";

const meta: Meta<typeof PtProgressBar> = {
  title: "PtProgressBar",
  component: PtProgressBar,
  parameters: {
    componentSubtitle:
      "Process status indicator. Values are clamped to 0-100 and a screen-reader '% Complete' label is rendered.",
    actions: {
      disable: true,
    },
  },
  argTypes: {
    value: {
      control: { type: "number" },
      type: "number",
      description: "Current progress (clamped to 0-100).",
    },
    mode: {
      options: ["determinate", "indeterminate"],
      control: { type: "select" },
      type: "string",
      description: "Determinate shows a value; indeterminate animates.",
    },
    showValue: {
      control: { type: "boolean" },
      type: "boolean",
      description: "Whether to render the 'N%' label inside the bar.",
    },
  },
  args: {
    value: 50,
    mode: "determinate",
    showValue: true,
  },
};

export default meta;

type Story = StoryObj<typeof PtProgressBar>;

export const Playground: Story = {
  name: "Playground",
  tags: ["!dev"],
  render: (args) => ({
    components: { PtProgressBar },
    setup: () => ({ args }),
    template: `<PtProgressBar :value="args.value" :mode="args.mode" :show-value="args.showValue" />`,
  }),
};

const generateTemplate = () => `<div><PtProgressBar v-bind="args" /></div>`;

export const Determinate: Story = {
  render: (args) => ({
    components: { PtProgressBar },
    setup() {
      return { args };
    },
    template: generateTemplate(),
  }),
  args: { value: 40 },
};

export const Overrun: Story = {
  render: (args) => ({
    components: { PtProgressBar },
    setup() {
      return { args };
    },
    template: generateTemplate(),
  }),
  args: { value: 130 },
};

export const Indeterminate: Story = {
  render: (args) => ({
    components: { PtProgressBar },
    setup() {
      return { args };
    },
    template: generateTemplate(),
  }),
  args: { mode: "indeterminate" },
};
