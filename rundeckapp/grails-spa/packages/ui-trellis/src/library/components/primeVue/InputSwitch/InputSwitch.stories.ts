import type { Meta, StoryObj } from "@storybook/vue3";
import "./inputswitch.scss";
import InputSwitch from "primevue/inputswitch";

const meta: Meta<typeof InputSwitch> = {
  title: "InputSwitch",
  parameters: {
    docs: {
      componentSubtitle: "InputSwitch is used to select a boolean value.",
    },
    actions: {
      disable: true,
    },
    controls: {
      disable: true,
    },
  },
  argTypes: {
    disabled: {
      type: "boolean",
      description:
        "When present, it specifies that the component should be disabled.",
    },
    modelValue: {
      type: "string | boolean",
      description: "Specifies whether a inputswitch should be checked or not.",
    },
  },
  args: {
    disabled: false,
    modelValue: true,
  },
};

export default meta;

type Story = StoryObj<typeof InputSwitch>;

// TODO: manually wire the props to the component name, so that the source will update correctly in the story
export const Playground: Story = {
  name: "Playground",
  render: (args) => ({
    components: { InputSwitch },
    setup: () => ({ args }),
    template: `<InputSwitch :disabled="args.disabled" v-model="args.checked" />`,
  }),
};

const generateTemplate = (args: Record<string, any>) => {
  return `<div>
    <InputSwitch v-bind="args" />
  </div>`;
};

export const Default: Story = {
  render: (args) => ({
    components: { InputSwitch },
    setup: () => ({ args }),
    template: generateTemplate(args),
  }),
};
