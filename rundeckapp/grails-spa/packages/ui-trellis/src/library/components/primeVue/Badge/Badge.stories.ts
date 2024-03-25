import type { Meta, StoryObj } from "@storybook/vue3";
import "./badge.scss";
import Badge from "primevue/badge";

const meta: Meta<typeof Badge> = {
  component: Badge,
  argTypes: {
    value: {
      control: {
        type: "text",
      },
    },
    severity: {
      control: {
        type: "select",
        options: ["sucess", "warning", "danger", "secondary"],
      },
    },
    size: {
      control: {
        type: "select",
        options: ["large", "xlarge"],
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

type Story = StoryObj<typeof Badge>;

export const Default: Story = {
  render: (args) => ({
    components: { Badge },
    name: "Default badge",
    setup() {
      return { args };
    },
    template: `
      <Badge v-bind="args" size="xlarge" />
      <Badge v-bind="args" size="large" />
      <Badge v-bind="args"/>`,
  }),
  args: {
    value: "2",
    severity: "",
    size: null,
  },
};

export const Success: Story = {
  render: (args) => ({
    components: { Badge },
    name: "Success badge",
    setup() {
      return { args };
    },
    template: `
      <Badge v-bind="args" size="xlarge" />
      <Badge v-bind="args" size="large" />
      <Badge v-bind="args"/>
`,
  }),
  args: {
    ...Default.args,
    severity: "success",
  },
};

export const Warning: Story = {
  render: (args) => ({
    components: { Badge },
    name: "Warning badge",
    setup() {
      return { args };
    },
    template: `
      <Badge v-bind="args" size="xlarge" />
      <Badge v-bind="args" size="large" />
      <Badge v-bind="args"/>
    `,
  }),
  args: {
    ...Default.args,
    severity: "warning",
  },
};

export const Danger: Story = {
  render: (args) => ({
    components: { Badge },
    name: "Danger badge",
    setup() {
      return { args };
    },
    template: `
      <Badge v-bind="args" size="xlarge" />
      <Badge v-bind="args" size="large" />
      <Badge v-bind="args"/>
    `,
  }),
  args: {
    ...Default.args,
    severity: "danger",
  },
};

export const Secondary: Story = {
  render: (args) => ({
    components: { Badge },
    name: "Secondary badge",
    setup() {
      return { args };
    },
    template: `
      <Badge v-bind="args" size="xlarge" />
      <Badge v-bind="args" size="large" />
      <Badge v-bind="args"/>
    `,
  }),
  args: {
    ...Default.args,
    severity: "secondary",
  },
};
