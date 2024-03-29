import type { Meta, StoryObj } from "@storybook/vue3";
import "./badge.scss";
import Badge from "primevue/badge";

const meta: Meta<typeof Badge> = {
  title: "Badge",
  component: Badge,
  parameters: {
    componentSubtitle: "Badge is a small status indicator for another element.",
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
        type: "text",
      },
      description: "Value to display on the badge",
    },
    severity: {
      options: [undefined, "success", "warning", "danger", "secondary"],
      control: {
        type: "select",
      },
      type: "string",
      description:
        "Severity type of the badge. Undefined will render the default badge.",
    },
    size: {
      options: [undefined, "large", "xlarge"],
      control: {
        type: "select",
      },
      type: "string",
      description:
        "Size of the badge, undefined will render the default size, which is the smallest.",
    },
  },
  args: {
    value: "2",
    severity: undefined,
    size: undefined,
  },
  decorators: [
    () => ({
      template:
        '<div style="margin: 1em; display: flex; gap: 10px; align-items: flex-end"><story /></div>',
    }),
  ],
  // tags: ["autodocs"],
};
export default meta;

type Story = StoryObj<typeof Badge>;

export const Playground: Story = {
  name: "Playground",
};

const generateTemplate = (severity, args) => {
  return `
      <Badge value="${args.value}" severity="${severity}" size="xlarge" />
      <Badge value="${args.value}" severity="${severity}" size="large" />
      <Badge value="${args.value}" severity="${severity}" />
`;
};

export const Default: Story = {
  name: "Default - no severity defined",
  render: (args) => ({
    components: { Badge },
    setup() {
      return { args };
    },
    template: `
      <Badge value="${args.value}" size="xlarge" />
      <Badge value="${args.value}" size="large" />
      <Badge value="${args.value}" />`,
  }),
};

export const Success: Story = {
  name: "Success",
  render: (args) => ({
    components: { Badge },
    setup() {
      return { args };
    },
    template: generateTemplate("success", args),
  }),
};

export const Warning: Story = {
  name: "Warning",
  render: (args) => ({
    components: { Badge },
    setup() {
      return { args };
    },
    template: generateTemplate("warning", args),
  }),
};

export const Danger: Story = {
  name: "Danger",
  render: (args) => ({
    components: { Badge },
    setup() {
      return { args };
    },
    template: generateTemplate("danger", args),
  }),
};

export const Secondary: Story = {
  name: "Secondary",
  render: (args) => ({
    components: { Badge },

    setup() {
      return { args };
    },
    template: generateTemplate("secondary", args),
  }),
};
