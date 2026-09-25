import type { Meta, StoryObj } from "@storybook/vue3";
import { ref } from "vue";

import PtAutoComplete from "./PtAutoComplete.vue";
import type { ContextVariable } from "../../../stores/contextVariables";
import type { TabConfig } from "./PtAutoCompleteTypes";

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
    severity: {
      options: [undefined, "success", "warning", "danger", "secondary"],
      control: {
        type: "select",
      },
      type: "string",
      description: "Severity type of the AutoComplete.",
    },
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
    <PtAutoComplete v-bind="args" />
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
  args: {},
};

// Hardcoded suggestions for testing
// These suggestions are designed to test the partial word matching logic:
// - Typing "execution" should match all execution.* variables
// - Typing "job" should match all job.* variables
// - Typing "node" should match all node.* variables
// - Typing "option" should match all option.* variables
const hardcodedSuggestions: ContextVariable[] = [
  // Job type variables
  {
    name: "job.id",
    title: "Job ID",
    description: "Unique identifier for the job",
    type: "job",
  },
  {
    name: "job.name",
    title: "Job Name",
    description: "Name of the job",
    type: "job",
  },
  {
    name: "job.group",
    title: "Job Group",
    description: "Group the job belongs to",
    type: "job",
  },
  {
    name: "job.project",
    title: "Project Name",
    description: "Project name",
    type: "job",
  },
  {
    name: "job.username",
    title: "Username",
    description: "Name of user executing the job",
    type: "job",
  },
  {
    name: "job.execid",
    title: "Execution ID",
    description: "Execution ID for the job",
    type: "job",
  },
  // Node type variables
  {
    name: "node.name",
    title: "Node Name",
    description: "Name of the node",
    type: "node",
  },
  {
    name: "node.hostname",
    title: "Node Hostname",
    description: "Hostname of the node",
    type: "node",
  },
  {
    name: "node.username",
    title: "Node Username",
    description: "Username for the node",
    type: "node",
  },
  {
    name: "node.os-name",
    title: "OS Name",
    description: "Operating system name",
    type: "node",
  },
  {
    name: "node.os-family",
    title: "OS Family",
    description: "Operating system family",
    type: "node",
  },
  // Execution type variables - these should match when typing "execution"
  {
    name: "execution.id",
    title: "Execution ID",
    description: "Unique identifier for the execution",
    type: "execution",
  },
  {
    name: "execution.status",
    title: "Execution Status",
    description: "Status of the execution",
    type: "execution",
  },
  {
    name: "execution.duration",
    title: "Execution Duration",
    description: "Duration of the execution",
    type: "execution",
  },
  {
    name: "execution.dateStarted",
    title: "Execution Start Date",
    description: "Date when the execution started",
    type: "execution",
  },
  {
    name: "execution.dateEnded",
    title: "Execution End Date",
    description: "Date when the execution ended",
    type: "execution",
  },
  {
    name: "execution.user",
    title: "Execution User",
    description: "User who started the execution",
    type: "execution",
  },
  // Option type variables
  {
    name: "option.environment",
    title: "Environment",
    description: "Environment option",
    type: "option",
  },
  {
    name: "option.region",
    title: "Region",
    description: "Region option",
    type: "option",
  },
  {
    name: "option.version",
    title: "Version",
    description: "Version option",
    type: "option",
  },
  {
    name: "option.deployment",
    title: "Deployment",
    description: "Deployment option",
    type: "option",
  },
];

// Tab configurations for testing tab mode
const tabConfigs: TabConfig[] = [
  {
    label: "Job",
    filter: (suggestion: ContextVariable) => suggestion.type === "job",
    getCount: (suggestions: ContextVariable[]) =>
      suggestions.filter((s) => s.type === "job").length,
  },
  {
    label: "Node",
    filter: (suggestion: ContextVariable) => suggestion.type === "node",
    getCount: (suggestions: ContextVariable[]) =>
      suggestions.filter((s) => s.type === "node").length,
  },
  {
    label: "Execution",
    filter: (suggestion: ContextVariable) => suggestion.type === "execution",
    getCount: (suggestions: ContextVariable[]) =>
      suggestions.filter((s) => s.type === "execution").length,
  },
  {
    label: "Option",
    filter: (suggestion: ContextVariable) => suggestion.type === "option",
    getCount: (suggestions: ContextVariable[]) =>
      suggestions.filter((s) => s.type === "option").length,
  },
];

export const WithHardcodedSuggestions: Story = {
  name: "With Hardcoded Suggestions",
  render: (args) => ({
    props: Object.keys(args),
    components: { PtAutoComplete },
    setup() {
      const value = ref("");
      return {
        args,
        value,
        suggestions: hardcodedSuggestions,
      };
    },
    template: `<div>
      <PtAutoComplete
        v-model="value"
        :suggestions="suggestions"
        placeholder="Type to search (e.g., 'job.', 'node.', 'execution.', 'option.')"
        name="test-autocomplete"
      />
    </div>`,
  }),
  args: {},
};

export const WithTabMode: Story = {
  name: "With Tab Mode",
  render: (args) => ({
    props: Object.keys(args),
    components: { PtAutoComplete },
    setup() {
      const value = ref("");
      return {
        args,
        value,
        suggestions: hardcodedSuggestions,
        tabs: tabConfigs,
      };
    },
    template: `<div>
      <PtAutoComplete
        v-model="value"
        :suggestions="suggestions"
        :tab-mode="true"
        :tabs="tabs"
        placeholder="Type to search context variables..."
        name="test-autocomplete-tabs"
      />
    </div>`,
  }),
  args: {},
};
