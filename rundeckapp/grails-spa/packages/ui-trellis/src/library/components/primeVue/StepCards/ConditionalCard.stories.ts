import type { Meta, StoryObj } from "@storybook/vue3";
import ConditionalCard from "./ConditionalCard.vue";
import StepCard from "./StepCard.vue";
import mitt from "mitt";
import { RootStore } from "../../../stores/RootStore";
import { RundeckToken } from "../../../interfaces/rundeckWindow";
import CommonUndoRedoDraggableList from "@/app/components/common/CommonUndoRedoDraggableList.vue";
import "./stories.scss"
import { Btn, Dropdown, Modal, Notification } from "uiv";

const eventBus = mitt();

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
      description: "Severity type of the ConditionalCard.",
    },
  },
  // TODO: Replace these args with ones appropriate for the component you are building.
  args: {
    content: "Hello world!",
  },
  async beforeEach() {
    const rootStore = new RootStore(window._rundeck.rundeckClient);

    window._rundeck = {
      rdBase: "http://localhost:4440",
      apiVersion: "52",
      projectName: "Conditional steps POC",
      appMeta: {},
      token: {} as RundeckToken,
      tokens: {},
      navbar: { items: [] as any },
      feature: {},
      data: {},
      rootStore: rootStore,
      eventBus: eventBus,
    };
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
    template: `<ConditionalCard></ConditionalCard>`,
  }),
};

const generateTemplate = (args: Record<string, any>) => {
  return `<div>
    <ConditionalCard v-bind="args" />
  </div>`;
};

export const Default: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { ConditionalCard },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {},
  parameters: {
    mockData: [
      {
        url: "api/52/plugin/detail/WorkflowStep/jira-create-issue?project=:project",
        method: "GET",
        status: 200,
        response: {
          title: "Jira / Issue / Create",
          targetHostCompatibility: "all",
          ver: null,
          id: "05c4d9ea9261",
          providerMetadata: {},
          dynamicProps: null,
          thirdPartyDependencies: null,
          name: "jira-create-issue",
          desc: null,
          iconUrl: "/plugin/icon/WorkflowStep/jira-create-issue",
          projectMapping: {
            project: "project.jira.default_project",
          },
          vueConfigComponent: null,
          props: [
            {
              allowed: null,
              defaultValue: null,
              desc: "Project",
              name: "project",
              options: {
                displayType: "SINGLE_LINE",
                groupName: "Issue Details",
              },
              required: true,
              scope: "Unspecified",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "project",
              type: "String",
            },
            {
              allowed: [],
              defaultValue: null,
              desc: "Issue type",
              name: "type",
              options: {
                displayType: "SINGLE_LINE",
                groupName: "Issue Details",
              },
              required: true,
              scope: "Unspecified",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "type",
              type: "FreeSelect",
            },
            {
              allowed: null,
              defaultValue: null,
              desc: "Issue summary",
              name: "summary",
              options: {
                displayType: "SINGLE_LINE",
                groupName: "Issue Details",
              },
              required: true,
              scope: "Unspecified",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "summary",
              type: "String",
            },
            {
              allowed: null,
              defaultValue: null,
              desc: "Issue description",
              name: "description",
              options: {
                displayType: "SINGLE_LINE",
                groupName: "Issue Details",
              },
              required: false,
              scope: "Unspecified",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "description",
              type: "String",
            },
            {
              allowed: null,
              defaultValue: null,
              desc: "Issue assignee",
              name: "assignee",
              options: {
                displayType: "SINGLE_LINE",
                groupName: "Issue Details",
              },
              required: false,
              scope: "Unspecified",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "assignee",
              type: "String",
            },
            {
              allowed: null,
              defaultValue: null,
              desc: "Here you can create an entry box to add data to a custom field that exists on your Jira project. The label must match exactly with the name of the custom field on your project you wish to populate, and the description is the text that appears underneath the entry box in this job step. The key is unused in the call, it is just used as a unique identifier for each entry box you create.",
              name: "customFieldsUserInput",
              options: {
                displayType: "DYNAMIC_FORM",
                groupName: "Issue Details",
              },
              required: false,
              scope: "Instance",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "Custom Fields",
              type: "String",
            },
            {
              allowed: null,
              defaultValue: null,
              desc: "Jira server URL. Leave blank if it is set at project level or framework level (jira.url)",
              name: "url",
              options: {
                displayType: "SINGLE_LINE",
                groupName: "Authentication",
              },
              required: false,
              scope: "Instance",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "Server URL",
              type: "String",
            },
            {
              allowed: null,
              defaultValue: null,
              desc: "The account login name. Leave blank if it is set at project level or framework level (jira.login)",
              name: "login",
              options: {
                displayType: "SINGLE_LINE",
                groupName: "Authentication",
              },
              required: false,
              scope: "Instance",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "login",
              type: "String",
            },
            {
              allowed: null,
              defaultValue: null,
              desc: "The account password. Must be a keystorage path. Leave blank if it is set at project level or framework level (jira.password-key-storage-path)",
              name: "password",
              options: {
                displayType: "SINGLE_LINE",
                groupName: "Authentication",
                "storage-path-root": "keys",
                "storage-file-meta-filter": "Rundeck-data-type=password",
                selectionAccessor: "STORAGE_PATH",
              },
              required: false,
              scope: "Instance",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "password",
              type: "String",
            },
            {
              allowed: null,
              defaultValue: null,
              desc: "Jira auth token. Leave blank if it is set at project level  or framework level (jira.auth_token)",
              name: "auth_token",
              options: {
                displayType: "SINGLE_LINE",
                groupName: "Authentication",
                "storage-path-root": "keys",
                "storage-file-meta-filter": "Rundeck-data-type=password",
                selectionAccessor: "STORAGE_PATH",
              },
              required: false,
              scope: "Instance",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "Auth Token",
              type: "String",
            },
          ],
          rundeckCompatibilityVersion: "unspecified",
          license: "unspecified",
          pluginVersion: "5.15.0-SNAPSHOT",
          description: "Creates a Jira Issue",
          sourceLink: null,
          fwkMapping: {
            "password-key-storage-path": "jira.password-key-storage-path",
            project: "jira.default_project",
            login: "jira.login",
            auth_token: "jira.auth_token",
            url: "jira.url",
          },
          dynamicDefaults: null,
        },
      },
    ],
  },
};

export const DragNDrop: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { ConditionalCard, CommonUndoRedoDraggableList, Btn, StepCard },
    setup() {
      return { args };
    },
      data() {
        return {
            list: [{}, {}]
        }
      },
    template: `<div>
          <common-undo-redo-draggable-list
              ref="historyControls"
              v-model="list"
              revert-all-enabled
              draggable-tag="ol"
              item-key="id"
          >
            <template #item="{ item: { element, index } }">
              <li>
                <i class="glyphicon glyphicon-menu-hamburger dragHandle"></i>
                <conditional-card v-if="index === 0" />
                <step-card v-else />
              </li>
            </template>
          </common-undo-redo-draggable-list>
        </div>`,
  }),
  args: {},
  parameters: {
    mockData: [
      {
        url: "api/52/plugin/detail/WorkflowStep/jira-create-issue?project=:project",
        method: "GET",
        status: 200,
        response: {
          title: "Jira / Issue / Create",
          targetHostCompatibility: "all",
          ver: null,
          id: "05c4d9ea9261",
          providerMetadata: {},
          dynamicProps: null,
          thirdPartyDependencies: null,
          name: "jira-create-issue",
          desc: null,
          iconUrl: "/plugin/icon/WorkflowStep/jira-create-issue",
          projectMapping: {
            project: "project.jira.default_project",
          },
          vueConfigComponent: null,
          props: [
            {
              allowed: null,
              defaultValue: null,
              desc: "Project",
              name: "project",
              options: {
                displayType: "SINGLE_LINE",
                groupName: "Issue Details",
              },
              required: true,
              scope: "Unspecified",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "project",
              type: "String",
            },
            {
              allowed: [],
              defaultValue: null,
              desc: "Issue type",
              name: "type",
              options: {
                displayType: "SINGLE_LINE",
                groupName: "Issue Details",
              },
              required: true,
              scope: "Unspecified",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "type",
              type: "FreeSelect",
            },
            {
              allowed: null,
              defaultValue: null,
              desc: "Issue summary",
              name: "summary",
              options: {
                displayType: "SINGLE_LINE",
                groupName: "Issue Details",
              },
              required: true,
              scope: "Unspecified",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "summary",
              type: "String",
            },
            {
              allowed: null,
              defaultValue: null,
              desc: "Issue description",
              name: "description",
              options: {
                displayType: "SINGLE_LINE",
                groupName: "Issue Details",
              },
              required: false,
              scope: "Unspecified",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "description",
              type: "String",
            },
            {
              allowed: null,
              defaultValue: null,
              desc: "Issue assignee",
              name: "assignee",
              options: {
                displayType: "SINGLE_LINE",
                groupName: "Issue Details",
              },
              required: false,
              scope: "Unspecified",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "assignee",
              type: "String",
            },
            {
              allowed: null,
              defaultValue: null,
              desc: "Here you can create an entry box to add data to a custom field that exists on your Jira project. The label must match exactly with the name of the custom field on your project you wish to populate, and the description is the text that appears underneath the entry box in this job step. The key is unused in the call, it is just used as a unique identifier for each entry box you create.",
              name: "customFieldsUserInput",
              options: {
                displayType: "DYNAMIC_FORM",
                groupName: "Issue Details",
              },
              required: false,
              scope: "Instance",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "Custom Fields",
              type: "String",
            },
            {
              allowed: null,
              defaultValue: null,
              desc: "Jira server URL. Leave blank if it is set at project level or framework level (jira.url)",
              name: "url",
              options: {
                displayType: "SINGLE_LINE",
                groupName: "Authentication",
              },
              required: false,
              scope: "Instance",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "Server URL",
              type: "String",
            },
            {
              allowed: null,
              defaultValue: null,
              desc: "The account login name. Leave blank if it is set at project level or framework level (jira.login)",
              name: "login",
              options: {
                displayType: "SINGLE_LINE",
                groupName: "Authentication",
              },
              required: false,
              scope: "Instance",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "login",
              type: "String",
            },
            {
              allowed: null,
              defaultValue: null,
              desc: "The account password. Must be a keystorage path. Leave blank if it is set at project level or framework level (jira.password-key-storage-path)",
              name: "password",
              options: {
                displayType: "SINGLE_LINE",
                groupName: "Authentication",
                "storage-path-root": "keys",
                "storage-file-meta-filter": "Rundeck-data-type=password",
                selectionAccessor: "STORAGE_PATH",
              },
              required: false,
              scope: "Instance",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "password",
              type: "String",
            },
            {
              allowed: null,
              defaultValue: null,
              desc: "Jira auth token. Leave blank if it is set at project level  or framework level (jira.auth_token)",
              name: "auth_token",
              options: {
                displayType: "SINGLE_LINE",
                groupName: "Authentication",
                "storage-path-root": "keys",
                "storage-file-meta-filter": "Rundeck-data-type=password",
                selectionAccessor: "STORAGE_PATH",
              },
              required: false,
              scope: "Instance",
              selectLabels: null,
              staticTextDefaultValue: "",
              title: "Auth Token",
              type: "String",
            },
          ],
          rundeckCompatibilityVersion: "unspecified",
          license: "unspecified",
          pluginVersion: "5.15.0-SNAPSHOT",
          description: "Creates a Jira Issue",
          sourceLink: null,
          fwkMapping: {
            "password-key-storage-path": "jira.password-key-storage-path",
            project: "jira.default_project",
            login: "jira.login",
            auth_token: "jira.auth_token",
            url: "jira.url",
          },
          dynamicDefaults: null,
        },
      },
    ],
  },
};
