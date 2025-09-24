import { Story } from "@storybook/blocks";
import type { Meta, StoryObj } from "@storybook/vue3";
import { setup } from "@storybook/vue3";
import OptionItem from "./OptionItem.vue";
import * as uiv from "uiv";

setup((app) => {
  app.use(uiv);
});
const meta: Meta<typeof OptionItem> = {
  title: "App/JobEdit/Options/OptionItem",
  component: OptionItem,
  parameters: {
    componentSubtitle: "An option item shown in the Job Options editor list",
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
    canMoveUp: {
      control: {
        type: "boolean",
      },
      description: "Move up or not",
    },
    canMoveDown: {
      control: {
        type: "boolean",
      },
      description: "Move down or not",
    },
  },
  args: {
    option: {
      name: "testoption",
      regex: "asdf",
      description: "bob's your uncle",
    },
    editable: true,
    canMoveUp: false,
    canMoveDown: false,
  },
  async beforeEach() {},
};

export default meta;

type Story = StoryObj<typeof OptionItem>;

// TODO: manually wire the props to the component name, so that the source will update correctly in the story
export const Playground: Story = {
  name: "Playground",
  tags: ["!dev"],
  render: (args) => ({
    components: { OptionItem },
    setup: () => ({ args }),
    template: `<OptionItem v-bind="args"></OptionItem>`,
  }),
};

const generateTemplate = (args: Record<string, any>) => {
  return `<div>
    <OptionItem v-bind="args" />
  </div>`;
};

export const Default: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { OptionItem },
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
    components: { OptionItem },
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
    components: { OptionItem },
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
