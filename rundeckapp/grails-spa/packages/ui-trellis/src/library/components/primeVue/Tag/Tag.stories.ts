import type { Meta, StoryObj } from "@storybook/vue3";
import "./tag.scss";
import Tag from "primevue/tag";

const meta: Meta<typeof Tag> = {
  component: Tag,
  argTypes: {
    value: {
      control: {
        type: "text",
      },
    },
    severity: {
      control: {
        type: "select",
        options: ["sucess", "warning", "danger"],
      },
    },
    icon: {
      control: {
        type: "text",
        description: "class of icon to display",
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

type Story = StoryObj<typeof Tag>;

export const Default: Story = {
  render: (args) => ({
    components: { Tag },
    name: "Default tag",
    setup() {
      return { args };
    },
    template: `
      <tag v-bind="args"/>`,
  }),
  args: {
    value: "Default",
    severity: "",
    icon: "",
  },
};

export const Success: Story = {
  render: (args) => ({
    components: { Tag },
    name: "Success tag",
    setup() {
      return { args };
    },
    template: `
      <tag v-bind="args"/>
`,
  }),
  args: {
    value: "Success",
    severity: "success",
  },
};

export const Warning: Story = {
  render: (args) => ({
    components: { Tag },
    name: "Warning tag",
    setup() {
      return { args };
    },
    template: `
      <tag v-bind="args"/>
    `,
  }),
  args: {
    value: "Warning",
    severity: "warning",
  },
};

export const Danger: Story = {
  render: (args) => ({
    components: { Tag },
    name: "Danger tag",
    setup() {
      return { args };
    },
    template: `
      <tag v-bind="args"/>
    `,
  }),
  args: {
    value: "Danger",
    severity: "danger",
  },
};
