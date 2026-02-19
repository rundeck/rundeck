import type { Meta, StoryObj } from "@storybook/vue3";

import PtSelectButton from "./PtSelectButton.vue";

const meta: Meta<typeof PtSelectButton> = {
  title: "PtSelectButton",
  component: PtSelectButton,
  parameters: {
    componentSubtitle: "A wrapper component for PrimeVue SelectButton with custom styling",
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
        type: "text",
      },
      description: "The value bound to the select button (v-model).",
    },
    options: {
      control: {
        type: "object",
      },
      description: "An array of options to display. Can be simple values or objects.",
    },
    optionLabel: {
      control: {
        type: "text",
      },
      type: "string",
      description: "Property name or getter function to use as the label of an option.",
    },
    optionValue: {
      control: {
        type: "text",
      },
      type: "string",
      description: "Property name or getter function to use as the value of an option.",
    },
    optionDisabled: {
      control: {
        type: "text",
      },
      type: "string",
      description: "Property name or getter function to use as the disabled flag of an option.",
    },
    multiple: {
      control: {
        type: "boolean",
      },
      type: "boolean",
      description: "When true, allows selecting multiple values.",
    },
    fluid: {
      control: {
        type: "boolean",
      },
      type: "boolean",
      description: "When true, the select button takes up the full width of its container.",
    },
    disabled: {
      control: {
        type: "boolean",
      },
      type: "boolean",
      description: "When true, the select button cannot be interacted with.",
    },
    invalid: {
      control: {
        type: "boolean",
      },
      type: "boolean",
      description: "When true, displays the select button in an invalid state.",
    },
    dataKey: {
      control: {
        type: "text",
      },
      type: "string",
      description: "A property to uniquely identify an option.",
    },
    allowEmpty: {
      control: {
        type: "boolean",
      },
      type: "boolean",
      description: "Whether selection can be cleared.",
    },
    name: {
      control: {
        type: "text",
      },
      type: "string",
      description: "Name attribute for the element, typically used in form submissions.",
    },
    ariaLabelledby: {
      control: {
        type: "text",
      },
      type: "string",
      description: "Identifier of the underlying element for accessibility.",
    },
  },
  args: {
    modelValue: "option1",
    options: ["Option 1", "Option 2", "Option 3"],
    optionLabel: undefined,
    optionValue: undefined,
    optionDisabled: undefined,
    multiple: false,
    fluid: false,
    disabled: false,
    invalid: false,
    dataKey: undefined,
    allowEmpty: true,
    name: undefined,
    ariaLabelledby: undefined,
  },
};

export default meta;

type Story = StoryObj<typeof PtSelectButton>;

export const Playground: Story = {
  name: "Playground",
  tags: ["!dev"],
  render: (args) => ({
    components: { PtSelectButton },
    setup: () => ({ args }),
    template: `<PtSelectButton v-bind="args" />`,
  }),
};

const generateTemplate = (args: Record<string, any>) => {
  return `<div>
    <PtSelectButton v-bind="args" />
  </div>`;
};

export const Default: Story = {
  render: args => ({
    props: Object.keys(args),
    components: { PtSelectButton },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {},
};

export const WithObjects: Story = {
  render: args => ({
    props: Object.keys(args),
    components: { PtSelectButton },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    modelValue: 1,
    options: [
      { name: "Node Steps", value: 1 },
      { name: "Workflow Steps", value: 2 },
    ],
    optionLabel: "name",
    optionValue: "value",
  },
};

export const Multiple: Story = {
  render: args => ({
    props: Object.keys(args),
    components: { PtSelectButton },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    modelValue: ["option1"],
    options: ["Option 1", "Option 2", "Option 3"],
    multiple: true,
  },
};

export const Fluid: Story = {
  render: args => ({
    props: Object.keys(args),
    components: { PtSelectButton },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    fluid: true,
  },
};

export const PreventDeselection: Story = {
  render: args => ({
    props: Object.keys(args),
    components: { PtSelectButton },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    modelValue: "Option 1",
    options: ["Option 1", "Option 2", "Option 3"],
    allowEmpty: false,
  },
};
