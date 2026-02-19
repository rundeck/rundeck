import { shallowMount, flushPromises, VueWrapper } from "@vue/test-utils";

// --- Mock setup (before imports) ---

const mockGetServiceProviderDescription = jest.fn();

jest.mock("@/library/modules/pluginService", () => ({
  getServiceProviderDescription: (...args: any[]) =>
    mockGetServiceProviderDescription(...args),
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

jest.mock("../../../../../library/services/projects");
jest.mock("vue-scrollto", () => ({ scrollTo: jest.fn() }));
jest.mock("@/app/utilities/loadJsonData", () => ({
  loadJsonData: jest.fn().mockReturnValue(null),
}));

// --- Import component after mocks ---
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

// --- Helper to create wrapper ---
const createWrapper = async (
  props: Record<string, any> = {},
): Promise<VueWrapper<any>> => {
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
const mockPluginProvider = {
  name: "script-inline",
  title: "Inline Script",
  description: "Execute an inline script",
  props: {},
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
      let resolveProvider: (val: any) => void;
      mockGetServiceProviderDescription.mockReturnValue(
        new Promise((r) => { resolveProvider = r; }),
      );

      const wrapper = shallowMount(EditStepCard, {
        props: {
          modelValue: { type: "script-inline", config: {}, nodeStep: true, id: "test-1" },
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
    const jobRefModelValue = {
      type: "job.reference",
      id: "test-jobref-1",
      nodeStep: true,
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

      const emittedData = wrapper.emitted("update:modelValue")![0][0] as any;
      expect(emittedData.description).toBe("Updated step name");
    });

    it("emits save for job reference with valid name", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "job.reference",
          id: "test-jobref-1",
          nodeStep: true,
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

      const emittedData = wrapper.emitted("update:modelValue")![0][0] as any;
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
                threadcount: null,
                keepgoing: null,
                rankAttribute: null,
                rankOrder: null,
                nodeIntersect: null,
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
      const vars = [{ name: "myVar", type: "option", title: "My Var" }];
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

  describe("Conditional logic step", () => {
    const conditionalModel = {
      type: "conditional.logic",
      config: { conditionSet: [], subSteps: [] },
      nodeStep: true,
      id: "test-conditional-1",
    };

    const conditionalModelWithSubSteps = {
      type: "conditional.logic",
      config: {
        conditionSet: [],
        subSteps: [
          { type: "exec-command", config: { adhocRemoteString: "echo" }, nodeStep: true, id: "sub-1" },
        ],
      },
      nodeStep: true,
      id: "test-conditional-1",
    };

    it("renders ConditionsEditor when type is conditional.logic", async () => {
      const wrapper = await createWrapper({ modelValue: conditionalModel });

      expect(wrapper.findComponent({ name: "ConditionsEditor" }).exists()).toBe(true);
    });

    it("does not render pluginConfig for conditional.logic steps", async () => {
      const wrapper = await createWrapper({ modelValue: conditionalModel });

      expect(wrapper.findComponent({ name: "pluginConfig" }).exists()).toBe(false);
    });

    it("does not render jobref form for conditional.logic steps", async () => {
      const wrapper = await createWrapper({ modelValue: conditionalModel });

      expect(wrapper.find('[data-testid="step-description"]').exists()).toBe(true);
      expect(wrapper.findComponent({ name: "JobRefFormFieldsStub" }).exists()).toBe(false);
    });

    it("save button is disabled when there are no inner commands", async () => {
      const wrapper = await createWrapper({ modelValue: conditionalModel });

      expect(wrapper.find('[data-testid="save-button"]').attributes("disabled")).toBeDefined();
    });

    it("save button is enabled when inner commands exist", async () => {
      const wrapper = await createWrapper({ modelValue: conditionalModelWithSubSteps });

      expect(wrapper.find('[data-testid="save-button"]').attributes("disabled")).toBeUndefined();
    });

    it("save button becomes disabled when InnerStepList signals editing started", async () => {
      const wrapper = await createWrapper({ modelValue: conditionalModelWithSubSteps });
      const innerStepList = wrapper.findComponent({ name: "InnerStepList" });

      await innerStepList.vm.$emit("update:editing", true);
      await wrapper.vm.$nextTick();

      expect(wrapper.find('[data-testid="save-button"]').attributes("disabled")).toBeDefined();
    });

    it("clicking save emits save and update:modelValue with conditionSet and subSteps", async () => {
      const wrapper = await createWrapper({ modelValue: conditionalModelWithSubSteps });

      await wrapper.find('[data-testid="save-button"]').trigger("click");

      expect(wrapper.emitted("save")).toBeTruthy();
      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
      const payload = wrapper.emitted("update:modelValue")![0][0] as any;
      expect(payload.config?.conditionSet).toBeDefined();
      expect(payload.config?.subSteps).toBeDefined();
    });

    it("clicking save includes the step description in the payload", async () => {
      const wrapper = await createWrapper({ modelValue: { ...conditionalModelWithSubSteps, description: "My Step" } });

      await wrapper.find('[data-testid="save-button"]').trigger("click");

      const payload = wrapper.emitted("update:modelValue")![0][0] as any;
      expect(payload.description).toBe("My Step");
    });
  });
});
