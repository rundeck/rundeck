import type { Meta, StoryObj } from "@storybook/vue3";

import PtInputText from "./PtInputText.vue";

const meta: Meta<typeof PtInputText> = {
  title: "PtInputText",
  component: PtInputText,
  parameters: {
    componentSubtitle: "A brief description of the PtInputText component",
    controls: { sort: "requiredFirst" },
  },
  // TODO: Replace these args with ones appropriate for the component you are building.
  args: {
    content: "Hello world!",
  },
};

export default meta;

type Story = StoryObj<typeof PtInputText>;

export const Default: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { PtInputText },
    setup() {
      return { args };
    },
    template: `<PtInputText v-bind="args" />`,
  }),
  args: {},
};
