import type { Meta, StoryObj } from "@storybook/vue3";
import { ref } from "vue";

import PtRadioGroup from "./PtRadioGroup.vue";

const meta: Meta<typeof PtRadioGroup> = {
  title: "PtRadioGroup",
  component: PtRadioGroup,
  parameters: {
    componentSubtitle:
      "A wrapper component for PrimeVue RadioButton that renders a labelled group",
    actions: { disable: true },
    controls: { disable: true },
  },
  argTypes: {
    modelValue: {
      control: { type: "text" },
      description: "The value of the selected option (v-model).",
    },
    options: {
      control: { type: "object" },
      description: "Options to choose from. Can be simple values or objects.",
    },
    optionLabel: {
      control: { type: "text" },
      description: "Property name or getter for an option's label.",
    },
    optionValue: {
      control: { type: "text" },
      description: "Property name or getter for an option's value.",
    },
    optionDisabled: {
      control: { type: "text" },
      description: "Property name or getter for an option's disabled flag.",
    },
    legend: {
      control: { type: "text" },
      description:
        "Text that names the group (its accessible name), shown above the options. Required.",
    },
    hideLegend: {
      control: { type: "boolean" },
      description:
        "Hides the legend visually while keeping it as the group's accessible name.",
    },
    name: {
      control: { type: "text" },
      description: "Native name shared by the radios.",
    },
    disabled: {
      control: { type: "boolean" },
      description: "Disables every option.",
    },
    invalid: {
      control: { type: "boolean" },
      description: "Marks the group as invalid.",
    },
  },
};

export default meta;
type Story = StoryObj<typeof PtRadioGroup>;

const options = [
  { label: "All nodes", value: "all" },
  { label: "Specific nodes", value: "specific" },
];

const render = (args: Record<string, unknown>) => ({
  components: { PtRadioGroup },
  setup() {
    const selected = ref("all");
    return { args, selected };
  },
  template: `<PtRadioGroup v-bind="args" v-model="selected" />`,
});

export const Playground: Story = {
  render,
  args: { options, legend: "Select Target Nodes:" },
};

export const Default: Story = {
  render,
  args: { options, legend: "Select Target Nodes:" },
};

export const HiddenLegend: Story = {
  render,
  args: { options, legend: "Select Target Nodes", hideLegend: true },
};

export const Disabled: Story = {
  render,
  args: { options, legend: "Select Target Nodes:", disabled: true },
};

export const DisabledOption: Story = {
  render,
  args: {
    options: [options[0], { ...options[1], disabled: true }],
    legend: "Select Target Nodes:",
  },
};
