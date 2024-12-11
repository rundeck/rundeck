import type { Meta, StoryObj } from "@storybook/vue3";
import PtStepper from "././PtStepper.vue";

const meta: Meta<typeof PtStepper> = {
  title: "Stepper",
  component: PtStepper,
  parameters: {
    componentSubtitle:
      "PtStepper is a wrapper for the component Stepper, that displays a wizard-like workflow by guiding users through the multi-step progression.",
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
      description: "Index of active step.",
    },
    items: {
      description:
        "Array of steps to render. By default each item must have a label",
    },
  },
  args: {
    activeStep: 1,
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

type Story = StoryObj<typeof PtStepper>;

export const Playground: Story = {
  name: "Playground",
  tags: ["!dev"],
  render: (args) => ({
    props: Object.keys(args),
    components: { PtStepper },
    setup: () => ({ args }),
    template: `<PtStepper :activeStep="args.activeStep" :items="args.items" />`,
  }),
  args: {},
};

export const Default: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { PtStepper },
    setup() {
      return { args };
    },
    template: `<PtStepper  :activeStep="args.activeStep" :items="args.items"  />`,
  }),
  args: {
    activeStep: 1,
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
    components: { PtStepper },
    setup() {
      return { args };
    },
    template: `<PtStepper :activeStep="args.activeStep" :items="args.items"  />`,
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
    activeStep: 2,
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
