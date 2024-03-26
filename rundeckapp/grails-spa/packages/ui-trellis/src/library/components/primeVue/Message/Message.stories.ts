import type { Meta, StoryObj } from "@storybook/vue3";
import "./message.scss";
import Message from "primevue/message";

const meta: Meta<typeof Message> = {
  title: "Message",
  component: Message,
  parameters: {
    componentSubtitle: "A brief description of the Message component",
    controls: { sort: "requiredFirst" },
  },
  // TODO: Replace these args with ones appropriate for the component you are building.
  args: {
    content: "Hello world!",
  },
};

export default meta;

type Story = StoryObj<typeof Message>;

export const Default: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { Message },
    setup() {
      return { args };
    },
    template: `<Message :severity="args.severity">{{ args.content }}</Message>`,
  }),
  args: {
    severity: "success",
    content: "Hello world!",
  },
};

export const Info: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { Message },
    setup() {
      return { args };
    },
    template: `<Message :severity="args.severity">{{ args.content }}</Message>`,
  }),
  args: {
    ...Default.args,
    severity: "info",
  },
};

export const Warning: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { Message },
    setup() {
      return { args };
    },
    template: `<Message :severity="args.severity">{{ args.content }}</Message>`,
  }),
  args: {
    ...Default.args,
    severity: "warn",
  },
};

export const Error: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { Message },
    setup() {
      return { args };
    },
    template: `<Message :severity="args.severity">{{ args.content }}</Message>`,
  }),
  args: {
    ...Default.args,
    severity: "error",
  },
};
