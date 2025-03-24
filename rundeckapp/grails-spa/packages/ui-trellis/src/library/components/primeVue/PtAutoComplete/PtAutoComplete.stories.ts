import type { Meta, StoryObj } from "@storybook/vue3";

import PtAutoComplete from "./PtAutoComplete.vue";

const meta: Meta<typeof PtAutoComplete> = {
  title: "AutoComplete",
  component: PtAutoComplete,
  parameters: {
    componentSubtitle: "A brief description of the AutoComplete component",
    actions: {
      disable: true,
    },
    controls: {
      disable: true,
    },
  },
  argTypes: {
    // TODO: ArgTypes are responsible for the prop table in the Storybook
  },
  // TODO: Replace these args with ones appropriate for the component you are building.
  args: {
    value: "Hello world!",
  },
};

export default meta;

type Story = StoryObj<typeof PtAutoComplete>;

// TODO: manually wire the props to the component name, so that the source will update correctly in the story
export const Playground: Story = {
  name: "Playground",
  tags: ["!dev"],
  render: (args) => ({
    components: { PtAutoComplete },
    setup: () => ({ args }),
    template: `<PtAutoComplete v-model="args.value"></PtAutoComplete>`,
  }),
};

const generateTemplate = (args: Record<string, any>) => {
  return `<div>
    <PtAutoComplete v-model="args.value" :placeholder="args.placeholder" :suggestions="args.suggestions" />
  </div>`;
};

export const Default: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { PtAutoComplete },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    placeholder: "autocomplete available",
    suggestions: [
      "$id",
      "$execid",
      "$executionType",
      "$name",
      "$group",
      "$username",
      "$project",
      "$loglevel",
      "$user.email",
      "$retryAttempt",
      "$retryInitialExecId",
      "$wasRetry",
      "$threadcount",
      "$filter",
      "${job.id}",
      "$ENV_VAR"
    ]
  },
};
