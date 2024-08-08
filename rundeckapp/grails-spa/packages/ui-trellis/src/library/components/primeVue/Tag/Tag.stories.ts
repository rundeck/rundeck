import type { Meta, StoryObj } from "@storybook/vue3";
import "./tag.scss";
import Tag from "primevue/tag";
import Badge from "primevue/badge";

const meta: Meta<typeof Tag> = {
  title: "Tag",
  parameters: {
    docs: {
      componentSubtitle: "Tag component is used to categorize content.",
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
        type: "text",
      },
      table: {
        defaultValue: { summary: undefined },
      },
      description: "Value to display inside the tag.",
    },
    severity: {
      options: [undefined, "success", "warning", "danger"],
      control: {
        type: "select",
      },
      table: {
        defaultValue: { summary: undefined },
      },
      type: "string",
      description: "Severity type of the tag.",
    },
    icon: {
      control: {
        type: "text",
      },
      type: "string",
      description: "class of icon to display next to the value.",
    },
  },
  args: {
    value: "Default",
    severity: "",
    icon: "",
  },
  decorators: [
    () => ({
      template:
        '<div style="margin: 1em; display: flex; gap: 10px; align-items: flex-end"><story /></div>',
    }),
  ],
};
export default meta;

type Story = StoryObj<typeof Tag>;

export const Playground: Story = {
  name: "Playground",
  render: (args) => ({
    components: { Tag },
    setup: () => ({ args }),
    template: `<Tag :severity="args.severity" :value="args.value" :icon="args.icon"  />`,
  }),
};

const generateTemplate = (severity, args) => {
  return `
      <Tag value="${args.value}" severity="${severity}" icon="${args.icon}" />
`;
};

export const Default: Story = {
  render: (args) => ({
    components: { Tag },
    setup: () => ({ args }),
    template: generateTemplate("default", args),
  }),
};

export const Success: Story = {
  render: (args) => ({
    components: { Tag },
    setup: () => ({ args }),
    template: generateTemplate("success", args),
  }),
  args: {
    value: "Success",
    severity: "success",
  },
};

export const Warning: Story = {
  render: (args) => ({
    components: { Tag },
    setup: () => ({ args }),
    template: generateTemplate("warning", args),
  }),
  args: {
    value: "Warning",
    severity: "warning",
  },
};

export const Danger: Story = {
  render: (args) => ({
    components: { Tag },
    setup: () => ({ args }),
    template: generateTemplate("danger", args),
  }),
  args: {
    value: "Danger",
    severity: "danger",
  },
};
