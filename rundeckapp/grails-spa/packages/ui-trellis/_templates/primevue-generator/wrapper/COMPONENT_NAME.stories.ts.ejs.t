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
    controls: { sort: "requiredFirst" },
  },
  // TODO: Replace these args with ones appropriate for the component you are building.
  args: {
    content: "Hello world!",
  },
};

export default meta;

type Story = StoryObj<typeof <%=componentName%>>;

export const Default: Story = {
  render: args => ({
    props: Object.keys(args),
    components: { <%=componentName%> },
    setup() {
      return { args };
    },
    template: `<<%=componentName%> v-bind="args" />`,
  }),
  args: {},
};
