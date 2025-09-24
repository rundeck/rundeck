import { Story } from "@storybook/blocks";
import type { Meta, StoryObj } from "@storybook/vue3";
import { setup } from "@storybook/vue3";
import { ref } from "vue";
import { RundeckToken } from "../../../../library/interfaces/rundeckWindow";
import { RootStore } from "../../../../library/stores/RootStore";
import OptionEdit from "./OptionEdit.vue";
import { JobOption } from "../../../../library/types/jobs/JobEdit";
import * as uiv from "uiv";

setup((app) => {
  app.use(uiv);
});
const meta: Meta<typeof OptionEdit> = {
  title: "App/JobEdit/Options/OptionEdit",
  component: OptionEdit,
  parameters: {
    componentSubtitle:
      "Form for creating and editing job options in Rundeck workflows",
    docs: {
      description: {
        component: `
# OptionEdit Component

A comprehensive form component for creating and editing job options in Rundeck. This component supports various option types including text, file upload, multiline text, and secure options.

## Features
- Option type selection: text, file, multiline
- Input type configuration: plain text, date, secure (hidden), secure exposed
- Default value management with storage path support for secure options
- Allowed values configuration with multiple sources: list, URL, plugin
- Validation rules including regex pattern validation
- Advanced settings like multi-value support and hidden options
`,
      },
    },
    actions: {
      disable: false,
    },
    controls: {
      disable: false,
    },
    mockData: [
      {
        url: "api/52/projects",
        method: "GET",
        status: 200,
        response: [
          {
            name: "TestProject1",
          },
        ],
      },
      {
        url: "api/52/storage/keys/",
        method: "GET",
        status: 200,
        response: {
          resources: [],
        },
      },
      {
        url: "api/52/storage/keys/project/TestProject1",
        method: "GET",
        status: 200,
        response: {
          resources: [],
        },
      },
    ],
    async beforeEach() {
      const rootStore = new RootStore(window._rundeck.rundeckClient);

      window._rundeck = {
        rdBase: "http://localhost:4440",
        apiVersion: "52",
        projectName: "TestProject.rdproject",
        appMeta: {},
        token: {} as RundeckToken,
        tokens: {},
        navbar: { items: [] as any },
        feature: {},
        data: {},
        rootStore: rootStore,
      };
    },
  },
  argTypes: {
    modelValue: {
      control: {
        type: "object",
      },
      description:
        "The option data being edited. Contains all fields for the job option configuration.",
      table: {
        type: {
          summary: "JobOptionEdit",
          detail:
            "Interface extending JobOption with additional editing properties",
        },
        defaultValue: { summary: "{}" },
      },
    },
    error: {
      control: {
        type: "text",
      },
      description:
        "Error message to display at the top of the form for global option errors.",
      table: {
        type: { summary: "string" },
        defaultValue: { summary: "undefined" },
      },
    },
    newOption: {
      control: {
        type: "boolean",
      },
      description:
        "Whether this is creating a new option or editing an existing one.",
      table: {
        type: { summary: "boolean" },
        defaultValue: { summary: "false" },
      },
    },
    features: {
      control: {
        type: "object",
      },
      description:
        "Feature flags to enable/disable certain functionality like multiline options and file uploads.",
      table: {
        type: {
          summary: "object",
          detail:
            "{ multilineJobOptions?: boolean, fileUploadPlugin?: boolean, optionValuesPlugin?: boolean }",
        },
        defaultValue: { summary: "{}" },
      },
    },
    fileUploadPluginType: {
      control: {
        type: "text",
      },
      description:
        "Type of file upload plugin to use when file options are enabled.",
      table: {
        type: { summary: "string" },
        defaultValue: { summary: '""' },
      },
    },
    errors: {
      control: {
        type: "object",
      },
      description:
        "External validation errors for specific fields in the form.",
      table: {
        type: {
          summary: "Record<string, string>",
          detail:
            "Object with field names as keys and error messages as values",
        },
        defaultValue: { summary: "{}" },
      },
    },
    optionValuesPlugins: {
      control: {
        type: "array",
      },
      description:
        "Available option values plugins that can provide dynamic option choices.",
      table: {
        type: {
          summary: "array",
          detail: "Array of { name: string, title: string } objects",
        },
        defaultValue: { summary: "[]" },
      },
    },
    uiFeatures: {
      control: {
        type: "object",
      },
      description:
        "UI feature configuration options for customizing component behavior.",
      table: {
        type: { summary: "object" },
        defaultValue: { summary: "{}" },
      },
    },
    jobWasScheduled: {
      control: {
        type: "boolean",
      },
      description:
        "Whether the job was previously scheduled (affects option requirements).",
      table: {
        type: { summary: "boolean" },
        defaultValue: { summary: "false" },
      },
    },
  },
  args: {
    modelValue: {
      name: "exampleOption",
      description: "An example job option",
      required: false,
      enforced: false,
      values: ["option1", "option2", "option3"],
      value: "option1",
      sortValues: false,
      type: "text",
      secure: false,
      valueExposed: false,
      isDate: false,
      hidden: false,
      multivalued: false,
      multivalueAllSelected: false,
    } as JobOption,
    newOption: true,
    features: {
      multilineJobOptions: true,
      fileUploadPlugin: true,
      optionValuesPlugin: true,
    },
    fileUploadPluginType: "file-upload",
    optionValuesPlugins: [
      { title: "Option Provider 1", name: "option-provider-1" },
      { title: "Option Provider 2", name: "option-provider-2" },
    ],
    uiFeatures: {},
    jobWasScheduled: false,
    errors: {},
  },
};

export default meta;

type Story = StoryObj<typeof OptionEdit>;

export const Playground: Story = {
  parameters: {
    docs: {
      description:
        "Interactive playground for experimenting with all available OptionEdit component props and configurations.",
    },
  },
  name: "Playground",
  tags: ["!dev"],
  render: (args) => ({
    components: { OptionEdit },
    setup() {
      const optionData = ref(args.modelValue);

      const onSave = () => {
        console.log("Save clicked", optionData.value);
      };

      const onCancel = () => {
        console.log("Cancel clicked");
      };

      return { args, optionData, onSave, onCancel };
    },
    template: `
    <div style="max-width: 800px; margin: 0 auto; padding: 20px;">
      <OptionEdit
        v-model="optionData"
        :error="args.error"
        :newOption="args.newOption"
        :features="args.features"
        :fileUploadPluginType="args.fileUploadPluginType"
        :errors="args.errors"
        :optionValuesPlugins="args.optionValuesPlugins"
        :uiFeatures="args.uiFeatures"
        :jobWasScheduled="args.jobWasScheduled"
        @save="onSave"
        @cancel="onCancel"
      />
    </div>`,
  }),
};

export const NewTextOption: Story = {
  parameters: {
    docs: {
      description:
        "Basic text option with minimal configuration in creation mode. Shows the standard option fields without any advanced features enabled.",
    },
  },
  render: (args) => ({
    components: { OptionEdit },
    setup() {
      const optionData = ref({
        name: "textOption",
        description: "A plain text option",
        required: false,
        enforced: false,
        type: "text",
        secure: false,
        valueExposed: false,
        isDate: false,
        hidden: false,
        multivalued: false,
        multivalueAllSelected: false,
        sortValues: false,
        value: "",
      } as JobOption);

      const onSave = () => {
        console.log("Save clicked", optionData.value);
      };

      const onCancel = () => {
        console.log("Cancel clicked");
      };

      return { args, optionData, onSave, onCancel };
    },
    template: `
    <div style="max-width: 800px; margin: 0 auto; padding: 20px;">
      <OptionEdit
        v-model="optionData"
        :newOption="true"
        :features="args.features"
        @save="onSave"
        @cancel="onCancel"
      />
    </div>`,
  }),
};

export const EditSecureOption: Story = {
  parameters: {
    docs: {
      description:
        "A secure option that uses key storage for the option value. The input will be hidden in logs and execution output.",
    },
  },
  render: (args) => ({
    components: { OptionEdit },
    setup() {
      const optionData = ref({
        name: "secureOption",
        description: "A secure input option that's hidden in logs",
        required: true,
        enforced: true,
        secure: true,
        valueExposed: false,
        storagePath: "keys/secure/mypassword",
        type: "text",
        isDate: false,
        hidden: false,
        multivalued: false,
        multivalueAllSelected: false,
        sortValues: false,
        value: "",
      } as JobOption);

      const onSave = () => {
        console.log("Save clicked", optionData.value);
      };

      const onCancel = () => {
        console.log("Cancel clicked");
      };

      return { args, optionData, onSave, onCancel };
    },
    template: `
    <div style="max-width: 800px; margin: 0 auto; padding: 20px;">
      <OptionEdit
        v-model="optionData"
        :newOption="false"
        :features="args.features"
        @save="onSave"
        @cancel="onCancel"
      />
    </div>`,
  }),
};

export const MultilineOption: Story = {
  parameters: {
    docs: {
      description:
        "A multiline text option that supports entering multi-paragraph text with line breaks. Requires the multilineJobOptions feature to be enabled.",
    },
  },
  render: (args) => ({
    components: { OptionEdit },
    setup() {
      const optionData = ref({
        name: "multilineOption",
        description: "A multiline text option",
        type: "multiline",
        required: false,
        enforced: false,
        value: "Line 1\nLine 2\nLine 3",
        isMultiline: true,
        secure: false,
        valueExposed: false,
        isDate: false,
        hidden: false,
        multivalued: false,
        multivalueAllSelected: false,
        sortValues: false,
      } as JobOption);

      const onSave = () => {
        console.log("Save clicked", optionData.value);
      };

      const onCancel = () => {
        console.log("Cancel clicked");
      };

      return { args, optionData, onSave, onCancel };
    },
    template: `
    <div style="max-width: 800px; margin: 0 auto; padding: 20px;">
      <OptionEdit
        v-model="optionData"
        :newOption="false"
        :features="{
          multilineJobOptions: true,
          fileUploadPlugin: false
        }"
        @save="onSave"
        @cancel="onCancel"
      />
    </div>`,
  }),
};

export const FileUploadOption: Story = {
  parameters: {
    docs: {
      description:
        "A file upload option that allows uploading files during job execution. Requires the fileUploadPlugin feature to be enabled.",
    },
  },
  render: (args) => ({
    components: { OptionEdit },
    setup() {
      const optionData = ref({
        name: "fileOption",
        description: "A file upload option",
        type: "file",
        required: false,
        secure: false,
        valueExposed: false,
        isDate: false,
        hidden: false,
        multivalued: false,
        multivalueAllSelected: false,
        sortValues: false,
        value: "",
      } as JobOption);

      const onSave = () => {
        console.log("Save clicked", optionData.value);
      };

      const onCancel = () => {
        console.log("Cancel clicked");
      };

      return { args, optionData, onSave, onCancel };
    },
    template: `
    <div style="max-width: 800px; margin: 0 auto; padding: 20px;">
      <OptionEdit
        v-model="optionData"
        :newOption="true"
        :features="{
          multilineJobOptions: false,
          fileUploadPlugin: true
        }"
        fileUploadPluginType="file-upload"
        @save="onSave"
        @cancel="onCancel"
      />
    </div>`,
  }),
};

export const OptionWithValidationErrors: Story = {
  parameters: {
    docs: {
      description:
        "An option with validation errors demonstrating error handling and display within the component.",
    },
  },
  render: (args) => ({
    components: { OptionEdit },
    setup() {
      const optionData = ref({
        name: "invalid#Option",
        description: "An option with validation errors",
        type: "text",
        required: true,
        secure: false,
        valueExposed: false,
        isDate: false,
        hidden: false,
        multivalued: false,
        multivalueAllSelected: false,
        sortValues: false,
        value: "",
      } as JobOption);

      const onSave = () => {
        console.log("Save clicked", optionData.value);
      };

      const onCancel = () => {
        console.log("Cancel clicked");
      };

      return {
        args,
        optionData,
        onSave,
        onCancel,
        errors: {
          name: "Option name can only contain letters, numbers, and the characters: '-', '_', '.'",
          value: "A default value is required for required options",
        },
      };
    },
    template: `
    <div style="max-width: 800px; margin: 0 auto; padding: 20px;">
      <OptionEdit
        v-model="optionData"
        :newOption="true"
        :features="args.features"
        :errors="errors"
        error="The option has validation errors"
        @save="onSave"
        @cancel="onCancel"
      />
    </div>`,
  }),
};

export const OptionWithAllowedValues: Story = {
  parameters: {
    docs: {
      description:
        "An option with a predefined list of allowed values that creates a dropdown/select input for the user.",
    },
  },
  render: (args) => ({
    components: { OptionEdit },
    setup() {
      const optionData = ref({
        name: "selectOption",
        description: "An option with allowed values",
        type: "text",
        required: false,
        enforced: true,
        values: ["red", "green", "blue", "yellow", "purple"],
        value: "green",
        sortValues: true,
        secure: false,
        valueExposed: false,
        isDate: false,
        hidden: false,
        multivalued: false,
        multivalueAllSelected: false,
      } as JobOption);

      const onSave = () => {
        console.log("Save clicked", optionData.value);
      };

      const onCancel = () => {
        console.log("Cancel clicked");
      };

      return { args, optionData, onSave, onCancel };
    },
    template: `
    <div style="max-width: 800px; margin: 0 auto; padding: 20px;">
      <OptionEdit
        v-model="optionData"
        :newOption="false"
        :features="args.features"
        @save="onSave"
        @cancel="onCancel"
      />
    </div>`,
  }),
};

export const OptionWithRemoteValues: Story = {
  parameters: {
    docs: {
      description:
        "An option that loads allowed values from a remote URL endpoint at runtime.",
    },
  },
  render: (args) => ({
    components: { OptionEdit },
    setup() {
      const optionData = ref({
        name: "remoteOption",
        description: "An option with remote URL values",
        type: "text",
        required: false,
        enforced: true,
        valuesUrl: "https://example.com/api/options",
        value: "",
        secure: false,
        valueExposed: false,
        isDate: false,
        hidden: false,
        multivalued: false,
        multivalueAllSelected: false,
        sortValues: false,
      } as JobOption);

      const onSave = () => {
        console.log("Save clicked", optionData.value);
      };

      const onCancel = () => {
        console.log("Cancel clicked");
      };

      return { args, optionData, onSave, onCancel };
    },
    template: `
    <div style="max-width: 800px; margin: 0 auto; padding: 20px;">
      <OptionEdit
        v-model="optionData"
        :newOption="false"
        :features="args.features"
        @save="onSave"
        @cancel="onCancel"
      />
    </div>`,
  }),
};

export const OptionWithPluginValues: Story = {
  parameters: {
    docs: {
      description:
        "An option that uses a plugin to provide its allowed values dynamically.",
    },
  },
  render: (args) => ({
    components: { OptionEdit },
    setup() {
      const optionData = ref({
        name: "pluginOption",
        description: "An option with plugin-provided values",
        type: "text",
        required: false,
        enforced: true,
        optionValuesPluginType: "option-provider-1",
        value: "",
        secure: false,
        valueExposed: false,
        isDate: false,
        hidden: false,
        multivalued: false,
        multivalueAllSelected: false,
        sortValues: false,
      } as JobOption);

      const onSave = () => {
        console.log("Save clicked", optionData.value);
      };

      const onCancel = () => {
        console.log("Cancel clicked");
      };

      return {
        args,
        optionData,
        onSave,
        onCancel,
        optionValuesPlugins: [
          { title: "Option Provider 1", name: "option-provider-1" },
          { title: "Option Provider 2", name: "option-provider-2" },
        ],
      };
    },
    template: `
    <div style="max-width: 800px; margin: 0 auto; padding: 20px;">
      <OptionEdit
        v-model="optionData"
        :newOption="false"
        :features="args.features"
        :optionValuesPlugins="optionValuesPlugins"
        @save="onSave"
        @cancel="onCancel"
      />
    </div>`,
  }),
};

export const DateOption: Story = {
  parameters: {
    docs: {
      description:
        "A date input option with configurable date format for selecting dates and times.",
    },
  },
  render: (args) => ({
    components: { OptionEdit },
    setup() {
      const optionData = ref({
        name: "dateOption",
        description: "A date input option",
        type: "text",
        isDate: true,
        dateFormat: "yyyy-MM-dd HH:mm:ss",
        required: false,
        enforced: false,
        secure: false,
        valueExposed: false,
        hidden: false,
        multivalued: false,
        multivalueAllSelected: false,
        sortValues: false,
        value: "",
      } as JobOption);

      const onSave = () => {
        console.log("Save clicked", optionData.value);
      };

      const onCancel = () => {
        console.log("Cancel clicked");
      };

      return { args, optionData, onSave, onCancel };
    },
    template: `
    <div style="max-width: 800px; margin: 0 auto; padding: 20px;">
      <OptionEdit
        v-model="optionData"
        :newOption="true"
        :features="args.features"
        @save="onSave"
        @cancel="onCancel"
      />
    </div>`,
  }),
};
