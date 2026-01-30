import type { Meta, StoryObj } from "@storybook/vue3";

import PtInput from "./PtInput.vue";

const meta: Meta<typeof PtInput> = {
  title: "PtInput",
  component: PtInput,
  parameters: {
    componentSubtitle: "A wrapper component for PrimeVue InputText and IconField with custom styling, labels, helper text, and error states",
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
      description: "The value bound to the input (v-model).",
    },
    placeholder: {
      control: {
        type: "text",
      },
      description: "Placeholder text displayed when input is empty.",
    },
    disabled: {
      control: {
        type: "boolean",
      },
      type: "boolean",
      description: "When true, the input cannot be interacted with.",
    },
    invalid: {
      control: {
        type: "boolean",
      },
      type: "boolean",
      description: "When true, displays the input in an invalid state with error styling.",
    },
    label: {
      control: {
        type: "text",
      },
      type: "string",
      description: "Label text displayed above the input field.",
    },
    helpText: {
      control: {
        type: "text",
      },
      type: "string",
      description: "Helper text displayed below the label and above the input.",
    },
    errorText: {
      control: {
        type: "text",
      },
      type: "string",
      description: "Error message displayed below the input when invalid is true.",
    },
    leftIcon: {
      control: {
        type: "text",
      },
      type: "string",
      description: "Icon class for left icon (e.g., 'pi pi-search'). When provided, renders IconField component.",
    },
    rightIcon: {
      control: {
        type: "text",
      },
      type: "string",
      description: "Icon class for right icon (e.g., 'pi pi-times'). When provided, renders IconField component.",
    },
    inputId: {
      control: {
        type: "text",
      },
      type: "string",
      description: "ID for the input element. Used to associate label with input.",
    },
    name: {
      control: {
        type: "text",
      },
      type: "string",
      description: "Name attribute for the element, typically used in form submissions.",
    },
    type: {
      options: ["text", "password", "email", "number", "tel", "url", "search"],
      control: {
        type: "select",
      },
      type: "string",
      description: "Input type. Options: 'text', 'password', 'email', 'number', 'tel', 'url', 'search'.",
    },
    readonly: {
      control: {
        type: "boolean",
      },
      type: "boolean",
      description: "When true, the input is read-only.",
    },
    maxlength: {
      control: {
        type: "number",
      },
      type: "number",
      description: "Maximum character length allowed in the input.",
    },
    autocomplete: {
      control: {
        type: "text",
      },
      type: "string",
      description: "HTML autocomplete attribute value.",
    },
    ariaLabel: {
      control: {
        type: "text",
      },
      type: "string",
      description: "Accessibility label for the input element.",
    },
    ariaLabelledby: {
      control: {
        type: "text",
      },
      type: "string",
      description: "Identifier of the element that labels the input for accessibility.",
    },
  },
  args: {
    modelValue: "",
    placeholder: undefined,
    disabled: false,
    invalid: false,
    label: undefined,
    helpText: undefined,
    errorText: undefined,
    leftIcon: undefined,
    rightIcon: undefined,
    inputId: undefined,
    name: undefined,
    type: "text",
    readonly: false,
    maxlength: undefined,
    autocomplete: undefined,
    ariaLabel: undefined,
    ariaLabelledby: undefined,
  },
};

export default meta;

type Story = StoryObj<typeof PtInput>;

export const Playground: Story = {
  name: "Playground",
  tags: ["!dev"],
  render: (args) => ({
    components: { PtInput },
    setup: () => ({ args }),
    template: `<PtInput v-bind="args" />`,
  }),
};

const generateTemplate = (args: Record<string, any>) => {
  return `<div>
    <PtInput v-bind="args" />
  </div>`;
};

export const Default: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { PtInput },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {},
};

export const WithLabel: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { PtInput },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    label: "Title Text",
    placeholder: "Enter text...",
  },
};

export const WithHelperText: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { PtInput },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    label: "Title Text",
    helpText: "Helper Text",
    placeholder: "Enter text...",
  },
};

export const WithLeftIcon: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { PtInput },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    label: "Title Text",
    helpText: "Helper Text",
    leftIcon: "pi pi-search",
    placeholder: "Search...",
  },
};

export const WithRightIcon: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { PtInput },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    label: "Title Text",
    helpText: "Helper Text",
    rightIcon: "pi pi-times",
    placeholder: "Enter text...",
  },
};

export const WithBothIcons: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { PtInput },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    label: "Title Text",
    helpText: "Helper Text",
    leftIcon: "pi pi-search",
    rightIcon: "pi pi-times",
    placeholder: "Search...",
  },
};

export const InvalidState: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { PtInput },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    label: "Title Text",
    helpText: "Helper Text",
    invalid: true,
    errorText: "Error Text",
    placeholder: "Invalid State",
  },
};

export const DisabledState: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { PtInput },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    label: "Title Text",
    helpText: "Helper Text",
    disabled: true,
    placeholder: "Disabled State",
  },
};

export const AllStates: Story = {
  render: () => ({
    components: { PtInput },
    template: `<div style="display: flex; flex-direction: column; gap: 2rem; max-width: 400px;">
      <PtInput 
        label="Title Text"
        helpText="Helper Text"
        placeholder="Default / Placeholder Text"
      />
      <PtInput 
        label="Title Text"
        helpText="Helper Text"
        model-value="Default / Typed Text"
      />
      <PtInput 
        label="Title Text"
        helpText="Helper Text"
        left-icon="pi pi-search"
        placeholder="Hover State"
      />
      <PtInput 
        label="Title Text"
        helpText="Helper Text"
        left-icon="pi pi-search"
        placeholder="Focus State"
      />
      <PtInput 
        label="Title Text"
        helpText="Helper Text"
        left-icon="pi pi-search"
        invalid
        error-text="Error Text"
        placeholder="Invalid State"
      />
      <PtInput 
        label="Title Text"
        helpText="Helper Text"
        left-icon="pi pi-search"
        disabled
        placeholder="Disabled State"
      />
    </div>`,
  }),
};
