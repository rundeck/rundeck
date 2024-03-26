import type { Meta, StoryObj } from "@storybook/vue3";
import PtButton from "./PtButton.vue";

const meta: Meta<typeof PtButton> = {
  component: PtButton,
  argTypes: {
    value: {
      control: {
        type: "text",
      },
    },
    severity: {
      control: {
        type: "select",
        options: ["sucess", "warning", "danger", "secondary"],
      },
    },
    size: {
      control: {
        type: "select",
        options: ["large", "xlarge"],
      },
    },
  },
  decorators: [
    () => ({
      template:
        '<div style="margin: 1em; display: flex; gap: 10px; align-items: flex-end"><story /></div>',
    }),
  ],
  tags: ["autodocs"],
};
export default meta;

type Story = StoryObj<typeof PtButton>;

export const Default: Story = {
  render: (args) => ({
    components: { PtButton },
    name: "Default badge",
    setup() {
      return { args };
    },
    template: `
      <PtButton v-bind="args" />`,
  }),
  args: {
    label: "Submit",
  },
};
