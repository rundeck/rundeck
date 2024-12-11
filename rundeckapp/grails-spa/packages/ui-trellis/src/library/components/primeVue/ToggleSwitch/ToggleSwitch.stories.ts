import type { Meta, StoryObj } from "@storybook/vue3";
import "./toggleSwitch.scss";
import ToggleSwitch from "primevue/toggleswitch";

const meta: Meta<typeof ToggleSwitch> = {
  title: "ToggleSwitch",
  parameters: {
    componentSubtitle: "ToggleSwitch is used to select a boolean value.",
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
      description: "Specifies whether a ToggleSwitch should be checked or not.",
    },
  },
  args: {
    disabled: false,
    modelValue: true,
  },
};

export default meta;

type Story = StoryObj<typeof ToggleSwitch>;

// TODO: manually wire the props to the component name, so that the source will update correctly in the story
export const Playground: Story = {
  name: "Playground",
  tags: ["!dev"],
  render: (args) => ({
    components: { ToggleSwitch },
    setup: () => ({ args }),
    template: `<ToggleSwitch :disabled="args.disabled" v-model="args.checked" />`,
  }),
};

const generateTemplate = (args: Record<string, any>) => {
  return '<div><ToggleSwitch :disabled="args.disabled" v-model="args.modelValue" /></div>';
};

export const Active: Story = {
  render: (args) => ({
    components: { ToggleSwitch },
    setup: () => ({ args }),
    template: generateTemplate(args),
  }),
};

export const Disabled: Story = {
  render: (args) => ({
    components: { ToggleSwitch },
    setup: () => ({ args }),
    template:
      '<div style="gap: 10px; display: flex;"><ToggleSwitch :disabled="args.disabled" v-model="args.modelValue" /><ToggleSwitch :disabled="args.disabled" /></div>',
  }),
  args: {
    disabled: true,
    modelValue: true,
  },
};
