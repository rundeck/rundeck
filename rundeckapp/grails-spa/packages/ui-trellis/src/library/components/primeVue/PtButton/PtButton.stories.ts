import type { Meta, StoryObj } from "@storybook/vue3";
import PtButton from "./PtButton.vue";

const meta: Meta<typeof PtButton> = {
  component: PtButton,
  argTypes: {
    severity: {
      control: {
        type: "select",
        options: ["sucess", "warning", "danger", "secondary"],
      },
    },
  },
  decorators: [
    () => ({
      template:
        '<div style="margin: 1em; display: flex; gap: 10px; align-items: flex-end"><story /></div>',
    }),
  ],
  tags: ["autodocs"],
};
export default meta;

type Story = StoryObj<typeof PtButton>;

export const Default: Story = {
  render: (args) => ({
    components: { PtButton },
    name: "Default badge",
    setup() {
      return { args };
    },
    template: `
      <PtButton v-bind="args" />`,
  }),
  args: {
    label: "Submit",
  },
};

export const DefaultGhost: Story = {
  render: (args) => ({
    components: { PtButton },
    setup() {
      return { args };
    },
    template: `
      <PtButton v-bind="args" />`,
  }),
  args: {
    label: "Submit",
    text: true,
  },
};

export const Danger: Story = {
  render: (args) => ({
    components: { PtButton },
    name: "Danger button",
    setup() {
      return { args };
    },
    template: `
      <PtButton v-bind="args" />`,
  }),
  args: {
    label: "Submit",
    severity: "danger",
  },
};

export const DangerGhost: Story = {
  render: (args) => ({
    components: { PtButton },
    setup() {
      return { args };
    },
    template: `
      <PtButton v-bind="args" />`,
  }),
  args: {
    label: "Submit",
    severity: "danger",
    text: true,
  },
};

export const Secondary: Story = {
  render: (args) => ({
    components: { PtButton },
    name: "Secondary button",
    setup() {
      return { args };
    },
    template: `
      <PtButton v-bind="args" />`,
  }),
  args: {
    label: "Submit",
    severity: "secondary",
    outlined: true,
  },
};

export const SecondaryGhost: Story = {
  render: (args) => ({
    components: { PtButton },
    setup() {
      return { args };
    },
    template: `
      <PtButton v-bind="args" />`,
  }),
  args: {
    label: "Submit",
    severity: "secondary",
    text: true,
  },
};

export const Disabled: Story = {
  render: (args) => ({
    components: { PtButton },
    setup() {
      return { args };
    },
    template: `
      <PtButton v-bind="args" />`,
  }),
  args: {
    label: "Submit",
    severity: "danger",
    text: true,
  },
};
