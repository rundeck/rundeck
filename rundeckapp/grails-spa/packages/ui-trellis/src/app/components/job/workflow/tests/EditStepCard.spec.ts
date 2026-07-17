import { shallowMount, flushPromises, VueWrapper } from "@vue/test-utils";

// --- Mock setup (before imports) ---

const mockGetServiceProviderDescription = jest.fn();

jest.mock("@/library/modules/pluginService", () => ({
  getServiceProviderDescription: (svcName: string, provider: string) =>
    mockGetServiceProviderDescription(svcName, provider),
  validatePluginConfig: jest.fn(),
  getPluginProvidersForService: jest.fn(),
}));

jest.mock("@/library/modules/rundeckClient", () => ({
  client: jest.fn(),
}));

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    rootStore: {
      plugins: {
        getServicePlugins: jest.fn().mockReturnValue([]),
        load: jest.fn(),
      },
      projects: {
        projects: [{ name: "testProject" }],
        loaded: true,
        load: jest.fn(),
      },
    },
    projectName: "testProject",
    eventBus: { on: jest.fn(), off: jest.fn(), emit: jest.fn() },
    rdBase: "http://localhost:4440",
    data: null,
  })),
}));

jest.mock("@/library/stores/Plugins", () => ({
  ServiceType: {
    WorkflowNodeStep: "WorkflowNodeStep",
    WorkflowStep: "WorkflowStep",
  },
  PluginStore: jest.fn(),
}));

jest.mock("@/library/stores/contextVariables", () => ({
  contextVariables: jest.fn().mockReturnValue({
    node: [],
    job: [],
  }),
  ContextVariable: {},
}));

jest.mock("@/library/stores/NodesStorePinia", () => ({
  useNodesStore: () => ({
    total: 0,
    nodeFilterStore: {
      filter: "",
      setSelectedFilter: jest.fn(),
    },
    currentNodes: [],
    lastCountFetched: 0,
    isResultsTruncated: false,
    fetchNodes: jest.fn(),
  }),
}));

jest.mock("@/library/components/utils/contextVariableUtils", () => ({
  getContextVariables: jest.fn().mockReturnValue([]),
  transformVariables: jest.fn().mockReturnValue([]),
}));

jest.mock("@/library/services/projects");
jest.mock("vue-scrollto", () => ({ scrollTo: jest.fn() }));
jest.mock("@/app/utilities/loadJsonData", () => ({
  loadJsonData: jest.fn().mockReturnValue(null),
}));

// --- Import component after mocks ---
import type { EditStepData } from "../types/workflowTypes";
import type { PluginConfig } from "../../../../../library/interfaces/PluginConfig";
import type { ContextVariable } from "../../../../../library/stores/contextVariables";
import type { PluginDetails } from "../stepEditorUtils";
import EditStepCard from "../EditStepCard.vue";

// BaseStepCard must render its slots for the test to see inner content.
const BaseStepCardStub = {
  template: `<div class="p-card">
    <div class="p-card-header"><slot name="header" /></div>
    <div class="p-card-content"><slot name="content" /></div>
    <div class="p-card-footer" data-testid="card-footer"><slot name="footer" /></div>
  </div>`,
};

// JobRefFormFields stub to render the actual form fields
const JobRefFormFieldsStub = {
  name: 'JobRefFormFieldsStub',
  template: `<div class="jobref-form-fields-stub">
    <select data-testid="jobProjectField">
      <option value="testProject">testProject</option>
    </select>
    <input data-testid="jobNameField" :value="modelValue.name" />
    <input data-testid="jobGroupField" :value="modelValue.group" />
    <input data-testid="jobUuidField" :value="modelValue.uuid" />
  </div>`,
  props: ['modelValue', 'showValidation', 'extraAutocompleteVars'],
  emits: ['update:modelValue'],
};

/** Minimal shell so EditStepCard mounts; plugin behavior is driven via $emit in tests when full plugin-config setup is not feasible. */
const pluginConfigStub = {
  name: "pluginConfig",
  template: `<div data-testid="plugin-info"></div>`,
  props: ["modelValue", "mode", "pluginConfig", "showTitle", "showDescription",
    "contextAutocomplete", "validation", "scope", "defaultScope", "groupCss",
    "descriptionCss", "serviceName", "extraAutocompleteVars"],
  emits: ["update:modelValue"],
};

const PtButtonStub = {
  name: "PtButton",
  inheritAttrs: false,
  template: `<button :data-testid="$attrs['data-testid']" :disabled="disabled || undefined" @click="$emit('click')">{{ label }}</button>`,
  props: {
    outlined: Boolean,
    text: Boolean,
    severity: String,
    label: String,
    disabled: { type: Boolean, default: false },
  },
  emits: ["click"],
};

const PtInputStub = {
  name: "PtInput",
  inheritAttrs: false,
  template: `<input :data-testid="$attrs['data-testid']" :value="modelValue" @input="$emit('update:modelValue', $event.target.value)" />`,
  props: ["modelValue", "placeholder"],
};

type EditStepCardTestProps = {
  modelValue?: EditStepData;
  serviceName?: string;
  pluginDetails?: PluginDetails | null;
  validation?: { valid: boolean; errors: Record<string, string> };
  extraAutocompleteVars?: ContextVariable[];
  showNavigation?: boolean;
  depth?: number;
  shouldScrollIntoView?: boolean;
};

// --- Helper to create wrapper ---
const createWrapper = async (
  props: EditStepCardTestProps = {},
): Promise<VueWrapper<InstanceType<typeof EditStepCard>>> => {
  const wrapper = shallowMount(EditStepCard, {
    props: {
      modelValue: {
        type: "script-inline",
        config: {},
        nodeStep: true,
        id: "test-step-1",
      },
      serviceName: "WorkflowNodeStep",
      ...props,
    },
    global: {
      stubs: {
        BaseStepCard: BaseStepCardStub,
        JobRefFormFields: JobRefFormFieldsStub,
        pluginConfig: pluginConfigStub,
        PtButton: PtButtonStub,
        PtInput: PtInputStub,
      },
    },
  });
  await wrapper.vm.$nextTick();
  await flushPromises();
  return wrapper;
};

// --- Mock provider data ---
const mockPluginProvider: PluginDetails = {
  title: "Inline Script",
  description: "Execute an inline script",
  iconUrl: "",
  tooltip: "",
};

// --- Tests ---

describe("EditStepCard", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGetServiceProviderDescription.mockResolvedValue(mockPluginProvider);
  });

  describe("Regular plugin step rendering", () => {
    it("renders save and cancel buttons", async () => {
      const wrapper = await createWrapper({ pluginDetails: mockPluginProvider });

      // Footer is v-show controlled (conditional element)
      expect(wrapper.find('[data-testid="save-button"]').exists()).toBe(true);
      expect(wrapper.find('[data-testid="cancel-button"]').exists()).toBe(true);
    });

    it("loads provider from API when pluginDetails prop is not provided", async () => {
      await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1" },
        serviceName: "WorkflowNodeStep",
      });

      expect(mockGetServiceProviderDescription).toHaveBeenCalledWith(
        "WorkflowNodeStep",
        "script-inline",
      );
    });

    it("uses pluginDetails prop when provided instead of fetching", async () => {
      await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1" },
        serviceName: "WorkflowNodeStep",
        pluginDetails: mockPluginProvider,
      });

      expect(mockGetServiceProviderDescription).not.toHaveBeenCalled();
    });

    it("renders pluginConfig component for regular plugin steps", async () => {
      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1" },
        pluginDetails: mockPluginProvider,
      });

      expect(wrapper.findComponent({ name: "pluginConfig" }).exists()).toBe(true);
      expect(wrapper.find('[data-testid="jobref-form-content"]').exists()).toBe(false);
    });

    it("shows step description input for regular steps", async () => {
      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1", description: "My step" },
        pluginDetails: mockPluginProvider,
      });

      const descInput = wrapper.find('[data-testid="step-description"]');
      expect(descInput.exists()).toBe(true);
      expect((descInput.element as HTMLInputElement).value).toBe("My step");
    });

    it("shows loading state while provider is being fetched", async () => {
      let resolveProvider!: (value: PluginDetails) => void;
      mockGetServiceProviderDescription.mockReturnValue(
        new Promise<PluginDetails>((resolve) => {
          resolveProvider = resolve;
        }),
      );

      const wrapper = shallowMount(EditStepCard, {
        props: {
          modelValue: {
            type: "script-inline",
            config: {},
            nodeStep: true,
            id: "test-1",
          } satisfies EditStepData,
          serviceName: "WorkflowNodeStep",
        },
        global: {
          stubs: { BaseStepCard: BaseStepCardStub, pluginConfig: pluginConfigStub, PtButton: PtButtonStub, PtInput: PtInputStub },
        },
      });
      await wrapper.vm.$nextTick();

      // While provider loads: loading container is visible, pluginConfig is not
      expect(wrapper.find('[data-testid="loading-container"]').exists()).toBe(true);
      expect(wrapper.findComponent({ name: "pluginConfig" }).exists()).toBe(false);

      resolveProvider!(mockPluginProvider);
      await flushPromises();

      // After provider loads: pluginConfig renders, loading container is gone
      expect(wrapper.find('[data-testid="loading-container"]').exists()).toBe(false);
      expect(wrapper.findComponent({ name: "pluginConfig" }).exists()).toBe(true);
    });

    it("passes mode=create to pluginConfig when config is empty", async () => {
      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1" },
        pluginDetails: mockPluginProvider,
      });

      expect(wrapper.findComponent({ name: "pluginConfig" }).props("mode")).toBe("create");
    });

    it("passes mode=edit to pluginConfig when config has values", async () => {
      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: { adhocLocalString: "echo hello" }, nodeStep: true, id: "test-1" },
        pluginDetails: mockPluginProvider,
      });

      expect(wrapper.findComponent({ name: "pluginConfig" }).props("mode")).toBe("edit");
    });
  });

  describe("Job reference step rendering", () => {
    const jobRefModelValue: EditStepData = {
      type: "job.reference",
      id: "test-jobref-1",
      nodeStep: true,
      config: {},
      description: "Run another job",
      jobref: {
        name: "My Job",
        uuid: "",
        group: "test-group",
        project: "testProject",
        args: "",
        nodeStep: true,
        failOnDisable: false,
        childNodes: false,
        importOptions: false,
        ignoreNotifications: false,
      },
    };

    it("renders job reference form when model has jobref property", async () => {
      const wrapper = await createWrapper({
        modelValue: jobRefModelValue,
        serviceName: "WorkflowNodeStep",
      });

      expect(wrapper.find('[data-testid="jobref-form-content"]').exists()).toBe(true);
      expect(wrapper.findComponent({ name: "pluginConfig" }).exists()).toBe(false);
    });

    it("shows job name input with correct value", async () => {
      const wrapper = await createWrapper({
        modelValue: jobRefModelValue,
        serviceName: "WorkflowNodeStep",
      });

      const nameInput = wrapper.find('[data-testid="jobNameField"]');
      expect(nameInput.exists()).toBe(true);
      expect((nameInput.element as HTMLInputElement).value).toBe("My Job");
    });

    it("shows job group input with correct value", async () => {
      const wrapper = await createWrapper({
        modelValue: jobRefModelValue,
        serviceName: "WorkflowNodeStep",
      });

      const groupInput = wrapper.find('[data-testid="jobGroupField"]');
      expect(groupInput.exists()).toBe(true);
      expect((groupInput.element as HTMLInputElement).value).toBe("test-group");
    });

    it("shows project selector", async () => {
      const wrapper = await createWrapper({
        modelValue: jobRefModelValue,
        serviceName: "WorkflowNodeStep",
      });

      expect(wrapper.find('[data-testid="jobProjectField"]').exists()).toBe(true);
    });

    it("shows step description for job references", async () => {
      const wrapper = await createWrapper({
        modelValue: jobRefModelValue,
        serviceName: "WorkflowNodeStep",
      });

      const descInput = wrapper.find('[data-testid="step-description"]');
      expect(descInput.exists()).toBe(true);
      expect((descInput.element as HTMLInputElement).value).toBe("Run another job");
    });

    it("renders jobref form (not pluginConfig) when model has jobref property", async () => {
      const wrapper = await createWrapper({
        modelValue: jobRefModelValue,
        serviceName: "WorkflowNodeStep",
      });

      expect(wrapper.find('[data-testid="jobref-form-content"]').exists()).toBe(true);
    });

    it("does not render jobref form for regular plugin steps", async () => {
      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1" },
        pluginDetails: mockPluginProvider,
      });

      expect(wrapper.find('[data-testid="jobref-form-content"]').exists()).toBe(false);
    });
  });

  describe("Save flow", () => {
    it("emits save and update:modelValue for regular plugin step", async () => {
      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: { adhocLocalString: "echo test" }, nodeStep: true, id: "test-1" },
        pluginDetails: mockPluginProvider,
      });

      await wrapper.find('[data-testid="save-button"]').trigger("click");

      expect(wrapper.emitted("save")).toBeTruthy();
      expect(wrapper.emitted("save")!.length).toBe(1);
      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
      expect(wrapper.emitted("update:modelValue")!.length).toBe(1);
    });

    it("includes description in save data", async () => {
      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1", description: "My step" },
        pluginDetails: mockPluginProvider,
      });

      await wrapper.find('[data-testid="step-description"]').setValue("Updated step name");
      await wrapper.find('[data-testid="save-button"]').trigger("click");

      const emittedData = wrapper.emitted("update:modelValue")![0][0] as EditStepData;
      expect(emittedData.description).toBe("Updated step name");
    });

    it("keeps log filters on Save when plugin-config emits a narrowed model (filters cleared)", async () => {
      const filterEntry: PluginConfig = {
        type: "log-filter",
        config: { pattern: ".*" },
      };
      const modelValue: EditStepData = {
        type: "script-inline",
        config: { adhocLocalString: "echo x" },
        nodeStep: true,
        id: "step-with-filters",
        description: "Named step",
        filters: [filterEntry],
      };
      const wrapper = await createWrapper({
        modelValue,
        pluginDetails: mockPluginProvider,
      });

      const pluginCfg = wrapper.findComponent({ name: "pluginConfig" });
      const current = pluginCfg.props("modelValue") as EditStepData;
      const narrowed: EditStepData = { ...current, filters: [] };
      await pluginCfg.vm.$emit("update:modelValue", narrowed);
      await wrapper.vm.$nextTick();

      await wrapper.find('[data-testid="save-button"]').trigger("click");

      const emittedData = wrapper.emitted("update:modelValue")![0][0] as EditStepData;
      expect(emittedData.filters).toEqual([filterEntry]);
      expect(emittedData.id).toBe("step-with-filters");
    });

    it("emits save for job reference with valid name", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "job.reference",
          id: "test-jobref-1",
          nodeStep: true,
          config: {},
          jobref: { name: "My Job", uuid: "", group: "", project: "testProject", args: "", nodeStep: true },
        },
        serviceName: "WorkflowNodeStep",
      });

      await wrapper.find('[data-testid="save-button"]').trigger("click");

      expect(wrapper.emitted("save")).toBeTruthy();
      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    });

    it("emits save for job reference with valid uuid", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "job.reference",
          id: "test-jobref-1",
          nodeStep: true,
          config: {},
          jobref: { name: "", uuid: "abc-123-def", group: "", project: "testProject", args: "", nodeStep: true },
        },
        serviceName: "WorkflowNodeStep",
      });

      await wrapper.find('[data-testid="save-button"]').trigger("click");

      expect(wrapper.emitted("save")).toBeTruthy();
    });
  });

  describe("Cancel flow", () => {
    it("emits cancel when cancel button is clicked", async () => {
      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1" },
        pluginDetails: mockPluginProvider,
      });

      await wrapper.find('[data-testid="cancel-button"]').trigger("click");

      expect(wrapper.emitted("cancel")).toBeTruthy();
      expect(wrapper.emitted("cancel")!.length).toBe(1);
    });

    it("does not emit save when cancel is clicked", async () => {
      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1" },
        pluginDetails: mockPluginProvider,
      });

      await wrapper.find('[data-testid="cancel-button"]').trigger("click");

      expect(wrapper.emitted("save")).toBeFalsy();
    });
  });

  describe("Validation error display", () => {
    it("passes validation prop to pluginConfig component", async () => {
      const validationErrors = {
        valid: false,
        errors: { adhocLocalString: "Script is required" },
      };

      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1" },
        pluginDetails: mockPluginProvider,
        validation: validationErrors,
      });

      const pluginConfigComp = wrapper.findComponent({ name: "pluginConfig" });
      expect(pluginConfigComp.exists()).toBe(true);
      expect(pluginConfigComp.props("validation")).toEqual(validationErrors);
    });
  });

  describe("Step description field", () => {
    it("shows description from modelValue in the input", async () => {
      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1", description: "Initial description" },
        pluginDetails: mockPluginProvider,
      });

      expect((wrapper.find('[data-testid="step-description"]').element as HTMLInputElement).value).toBe("Initial description");
    });

    it("shows empty description when modelValue has no description", async () => {
      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1" },
        pluginDetails: mockPluginProvider,
      });

      expect((wrapper.find('[data-testid="step-description"]').element as HTMLInputElement).value).toBe("");
    });

    it("updated description appears in the save payload after typing", async () => {
      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1" },
        pluginDetails: mockPluginProvider,
      });

      await wrapper.find('[data-testid="step-description"]').setValue("New description");
      await wrapper.find('[data-testid="save-button"]').trigger("click");

      const emittedData = wrapper.emitted("update:modelValue")![0][0] as EditStepData;
      expect(emittedData.description).toBe("New description");
    });
  });

  describe("Model initialization", () => {
    it("shows description from modelValue in the step description input", async () => {
      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1", description: "Step desc" },
        pluginDetails: mockPluginProvider,
      });

      expect((wrapper.find('[data-testid="step-description"]').element as HTMLInputElement).value).toBe("Step desc");
    });

    it("passes jobref data to JobRefFormFields component", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "job.reference",
          id: "test-jobref-1",
          nodeStep: true,
          config: {},
          jobref: {
            name: "My Job",
            uuid: "",
            project: "testProject",
            group: "",
            args: "",
            nodeStep: true,
            failOnDisable: false,
            childNodes: false,
            importOptions: false,
            ignoreNotifications: false,
            nodefilters: {
              filter: "",
              dispatch: {
                threadcount: undefined,
                keepgoing: undefined,
                rankAttribute: undefined,
                rankOrder: undefined,
                nodeIntersect: undefined,
              },
            },
          },
        },
        serviceName: "WorkflowNodeStep",
      });

      const jobrefComp = wrapper.findComponent({ name: "JobRefFormFieldsStub" });
      expect(jobrefComp.exists()).toBe(true);
      expect(jobrefComp.props("modelValue").name).toBe("My Job");
    });
  });

  describe("Props", () => {
    it("accepts showNavigation prop", async () => {
      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1" },
        pluginDetails: mockPluginProvider,
        showNavigation: true,
      });

      expect(wrapper.props("showNavigation")).toBe(true);
    });

    it("accepts extraAutocompleteVars prop", async () => {
      const vars: ContextVariable[] = [
        { name: "myVar", type: "option", title: "My Var" },
      ];
      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1" },
        pluginDetails: mockPluginProvider,
        extraAutocompleteVars: vars,
      });

      expect(wrapper.props("extraAutocompleteVars")).toEqual(vars);
    });
  });

  describe("Footer buttons", () => {
    it("renders both Save and Cancel buttons", async () => {
      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1" },
        pluginDetails: mockPluginProvider,
      });

      // Both buttons are inside a v-show container (conditional)
      const saveButtons = wrapper.findAllComponents({ name: "PtButton" }).filter(
        (b) => b.attributes("data-testid") === "save-button"
      );
      const cancelButtons = wrapper.findAllComponents({ name: "PtButton" }).filter(
        (b) => b.attributes("data-testid") === "cancel-button"
      );
      expect(saveButtons.length).toBe(1);
      expect(cancelButtons.length).toBe(1);
    });
  });

  describe("Button DOM interactions", () => {
    it("clicking cancel button emits cancel", async () => {
      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1" },
        pluginDetails: mockPluginProvider,
      });

      await wrapper.find('[data-testid="cancel-button"]').trigger("click");

      expect(wrapper.emitted("cancel")).toBeTruthy();
    });

    it("clicking save button emits save and update:modelValue for a regular step", async () => {
      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1" },
        pluginDetails: mockPluginProvider,
      });

      await wrapper.find('[data-testid="save-button"]').trigger("click");

      expect(wrapper.emitted("save")).toBeTruthy();
      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    });
  });

  describe("Async pluginDetails prop updates", () => {
    it("renders pluginConfig when pluginDetails prop is provided after mount", async () => {
      // Simulate ChoosePluginsEAModal scenario:
      // Component mounts without pluginDetails, API returns nothing initially
      mockGetServiceProviderDescription.mockResolvedValueOnce(null);

      const wrapper = shallowMount(EditStepCard, {
        props: {
          modelValue: { type: "script-file-url", config: {}, nodeStep: true, id: "test-1" },
          serviceName: "WorkflowNodeStep",
          pluginDetails: null,
        },
        global: {
          stubs: {
            BaseStepCard: BaseStepCardStub,
            JobRefFormFields: JobRefFormFieldsStub,
            pluginConfig: pluginConfigStub,
            PtButton: PtButtonStub,
            PtInput: PtInputStub,
          },
        },
      });

      await wrapper.vm.$nextTick();
      await flushPromises();

      // Initially: no plugin config should render (API returned null)
      expect(wrapper.findComponent({ name: "pluginConfig" }).exists()).toBe(false);
      expect(wrapper.find('[data-testid="plugin-info"]').exists()).toBe(false);

      // Parent component finishes async fetch and provides pluginDetails
      const asyncLoadedPluginDetails = {
        name: "script-file-url",
        title: "Script file or URL",
        description: "Execute a local script file or a script from a URL",
        iconUrl: "http://localhost:4440/plugin/icon/WorkflowNodeStep/script-file-url",
        props: {},
      };

      await wrapper.setProps({ pluginDetails: asyncLoadedPluginDetails });
      await flushPromises();

      // Now pluginConfig should render (form is interactive)
      expect(wrapper.findComponent({ name: "pluginConfig" }).exists()).toBe(true);
      expect(wrapper.find('[data-testid="plugin-info"]').exists()).toBe(true);
      expect(wrapper.find('[data-testid="loading-container"]').exists()).toBe(false);
    });

    it("transitions from loading state to form when pluginDetails arrives", async () => {
      const wrapper = shallowMount(EditStepCard, {
        props: {
          modelValue: { type: "script-file-url", config: {}, nodeStep: true, id: "dkzxn" },
          serviceName: "WorkflowNodeStep",
          pluginDetails: undefined,
        },
        global: {
          stubs: {
            BaseStepCard: BaseStepCardStub,
            pluginConfig: pluginConfigStub,
            PtButton: PtButtonStub,
          },
        },
      });

      await wrapper.vm.$nextTick();

      // Before pluginDetails arrives: either loading or no content
      const hasPluginConfigBefore = wrapper.findComponent({ name: "pluginConfig" }).exists();
      expect(hasPluginConfigBefore).toBe(false);

      // Parent provides pluginDetails
      const pluginDetails = {
        title: "Script file or URL",
        description: "Execute a local script file or a script from a URL",
        props: {},
      };

      await wrapper.setProps({ pluginDetails });
      await flushPromises();

      // After pluginDetails arrives: form renders
      expect(wrapper.findComponent({ name: "pluginConfig" }).exists()).toBe(true);
      expect(wrapper.find('[data-testid="plugin-info"]').exists()).toBe(true);
    });

    it("does not call API when pluginDetails prop updates from null to defined", async () => {
      const wrapper = shallowMount(EditStepCard, {
        props: {
          modelValue: { type: "script-file-url", config: {}, nodeStep: true, id: "test-1" },
          serviceName: "WorkflowNodeStep",
          pluginDetails: null,
        },
        global: {
          stubs: {
            BaseStepCard: BaseStepCardStub,
            pluginConfig: pluginConfigStub,
            PtButton: PtButtonStub,
          },
        },
      });

      await wrapper.vm.$nextTick();
      await flushPromises();

      // Clear mount-time API calls
      mockGetServiceProviderDescription.mockClear();

      const pluginDetails = {
        name: "script-file-url",
        title: "Script file or URL",
        props: {},
      };

      await wrapper.setProps({ pluginDetails });
      await flushPromises();

      // Should use prop, not fetch from API
      expect(mockGetServiceProviderDescription).not.toHaveBeenCalled();
      expect(wrapper.findComponent({ name: "pluginConfig" }).exists()).toBe(true);
    });

    it("updates step description input when modelValue description changes", async () => {
      const wrapper = await createWrapper({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1", description: "Original" },
        pluginDetails: mockPluginProvider,
      });

      expect((wrapper.find('[data-testid="step-description"]').element as HTMLInputElement).value).toBe("Original");

      await wrapper.setProps({
        modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1", description: "Updated" },
      });
      await flushPromises();

      expect((wrapper.find('[data-testid="step-description"]').element as HTMLInputElement).value).toBe("Updated");
    });
  });

  describe("Error handler and log filter sections (RUN-4315 core parity)", () => {
    const mockErrorHandler = {
      id: "eh-1",
      type: "exec",
      config: { exec: "echo hello" },
      nodeStep: true,
      keepgoingOnSuccess: false,
    };

    const mockLogFilter = {
      type: "mask-passwords",
      config: { pattern: "secret" },
    };

    const ConfigSectionStub = {
      name: "ConfigSection",
      template: `<div>
        <button data-testid="config-section-add-btn" @click="$emit('addElement')">+ Add</button>
        <div v-for="(el, i) in modelValue" :key="i" data-testid="config-section-chip" @click="$emit('editElement', el, i)">
          {{ el.type }}
          <button data-testid="config-section-remove-btn" @click.stop="$emit('removeElement', i)">x</button>
        </div>
        <slot name="content" />
      </div>`,
      props: ["title", "tooltip", "modelValue", "hideWhenSingle", "hideIcon", "isEditView"],
      emits: ["addElement", "removeElement", "editElement", "update:modelValue"],
    };

    const InlinePluginConfigFormStub = {
      name: "InlinePluginConfigForm",
      template: `<div data-testid="inline-plugin-config-form-stub"><slot name="extra" /></div>`,
      props: ["modelValue", "serviceName", "validation", "extraAutocompleteVars", "showButtons"],
      emits: ["update:modelValue", "save", "cancel"],
      methods: {
        getFormData(this: { modelValue: Record<string, unknown> }) {
          return this.modelValue;
        },
      },
    };

    const createSectionWrapper = async (
      props: EditStepCardTestProps = {},
    ): Promise<VueWrapper<InstanceType<typeof EditStepCard>>> => {
      const wrapper = shallowMount(EditStepCard, {
        props: {
          modelValue: {
            type: "script-inline",
            config: {},
            nodeStep: true,
            id: "test-step-1",
          },
          serviceName: "WorkflowNodeStep",
          pluginDetails: mockPluginProvider,
          ...props,
        },
        global: {
          stubs: {
            BaseStepCard: BaseStepCardStub,
            JobRefFormFields: JobRefFormFieldsStub,
            pluginConfig: pluginConfigStub,
            PtButton: PtButtonStub,
            PtInput: PtInputStub,
            ConfigSection: ConfigSectionStub,
            inlinePluginConfigForm: InlinePluginConfigFormStub,
          },
        },
      });
      await wrapper.vm.$nextTick();
      await flushPromises();
      return wrapper;
    };

    it("shows the inline error handler form (no chip) when an error handler exists", async () => {
      const wrapper = await createSectionWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-step-1",
          errorhandler: mockErrorHandler,
        },
      });

      expect(
        wrapper.find('[data-testid="inline-plugin-config-form-stub"]').exists(),
      ).toBe(true);
      expect((wrapper.vm as any).showInlineErrorHandlerForm).toBe(true);
    });

    it("shows a chip (not the inline form) for log filters", async () => {
      const wrapper = await createSectionWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-step-1",
          filters: [mockLogFilter],
        },
      });

      const logFilterSection = wrapper.find('[data-testid="log-filter-section"]');
      expect(logFilterSection.find('[data-testid="config-section-chip"]').exists()).toBe(true);
    });

    it("clicking + Add on the error handler section emits add-error-handler", async () => {
      const wrapper = await createSectionWrapper();

      const errorHandlerSection = wrapper.find('[data-testid="error-handler-section"]');
      await errorHandlerSection
        .find('[data-testid="config-section-add-btn"]')
        .trigger("click");

      expect(wrapper.emitted("add-error-handler")).toHaveLength(1);
    });

    it("clicking + Add on the log filter section emits add-log-filter", async () => {
      const wrapper = await createSectionWrapper();

      const logFilterSection = wrapper.find('[data-testid="log-filter-section"]');
      await logFilterSection
        .find('[data-testid="config-section-add-btn"]')
        .trigger("click");

      expect(wrapper.emitted("add-log-filter")).toHaveLength(1);
    });

    it("removing the log filter chip emits remove-log-filter with its index", async () => {
      const wrapper = await createSectionWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-step-1",
          filters: [mockLogFilter],
        },
      });

      const logFilterSection = wrapper.find('[data-testid="log-filter-section"]');
      await logFilterSection
        .find('[data-testid="config-section-remove-btn"]')
        .trigger("click");

      expect(wrapper.emitted("remove-log-filter")).toHaveLength(1);
      expect((wrapper.emitted("remove-log-filter") as any[])[0][0]).toBe(0);
    });

    it("editing an existing error handler does not open a modal (edit-error-handler is not emitted)", async () => {
      const wrapper = await createSectionWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-step-1",
          errorhandler: mockErrorHandler,
        },
      });

      const errorHandlerSection = wrapper.find('[data-testid="error-handler-section"]');
      const chip = errorHandlerSection.find('[data-testid="config-section-chip"]');
      if (chip.exists()) {
        await chip.trigger("click");
      }

      expect(wrapper.emitted("edit-error-handler")).toBeUndefined();
    });

    it("Save persists the edited error handler config from the inline form", async () => {
      const wrapper = await createSectionWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-step-1",
          errorhandler: mockErrorHandler,
        },
      });

      const inlineForm = wrapper.findComponent({ name: "InlinePluginConfigForm" });
      await inlineForm.vm.$emit("update:model-value", {
        type: "exec",
        config: { exec: "echo updated" },
      });

      await wrapper.find('[data-testid="save-button"]').trigger("click");
      await flushPromises();

      const emissions = wrapper.emitted("update:modelValue");
      const savedStep = emissions?.[emissions.length - 1]?.[0] as any;
      expect(savedStep.errorhandler.config.exec).toBe("echo updated");
    });

    it("Save preserves keepgoingOnSuccess after the checkbox is checked", async () => {
      const wrapper = await createSectionWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-step-1",
          errorhandler: { ...mockErrorHandler, keepgoingOnSuccess: false },
        },
      });

      const checkbox = wrapper.find("#editStepKeepgoingOnSuccess");
      await checkbox.setValue(true);

      await wrapper.find('[data-testid="save-button"]').trigger("click");
      await flushPromises();

      const emissions = wrapper.emitted("update:modelValue");
      const savedStep = emissions?.[emissions.length - 1]?.[0] as any;
      expect(savedStep.errorhandler.keepgoingOnSuccess).toBe(true);
    });

    it("Save includes filters carried over from modelValue", async () => {
      const wrapper = await createSectionWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-step-1",
          filters: [mockLogFilter],
        },
      });

      await wrapper.find('[data-testid="save-button"]').trigger("click");
      await flushPromises();

      const emissions = wrapper.emitted("update:modelValue");
      const savedStep = emissions?.[emissions.length - 1]?.[0] as any;
      expect(savedStep.filters[0].config.pattern).toBe("secret");
    });

    it("Cancel emits cancel without touching the error handler/log filter sections", async () => {
      const wrapper = await createSectionWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-step-1",
          errorhandler: mockErrorHandler,
          filters: [mockLogFilter],
        },
      });

      await wrapper.find('[data-testid="cancel-button"]').trigger("click");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("cancel")).toHaveLength(1);
      expect(wrapper.emitted("update:modelValue")).toBeUndefined();
    });

    it("does not render error handler or log filter sections for job reference steps", async () => {
      const wrapper = await createSectionWrapper({
        modelValue: {
          type: "job.reference",
          config: {},
          nodeStep: false,
          id: "test-step-1",
          jobref: { name: "test-job", group: "", uuid: "" },
        },
      });

      expect(wrapper.find('[data-testid="error-handler-section"]').exists()).toBe(false);
      expect(wrapper.find('[data-testid="log-filter-section"]').exists()).toBe(false);
    });
  });
});
