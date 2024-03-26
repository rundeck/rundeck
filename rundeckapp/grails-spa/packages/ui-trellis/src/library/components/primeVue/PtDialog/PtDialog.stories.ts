import type { Meta, StoryObj } from "@storybook/vue3";

import PtDialog from "./PtDialog.vue";

const meta: Meta<typeof PtDialog> = {
  title: "PtDialog",
  component: PtDialog,
  parameters: {
    componentSubtitle: "A brief description of the PtDialog component",
    controls: { sort: "requiredFirst" },
  },
  // TODO: Replace these args with ones appropriate for the component you are building.
  args: {
    content: "Hello world!",
  },
};

export default meta;

type Story = StoryObj<typeof PtDialog>;

export const Default: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { PtDialog },
    setup() {
      return { args };
    },
    template: `<PtDialog v-bind="args" />`,
  }),
  args: {},
};
