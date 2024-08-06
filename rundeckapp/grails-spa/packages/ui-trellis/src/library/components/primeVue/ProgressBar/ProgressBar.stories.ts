import type { Meta, StoryObj } from "@storybook/vue3";
import "./progressBar.scss";

import ProgressBar from "primevue/progressbar";

const meta: Meta<typeof ProgressBar> = {
  parameters: {
    docs: {
      componentSubtitle: "ProgressBar is a process status indicator.",
    },
    actions: {
      disable: true,
    },
    controls: {
      disable: true,
    },
  },
  argTypes: {
    value: {
      control: {
        type: "number",
      },
      type: "number",
      description: "Current value of the progress.",
    },
    mode: {
      options: ["determinate", "indeterminate"],
      control: {
        type: "select",
      },
      table: {
        defaultValue: { summary: "determinate" },
      },
      description: "Defines the mode of the progress bar.",
    },
  },
  args: {
    value: 20,
    mode: "determinate",
  },
};
export default meta;

type Story = StoryObj<typeof ProgressBar>;

export const Playground: Story = {
  name: "Playground",
  render: (args) => ({
    components: { ProgressBar },
    setup: () => ({ args }),
    template: `
      <ProgressBar :value="args.value" :mode="args.mode" />`,
  }),
};

export const Default: Story = {
  render: (args) => ({
    components: { ProgressBar },
    setup: () => ({ args }),
    template: `
      <ProgressBar v-bind="args"/>`,
  }),
  args: {
    value: 30,
    mode: "determinate",
  },
};

export const Indeterminate: Story = {
  name: "Indeterminate ProgressBar",
  render: (args) => ({
    components: { ProgressBar },
    setup: () => ({ args }),
    template: `
      <ProgressBar v-bind="args"/>`,
  }),
  args: {
    value: 30,
    mode: "indeterminate",
  },
  parameters: {
    docs: {
      description: {
        story:
          "For progresses with no value to track, set the mode property to indeterminate.",
      },
    },
  },
};
