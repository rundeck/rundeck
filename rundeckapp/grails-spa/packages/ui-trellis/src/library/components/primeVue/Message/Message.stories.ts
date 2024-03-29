import type { Meta, StoryObj } from "@storybook/vue3";
import "./message.scss";
import Message from "primevue/message";
import PtButton from "../PtButton/PtButton.vue";

const meta: Meta<typeof Message> = {
  title: "Message",
  component: Message,
  parameters: {
    componentSubtitle: "Message component is used to display inline messages.",
    actions: {
      disable: true,
    },
    controls: {
      disable: true,
    },
  },
  argTypes: {
    severity: {
      options: ["info", "warn", "error", "success"],
      control: {
        type: "select",
      },
      type: "string",
      description:
        "Severity type of the message. Info will render the default message.",
    },
    closable: {
      control: {
        type: "boolean",
      },
      type: "boolean",
      description:
        "Whether the message can be closed manually using the close icon. Default is true.",
    },
  },
  args: {
    severity: "info",
  },
};

export default meta;

type Story = StoryObj<typeof Message>;

const generateTemplate = (severity, args) => {
  return `<Message :closable="${args.closable}" severity="${severity}">Hello world!</Message>`;
};

export const Playground: Story = {
  name: "Playground",
  render: (args) => ({
    props: Object.keys(args),
    components: { Message },
    setup() {
      return { args };
    },
    template: generateTemplate("info", args),
  }),
};

export const Info: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { Message },
    setup() {
      return { args };
    },
    template: generateTemplate("info", args),
  }),
};

export const Success: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { Message },
    setup() {
      return { args };
    },
    template: generateTemplate("success", args),
  }),
};

export const Warning: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { Message },
    setup() {
      return { args };
    },
    template: generateTemplate("warn", args),
  }),
};

export const Error: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { Message },
    setup() {
      return { args };
    },
    template: generateTemplate("error", args),
  }),
};

export const Closable: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { Message },
    setup() {
      return { args };
    },
    template: generateTemplate("success", args),
  }),
  args: {
    closable: false,
  },
  parameters: {
    docs: {
      description: {
        story:
          "Messages are closable by default, disable closable option to remove the close button.",
      },
    },
  },
};

export const CustomContent: Story = {
  name: "Custom Content",
  render: (args) => ({
    props: Object.keys(args),
    components: { Message, PtButton },
    setup() {
      return { args };
    },
    template: `<Message :closable="${args.closable}" severity="${args.severity}">
      <p>License has expired.<span class="p-message-subtitle"> Fix it here </span></p>
      <a href="..">
        <PtButton label="contact us" :link="true" severity="danger" />
      </a>
    </Message>`,
  }),
  args: {
    severity: "error",
    closable: false,
  },
  parameters: {
    docs: {
      description: {
        story: "Message component can contain components besides text.",
      },
    },
  },
};
