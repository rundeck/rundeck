---
to: <%= h.path.join(`src/library/components/primeVue/${componentName}`, `${componentName}.stories.ts`) %>
unless_exists: true
---
import type { Meta, StoryObj } from "@storybook/vue3";
import "./<%= h.changeCase.lower(componentName) %>.scss";
import <%= h.changeCase.pascal(componentName) %> from "primevue/<%= h.changeCase.lower(componentName) %>";


const meta: Meta<typeof <%=componentName%>> = {
  title: "<%=componentName%>",
  component: <%=componentName%>,
  parameters: {
    docs: {
        componentSubtitle: "A brief description of the <%=componentName%> component",
    },
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
        "Severity type of the <%=componentName%>.",
    },
  },
  // TODO: Replace these args with ones appropriate for the component you are building.
  args: {
    content: "Hello world!",
  },
};

export default meta;

type Story = StoryObj<typeof <%=componentName%>>;

// TODO: manually wire the props to the component name, so that the source will update correctly in the story
export const Playground: Story = {
  name: "Playground",
  render: (args) => ({
      components: { <%=componentName%> },
      setup: () => ({ args }),
      template: <<%=componentName%>  />,
    }),
};

const generateTemplate = (args: Record<string, any>) => {
  return `<div>
    <<%=componentName%> v-bind="args" />
  </div>`;
};

export const Default: Story = {
  render: (args) => ({
    components: { <%=componentName%> },
    setup: () => ({ args }),
    template: generateTemplate(args),
  }),
};
