import type { Meta, StoryObj } from "@storybook/vue3";

import PtAutoComplete from "./PtAutoComplete.vue";

const meta: Meta<typeof PtAutoComplete> = {
  title: "AutoComplete",
  component: PtAutoComplete,
  parameters: {
    docs: {
      componentSubtitle: "A brief description of the AutoComplete component",
    },
    actions: {
      disable: true,
    },
    controls: {
      disable: true,
    },
  },
  argTypes: {
    // TODO: ArgTypes are responsible for the prop table in the Storybook
    severity: {
      options: [undefined, "success", "warning", "danger", "secondary"],
      control: {
        type: "select",
      },
      type: "string",
      description: "Severity type of the AutoComplete.",
    },
  },
  // TODO: Replace these args with ones appropriate for the component you are building.
  args: {
    content: "Hello world!",
  },
};

export default meta;

type Story = StoryObj<typeof PtAutoComplete>;

// TODO: manually wire the props to the component name, so that the source will update correctly in the story
export const Playground: Story = {
  name: "Playground",
  render: (args) => ({
    components: { PtAutoComplete },
    setup: () => ({ args }),
    template: `<PtAutoComplete></PtAutoComplete>`,
  }),
};

const generateTemplate = (args: Record<string, any>) => {
  return `<div>
    <PtAutoComplete v-bind="args" />
  </div>`;
};

export const Default: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { PtAutoComplete },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {},
};
