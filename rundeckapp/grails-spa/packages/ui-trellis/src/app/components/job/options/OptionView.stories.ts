import { Story } from "@storybook/blocks";
import type { Meta, StoryObj } from "@storybook/vue3";
import OptionView from "./OptionView.vue";

const meta: Meta<typeof OptionView> = {
  title: "App/JobEdit/Options/OptionView",
  component: OptionView,
  parameters: {
    componentSubtitle: "Display of an Option",
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
      description: "Name",
    },
    editable: {
      control: {
        type: "boolean",
      },
      description: "Editable or not",
    },
  },
  args: {
    option: {
      name: "testoption",
      regex: "asdf",
      description: "bob's your uncle",
    },
    editable: true,
  },
  async beforeEach() {},
};

export default meta;

type Story = StoryObj<typeof OptionView>;

// TODO: manually wire the props to the component name, so that the source will update correctly in the story
export const Playground: Story = {
  name: "Playground",
  tags: ["!dev"],
  render: (args) => ({
    components: { OptionView },
    setup: () => ({ args }),
    template: `<OptionView v-bind="args"></OptionView>`,
  }),
};

const generateTemplate = (args: Record<string, any>) => {
  return `<div>
    <OptionView v-bind="args" />
  </div>`;
};

export const Default: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { OptionView },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {},
  parameters: {},
};

export const TypeMultiLine: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { OptionView },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    option: {
      name: "testoption",
      type: "multiline",
      regex: "asdf",
      description: "bob's your uncle",
    },
  },
  parameters: {},
};

export const TypeFile: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { OptionView },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    option: {
      name: "testoption",
      type: "file",
      description: "bob's your uncle",
    },
  },
  parameters: {},
};
