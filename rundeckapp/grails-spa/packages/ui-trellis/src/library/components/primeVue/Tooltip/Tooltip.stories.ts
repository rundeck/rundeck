import type { Meta, StoryObj } from "@storybook/vue3";
import "./tooltip.scss";
import "../Tag/tag.scss";

import Tooltip from "primevue/tooltip";
import Tag from "primevue/tag";

const meta: Meta<typeof Tooltip> = {
  component: Tooltip,
  argTypes: {
    value: {
      control: {
        type: "text",
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

type Story = StoryObj<typeof Tag>;

export const Default: Story = {
  render: (args) => ({
    components: { Tag },
    name: "Tooltip directive",
    setup() {
      return { args };
    },
    template: `
      <tag v-tooltip="{ value: 'This is a tag', showDelay: 0, hideDelay: 0}" v-bind="args"/>`,
  }),
  args: {
    value: "Default",
  },
};
