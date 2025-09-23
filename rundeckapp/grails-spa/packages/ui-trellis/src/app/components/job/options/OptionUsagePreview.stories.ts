import { Story } from "@storybook/blocks";
import type { Meta, StoryObj } from "@storybook/vue3";
import OptionUsagePreview from "./OptionUsagePreview.vue";
const meta: Meta<typeof OptionUsagePreview> = {
  title: "App/JobEdit/Options/OptionUsagePreview",
  component: OptionUsagePreview,
  parameters: {
    componentSubtitle: "Usage preview of an Option.",
    actions: {
      disable: true,
    },
    controls: {
      disable: true,
    },
    mockData: [],
  }, // TODO: Replace these args with ones appropriate for the component you are building.
  argTypes: {
    option: {
      control: {
        type: "object",
      },
      description: "Option object",
    },
    validationErrors: {
      control: {
        type: "object",
      },
      description: "Validation Errors object",
    },
  },
  args: {
    option: {
      name: "testoption",
      regex: "asdf",
      description: "bob's your uncle",
    },
    validationErrors: {},
  },
  async beforeEach() {},
};

export default meta;

type Story = StoryObj<typeof OptionUsagePreview>;

// TODO: manually wire the props to the component name, so that the source will update correctly in the story
export const Playground: Story = {
  name: "Playground",
  tags: ["!dev"],
  render: (args) => ({
    components: { OptionUsagePreview },
    setup: () => ({ args }),
    template: `<OptionUsagePreview v-bind="args"></OptionUsagePreview>`,
  }),
};

const generateTemplate = (args: Record<string, any>) => {
  return `<div>
    <OptionUsagePreview v-bind="args" />
  </div>`;
};

export const Default: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { OptionUsagePreview },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {},
  parameters: {},
};
export const Secure: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { OptionUsagePreview },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    option: {
      name: "secureoption",
      secure: true,
    },
  },
  parameters: {},
};

export const PlaintextWithPasswordInput: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { OptionUsagePreview },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    option: {
      name: "exposedsecureoption",
      secure: true,
      valueExposed: true,
    },
  },
  parameters: {},
};

export const TypeFile: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { OptionUsagePreview },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    option: {
      name: "fileoption",
      type: "file",
      description: "this is a file option",
    },
  },
  parameters: {},
};
export const TypeMultiline: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { OptionUsagePreview },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    option: {
      name: "multilineoption",
      type: "multiline",
      description: "this is a multiline option",
    },
  },
  parameters: {},
};
