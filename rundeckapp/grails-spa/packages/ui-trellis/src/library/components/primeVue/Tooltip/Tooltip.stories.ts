import type { Meta, StoryObj } from "@storybook/vue3";
import "./tooltip.scss";
import "../Tag/tag.scss";

import { TooltipDirectiveBinding } from "primevue/tooltip";
import Tag from "primevue/tag";

const meta: Meta<TooltipDirectiveBinding> = {
  title: "Tooltip",
  parameters: {
    docs: {
      componentSubtitle:
        "Tooltip directive provides advisory information for a component.",
    },
    actions: {
      disable: true,
    },
    controls: {
      disable: true,
    },
  },
  argTypes: {
    value: {
      control: {
        type: "text",
      },
      type: "string",
      description:
        "Value of the tooltip. Can be a string or a TooltipOptions object.",
    },
  },
  args: {
    value: "This is an important information!",
  },
  decorators: [
    () => ({
      template:
        '<div style="margin: 1em; display: flex; gap: 10px; align-items: flex-end"><story /></div>',
    }),
  ],
};
export default meta;

type Story = StoryObj<typeof Tag>;

export const Playground: Story = {
  name: "Tooltip directive",
  render: (args) => ({
    components: { Tag },
    setup() {
      return { args };
    },
    template: `
      <Tag v-tooltip="{ value: args.value, showDelay: 0, hideDelay: 0}" value="Hover me"/>`,
  }),
};

export const Default: Story = {
  name: "Tooltip directive using string as value",
  render: (args) => ({
    components: { Tag },
    setup: () => ({ args }),
    template: `<Tag v-tooltip="'Check this out!'" value="Hover me"  />`,
  }),
  parameters: {
    docs: {
      description: {
        story:
          "It's possible to pass directly text to the v-tooltip directive.",
      },
    },
  },
};

export const Position: Story = {
  name: "Positioning the tooltip",
  render: (args) => ({
    components: { Tag },
    setup: () => ({ args }),
    template: `<Tag v-tooltip="'Check this out!'" value="right"  />
    <Tag v-tooltip.top="'Check this out!'" value="top"  />
    <Tag v-tooltip.bottom="'Check this out!'" value="bottom"  />
    <Tag v-tooltip.left="'Check this out!'" value="left"  />`,
  }),
  parameters: {
    docs: {
      description: {
        story:
          "There are four choices to position the tooltip, default value is right and alternatives are top, bottom, left.",
      },
    },
  },
};

export const Delays: Story = {
  name: "Delaying showing/hiding tooltip",
  render: (args) => ({
    components: { Tag },
    setup: () => ({ args }),
    template: `
      <Tag v-tooltip="{ value: 'Tooltip with 500ms delay', showDelay: 500, hideDelay: 500}" value="Hover me"/>`,
  }),
  parameters: {
    docs: {
      description: {
        story:
          "Delays to the enter and leave events are defined with `showDelay` and `hideDelay` options respectively.",
      },
    },
  },
};
