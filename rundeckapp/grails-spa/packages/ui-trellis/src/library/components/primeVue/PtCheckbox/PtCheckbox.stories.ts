import type { Meta, StoryObj } from "@storybook/vue3";
import { ref } from "vue";

import PtCheckbox from "./PtCheckbox.vue";

const meta: Meta<typeof PtCheckbox> = {
  title: "PtCheckbox",
  component: PtCheckbox,
  parameters: {
    componentSubtitle:
      "A Rundeck-styled wrapper for the PrimeVue binary checkbox control",
  },
  argTypes: {
    modelValue: { control: { type: "boolean" } },
    disabled: { control: { type: "boolean" } },
    readonly: { control: { type: "boolean" } },
    invalid: { control: { type: "boolean" } },
    indeterminate: { control: { type: "boolean" } },
    ariaLabel: { control: { type: "text" } },
  },
};

export default meta;
type Story = StoryObj<typeof PtCheckbox>;

const render = (args: Record<string, unknown>) => ({
  components: { PtCheckbox },
  setup() {
    const checked = ref(false);
    return { args, checked };
  },
  template: `<PtCheckbox v-bind="args" v-model="checked" />`,
});

export const Playground: Story = {
  render,
  args: { ariaLabel: "Example checkbox" },
};

export const Checked: Story = {
  render,
  args: { modelValue: true, ariaLabel: "Checked checkbox" },
};

export const Indeterminate: Story = {
  render,
  args: { indeterminate: true, ariaLabel: "Indeterminate checkbox" },
};

export const Disabled: Story = {
  render,
  args: { disabled: true, ariaLabel: "Disabled checkbox" },
};

export const Invalid: Story = {
  render,
  args: { invalid: true, ariaLabel: "Invalid checkbox" },
};
