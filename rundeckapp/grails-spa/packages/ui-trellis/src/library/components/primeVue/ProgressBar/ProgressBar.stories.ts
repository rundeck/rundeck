import type { Meta, StoryObj } from "@storybook/vue3";
import "./progressBar.scss";

import ProgressBar from "primevue/progressbar";

const meta: Meta<typeof ProgressBar> = {
  component: ProgressBar,
  argTypes: {
    value: {
      control: {
        type: "number",
      },
    },
    mode: {
      control: {
        type: "select",
        options: ["determinate", "indeterminate"],
      },
    },
  },
  tags: ["autodocs"],
};
export default meta;

type Story = StoryObj<typeof ProgressBar>;

export const Default: Story = {
  render: (args) => ({
    components: { ProgressBar },
    name: "Default ProgressBar",
    setup() {
      return { args };
    },
    template: `
      <ProgressBar v-bind="args"/>`,
  }),
  args: {
    value: 30,
    mode: "determinate",
  },
};

export const Indeterminate: Story = {
  render: (args) => ({
    components: { ProgressBar },
    name: "Indeterminate ProgressBar",
    setup() {
      return { args };
    },
    template: `
      <ProgressBar v-bind="args"/>`,
  }),
  args: {
    value: 30,
    mode: "indeterminate",
  },
};
