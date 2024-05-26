import type { Meta, StoryObj } from "@storybook/vue3";
import PtSteps from "./PtSteps.vue";

const meta: Meta<typeof PtSteps> = {
  title: "PtSteps",
  component: PtSteps,
  parameters: {
    docs: {
      componentSubtitle:
        "PtSteps is a wrapper for the component Steps, also known as Stepper, which is an indicator for the steps in a workflow.",
    },
    actions: {
      disable: true,
    },
    controls: {
      disable: true,
    },
  },
  argTypes: {
    activeStep: {
      control: {
        type: "number",
      },
      type: "number",
      description: "Active step index of menuitem.",
    },
    items: {
      description: "An array of menuitems.",
    },
  },
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

export default meta;

type Story = StoryObj<typeof PtSteps>;

export const Playground: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { PtSteps },
    setup: () => ({ args }),
    template: `<PtSteps :activeStep="args.activeStep" :items="args.items" />`,
  }),
  args: {},
};

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

export const Completed: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { PtSteps },
    setup() {
      return { args };
    },
    template: `<PtSteps  :activeStep="args.activeStep" :items="args.items"  />`,
  }),
  args: {
    items: [
      {
        label: "Personal Info",
        completed: true,
      },
      {
        label: "Reservation",
        completed: true,
      },
      {
        label: "Review",
        completed: false,
      },
    ],
  },
  parameters: {
    docs: {
      description: {
        story:
          "It's also possible to mark a step as completed by passing the 'completed' key inside the items object. This is useful when users are allowed to go back to previous steps to do changes, without losing the state of more advanced steps.",
      },
    },
  },
};
