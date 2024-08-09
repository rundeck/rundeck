import type { Meta, StoryObj } from "@storybook/vue3";
import "./progressSpinner.scss";
import ProgressSpinner from "primevue/progressspinner";

const meta: Meta<typeof ProgressSpinner> = {
  title: "Misc/ProgressSpinner",
  component: ProgressSpinner,
  parameters: {
    docs: {
      componentSubtitle: "ProgressSpinner is a process status indicator.",
    },
    actions: {
      disable: true,
    },
    controls: {
      disable: true,
    },
  },
  argTypes: {
    severity: {
      options: [undefined, "success", "warning", "danger"],
      control: {
        type: "select",
      },
      type: "string",
      description: "Severity type of the ProgressSpinner.",
    },
  },
  args: {
    severity: "success",
  },
};

export default meta;

type Story = StoryObj<typeof ProgressSpinner>;

export const Playground: Story = {
  name: "Playground",
  render: (args) => ({
    components: { ProgressSpinner },
    setup: () => ({ args }),
    template: `<ProgressSpinner :class="args.severity"  />`,
  }),
};

const generateTemplate = ({ severity }) => {
  return `
    <ProgressSpinner class="${severity}" />
  `;
};

export const Default: Story = {
  render: (args) => ({
    components: { ProgressSpinner },
    setup: () => ({ args }),
    template: `<ProgressSpinner />`,
  }),
};

export const Success: Story = {
  render: (args) => ({
    components: { ProgressSpinner },
    setup: () => ({ args }),
    template: generateTemplate(args),
  }),
  args: {
    severity: "success",
  },
};

export const Warning: Story = {
  render: (args) => ({
    components: { ProgressSpinner },
    setup: () => ({ args }),
    template: generateTemplate(args),
  }),
  args: {
    severity: "warning",
  },
};

export const Danger: Story = {
  render: (args) => ({
    components: { ProgressSpinner },
    setup: () => ({ args }),
    template: generateTemplate(args),
  }),
  args: {
    severity: "danger",
  },
};
