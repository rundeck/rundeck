---
to: <%= h.path.join(`src/library/components/primeVue/${componentName}`, `${componentName}.stories.ts`) %>
unless_exists: true
---
import type { Meta, StoryObj } from "@storybook/vue3";

import <%=componentName%> from "./<%=componentName%>.vue";

const meta: Meta<typeof <%=componentName%>> = {
  title: "<%=componentName%>",
  component: <%=componentName%>,
  parameters: {
    componentSubtitle: "A brief description of the <%=componentName%> component",
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
          "Severity type of the <%=componentName=>.",
      },
    },
    // TODO: Replace these args with ones appropriate for the component you are building.
    args: {
      content: "Hello world!",
    },
};

export default meta;

type Story = StoryObj<typeof <%=componentName%>>;

export const Playground: Story = {
  name: "Playground",
};

const generateTemplate = (args: Record<string, any>) => {
  return `<div>
    <<%=componentName%> v-bind="args" />
  </div>`;
};

export const Default: Story = {
  render: args => ({
    props: Object.keys(args),
    components: { <%=componentName%> },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {},
};
