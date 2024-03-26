import type { Meta, StoryObj } from "@storybook/vue3";
import PtSteps from "./PtSteps.vue";

const meta: Meta<typeof PtSteps> = {
  title: "PtSteps",
  component: PtSteps,
  parameters: {
    componentSubtitle: "A brief description of the PtSteps component",
    controls: { sort: "requiredFirst" },
  },
  // TODO: Replace these args with ones appropriate for the component you are building.
  args: {},
};

export default meta;

type Story = StoryObj<typeof PtSteps>;

export const Default: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { PtSteps },
    setup() {
      return { args };
    },
    template: `<PtSteps  :activeStep="args.activeStep" :items="args.items"  />`,
  }),
  args: {
    activeStep: 0,
    items: [
      {
        label: "Personal Info",
      },
      {
        label: "Reservation",
      },
      {
        label: "Review",
      },
    ],
  },
};
