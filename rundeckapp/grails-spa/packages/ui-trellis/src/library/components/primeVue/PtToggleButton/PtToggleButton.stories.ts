import type { Meta, StoryObj } from "@storybook/vue3";

import PtToggleButton from "./PtToggleButton.vue";

const meta: Meta<typeof PtToggleButton> = {
  title: "PtToggleButton",
  component: PtToggleButton,
  parameters: {
    componentSubtitle: "A wrapper component for PrimeVue ToggleButton with custom styling",
    actions: {
      disable: true,
    },
    controls: {
      disable: true,
    },
  },
  argTypes: {
    modelValue: {
      control: {
        type: "boolean",
      },
      type: "boolean",
      description: "The boolean value bound to the toggle button (v-model).",
    },
    onLabel: {
      control: {
        type: "text",
      },
      type: "string",
      description: "Label to display when the toggle button is in the 'on' state.",
    },
    offLabel: {
      control: {
        type: "text",
      },
      type: "string",
      description: "Label to display when the toggle button is in the 'off' state.",
    },
    onIcon: {
      control: {
        type: "text",
      },
      type: "string",
      description: "Icon class to display when the toggle button is in the 'on' state.",
    },
    offIcon: {
      control: {
        type: "text",
      },
      type: "string",
      description: "Icon class to display when the toggle button is in the 'off' state.",
    },
    size: {
      options: [undefined, "small", "large"],
      control: {
        type: "select",
      },
      type: "string",
      description: "Size of the toggle button. Options: 'small', 'large', or undefined for default.",
    },
    fluid: {
      control: {
        type: "boolean",
      },
      type: "boolean",
      description: "When true, the toggle button takes up the full width of its container.",
    },
    disabled: {
      control: {
        type: "boolean",
      },
      type: "boolean",
      description: "When true, the toggle button cannot be interacted with.",
    },
    invalid: {
      control: {
        type: "boolean",
      },
      type: "boolean",
      description: "When true, displays the toggle button in an invalid state.",
    },
    ariaLabel: {
      control: {
        type: "text",
      },
      type: "string",
      description: "ARIA label for accessibility. Use this or ariaLabelledby.",
    },
    ariaLabelledby: {
      control: {
        type: "text",
      },
      type: "string",
      description: "ID of an element that labels the toggle button. Use this or ariaLabel.",
    },
  },
  args: {
    modelValue: false,
    onLabel: "On",
    offLabel: "Off",
    onIcon: undefined,
    offIcon: undefined,
    size: undefined,
    fluid: false,
    disabled: false,
    invalid: false,
    ariaLabel: undefined,
    ariaLabelledby: undefined,
  },
};

export default meta;

type Story = StoryObj<typeof PtToggleButton>;

export const Playground: Story = {
  name: "Playground",
  tags: ["!dev"],
  render: (args) => ({
    components: { PtToggleButton },
    setup: () => ({ args }),
    template: `<PtToggleButton v-bind="args" />`,
  }),
};

const generateTemplate = (args: Record<string, any>) => {
  return `<div>
    <PtToggleButton v-bind="args" />
  </div>`;
};

export const Default: Story = {
  render: args => ({
    props: Object.keys(args),
    components: { PtToggleButton },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {},
};
