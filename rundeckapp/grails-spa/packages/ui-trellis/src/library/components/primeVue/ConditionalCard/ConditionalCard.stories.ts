import type { Meta, StoryObj } from "@storybook/vue3";

import ConditionalCard from "./ConditionalCard.vue";

const meta: Meta<typeof ConditionalCard> = {
  title: "ConditionalCard",
  component: ConditionalCard,
  parameters: {
    componentSubtitle: "A brief description of the ConditionalCard component",
    actions: {
      disable: true,
    },
    controls: {
      disable: true,
    },
  },
  argTypes: {
      // TODO: ArgTypes are responsible for the prop table in the Storybook
      severity: {
        options: [undefined, "success", "warning", "danger", "secondary"],
        control: {
          type: "select",
        },
        type: "string",
        description:
          "Severity type of the ConditionalCard.",
      },
    },
    // TODO: Replace these args with ones appropriate for the component you are building.
    args: {
      content: "Hello world!",
    },
};

export default meta;

type Story = StoryObj<typeof ConditionalCard>;

// TODO: manually wire the props to the component name, so that the source will update correctly in the story
export const Playground: Story = {
  name: "Playground",
  tags: ["!dev"],
  render: (args) => ({
        components: { ConditionalCard },
        setup: () => ({ args }),
        template:  `<ConditionalCard></ConditionalCard>`,
      }),
};

const generateTemplate = (args: Record<string, any>) => {
  return `<div>
    <ConditionalCard v-bind="args" />
  </div>`;
};

export const Default: Story = {
  render: args => ({
    props: Object.keys(args),
    components: { ConditionalCard },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {},
};
