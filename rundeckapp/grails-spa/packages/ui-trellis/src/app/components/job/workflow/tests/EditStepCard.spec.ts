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

// --- Import component after mocks ---
import EditStepCard from "../EditStepCard.vue";

// Card is the only manual stub needed because its named slots (#header, #content, #footer)
// must render for the test to see inner content. Everything else is auto-stubbed by shallowMount.
const CardStub = {
  template: `<div class="p-card">
    <div class="p-card-header"><slot name="header" /></div>
    <div class="p-card-content"><slot name="content" /></div>
    <div class="p-card-footer"><slot name="footer" /></div>
  </div>`,
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
        Card: CardStub,
      },
      mocks: {
        $t: (key: string, args?: any[]) => {
          if (args) return `${key}[${args.join(",")}]`;
          return key;
        },
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
    it("renders the component", async () => {
      const wrapper = await createWrapper();
      expect(wrapper.exists()).toBe(true);
    });

    it("loads provider from API when pluginDetails prop is not provided", async () => {
      await createWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-1",
        },
        serviceName: "WorkflowNodeStep",
      });

      expect(mockGetServiceProviderDescription).toHaveBeenCalledWith(
        "WorkflowNodeStep",
        "script-inline",
      );
    });

    it("uses pluginDetails prop when provided instead of fetching", async () => {
      await createWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-1",
        },
        serviceName: "WorkflowNodeStep",
        pluginDetails: mockPluginProvider,
      });

      expect(mockGetServiceProviderDescription).not.toHaveBeenCalled();
    });

    it("renders pluginConfig component for regular plugin steps", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-1",
        },
        pluginDetails: mockPluginProvider,
      });

      expect(wrapper.findComponent({ name: "pluginConfig" }).exists()).toBe(
        true,
      );
      expect(wrapper.find(".jobref-form-content").exists()).toBe(false);
    });

    it("shows step description input for regular steps", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-1",
          description: "My step",
        },
        pluginDetails: mockPluginProvider,
      });

      const descInput = wrapper.find("input[data-testid='step-description']");
      expect(descInput.exists()).toBe(true);
      expect((descInput.element as HTMLInputElement).value).toBe("My step");
    });

    it("renders StepCardHeader when provider is loaded", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-1",
        },
        pluginDetails: mockPluginProvider,
      });

      expect(
        wrapper.findComponent({ name: "StepCardHeader" }).exists(),
      ).toBe(true);
    });

    it("shows loading state while provider is being fetched", async () => {
      let resolveProvider: (val: any) => void;
      mockGetServiceProviderDescription.mockReturnValue(
        new Promise((r) => {
          resolveProvider = r;
        }),
      );

      const wrapper = shallowMount(EditStepCard, {
        props: {
          modelValue: {
            type: "script-inline",
            config: {},
            nodeStep: true,
            id: "test-1",
          },
          serviceName: "WorkflowNodeStep",
        },
        global: {
          stubs: { Card: CardStub },
          mocks: { $t: (key: string) => key },
        },
      });
      await wrapper.vm.$nextTick();

      expect((wrapper.vm as any).loading).toBe(true);

      resolveProvider!(mockPluginProvider);
      await flushPromises();

      expect((wrapper.vm as any).loading).toBe(false);
      expect((wrapper.vm as any).provider).toEqual(mockPluginProvider);
    });

    it("sets pluginConfigMode to create when config is empty", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-1",
        },
        pluginDetails: mockPluginProvider,
      });

      expect((wrapper.vm as any).pluginConfigMode).toBe("create");
    });

    it("sets pluginConfigMode to edit when config has values", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "script-inline",
          config: { adhocLocalString: "echo hello" },
          nodeStep: true,
          id: "test-1",
        },
        pluginDetails: mockPluginProvider,
      });

      expect((wrapper.vm as any).pluginConfigMode).toBe("edit");
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

      expect(wrapper.find(".jobref-form-content").exists()).toBe(true);
      expect(
        wrapper.findComponent({ name: "pluginConfig" }).exists(),
      ).toBe(false);
    });

    it("shows job name input with correct value", async () => {
      const wrapper = await createWrapper({
        modelValue: jobRefModelValue,
        serviceName: "WorkflowNodeStep",
      });

      const nameInput = wrapper.find("input[data-testid='jobNameField']");
      expect(nameInput.exists()).toBe(true);
      expect((nameInput.element as HTMLInputElement).value).toBe("My Job");
    });

    it("shows job group input with correct value", async () => {
      const wrapper = await createWrapper({
        modelValue: jobRefModelValue,
        serviceName: "WorkflowNodeStep",
      });

      const groupInput = wrapper.find("input[data-testid='jobGroupField']");
      expect(groupInput.exists()).toBe(true);
      expect((groupInput.element as HTMLInputElement).value).toBe(
        "test-group",
      );
    });

    it("shows project selector", async () => {
      const wrapper = await createWrapper({
        modelValue: jobRefModelValue,
        serviceName: "WorkflowNodeStep",
      });

      const projectSelect = wrapper.find(
        "select[data-testid='jobProjectField']",
      );
      expect(projectSelect.exists()).toBe(true);
    });

    it("shows step description for job references", async () => {
      const wrapper = await createWrapper({
        modelValue: jobRefModelValue,
        serviceName: "WorkflowNodeStep",
      });

      const descInput = wrapper.find("input[data-testid='step-description']");
      expect(descInput.exists()).toBe(true);
      expect((descInput.element as HTMLInputElement).value).toBe(
        "Run another job",
      );
    });

    it("correctly identifies isJobRef computed", async () => {
      const wrapper = await createWrapper({
        modelValue: jobRefModelValue,
        serviceName: "WorkflowNodeStep",
      });

      expect((wrapper.vm as any).isJobRef).toBe(true);
    });

    it("isJobRef is false for regular plugin steps", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-1",
        },
        pluginDetails: mockPluginProvider,
      });

      expect((wrapper.vm as any).isJobRef).toBe(false);
    });
  });

  describe("Save flow", () => {
    it("emits save and update:modelValue for regular plugin step", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "script-inline",
          config: { adhocLocalString: "echo test" },
          nodeStep: true,
          id: "test-1",
        },
        pluginDetails: mockPluginProvider,
      });

      // Call handleSave directly since shallowMount stubs PtButton
      (wrapper.vm as any).handleSave();
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("save")).toBeTruthy();
      expect(wrapper.emitted("save")!.length).toBe(1);
      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
      expect(wrapper.emitted("update:modelValue")!.length).toBe(1);
    });

    it("includes description in save data", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-1",
          description: "My step",
        },
        pluginDetails: mockPluginProvider,
      });

      // Update description via the native input
      const descInput = wrapper.find("input[data-testid='step-description']");
      await descInput.setValue("Updated step name");

      (wrapper.vm as any).handleSave();
      await wrapper.vm.$nextTick();

      const emittedData = wrapper.emitted("update:modelValue")![0][0] as any;
      expect(emittedData.description).toBe("Updated step name");
    });

    it("emits save for job reference with valid name", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "job.reference",
          id: "test-jobref-1",
          nodeStep: true,
          jobref: {
            name: "My Job",
            uuid: "",
            group: "",
            project: "testProject",
            args: "",
            nodeStep: true,
          },
        },
        serviceName: "WorkflowNodeStep",
      });

      (wrapper.vm as any).handleSave();
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("save")).toBeTruthy();
      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    });

    it("emits save for job reference with valid uuid", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "job.reference",
          id: "test-jobref-1",
          nodeStep: true,
          jobref: {
            name: "",
            uuid: "abc-123-def",
            group: "",
            project: "testProject",
            args: "",
            nodeStep: true,
          },
        },
        serviceName: "WorkflowNodeStep",
      });

      (wrapper.vm as any).handleSave();
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("save")).toBeTruthy();
    });

    it("blocks save for job reference without name or uuid", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "job.reference",
          id: "test-jobref-1",
          nodeStep: true,
          jobref: {
            name: "",
            uuid: "",
            group: "",
            project: "testProject",
            args: "",
            nodeStep: true,
          },
        },
        serviceName: "WorkflowNodeStep",
      });

      (wrapper.vm as any).handleSave();
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("save")).toBeFalsy();
      expect(wrapper.emitted("update:modelValue")).toBeFalsy();
      expect((wrapper.vm as any).validationError).toBeTruthy();
      expect((wrapper.vm as any).showRequired).toBe(true);
    });

    it("shows validation error message in the DOM for invalid job ref", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "job.reference",
          id: "test-jobref-1",
          nodeStep: true,
          jobref: {
            name: "",
            uuid: "",
            group: "",
            project: "testProject",
            args: "",
            nodeStep: true,
          },
        },
        serviceName: "WorkflowNodeStep",
      });

      (wrapper.vm as any).handleSave();
      await wrapper.vm.$nextTick();

      const alert = wrapper.find(".alert-danger");
      expect(alert.exists()).toBe(true);
      expect(alert.text()).toBe("commandExec.jobName.blank.message");
    });

    it("clears validation error when job ref save succeeds after failure", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "job.reference",
          id: "test-jobref-1",
          nodeStep: true,
          jobref: {
            name: "",
            uuid: "",
            group: "",
            project: "testProject",
            args: "",
            nodeStep: true,
          },
        },
        serviceName: "WorkflowNodeStep",
      });

      // First save fails
      (wrapper.vm as any).handleSave();
      await wrapper.vm.$nextTick();
      expect((wrapper.vm as any).validationError).toBeTruthy();

      // Fix the name
      (wrapper.vm as any).editModel.jobref.name = "Fixed Job";
      await wrapper.vm.$nextTick();

      // Save again
      (wrapper.vm as any).handleSave();
      await wrapper.vm.$nextTick();
      expect((wrapper.vm as any).validationError).toBe("");
      expect((wrapper.vm as any).showRequired).toBe(false);
      expect(wrapper.emitted("save")).toBeTruthy();
    });
  });

  describe("Cancel flow", () => {
    it("emits cancel when handleCancel is called", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-1",
        },
        pluginDetails: mockPluginProvider,
      });

      (wrapper.vm as any).handleCancel();
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("cancel")).toBeTruthy();
      expect(wrapper.emitted("cancel")!.length).toBe(1);
    });

    it("does not emit save when cancel is called", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-1",
        },
        pluginDetails: mockPluginProvider,
      });

      (wrapper.vm as any).handleCancel();
      await wrapper.vm.$nextTick();

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
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-1",
        },
        pluginDetails: mockPluginProvider,
        validation: validationErrors,
      });

      const pluginConfigComp = wrapper.findComponent({ name: "pluginConfig" });
      expect(pluginConfigComp.exists()).toBe(true);
      expect(pluginConfigComp.props("validation")).toEqual(validationErrors);
    });
  });

  describe("Step description field", () => {
    it("initializes description from modelValue", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-1",
          description: "Initial description",
        },
        pluginDetails: mockPluginProvider,
      });

      expect((wrapper.vm as any).stepDescription).toBe("Initial description");
    });

    it("handles empty description gracefully", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-1",
        },
        pluginDetails: mockPluginProvider,
      });

      expect((wrapper.vm as any).stepDescription).toBe("");
    });

    it("updates stepDescription when input changes", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-1",
        },
        pluginDetails: mockPluginProvider,
      });

      const descInput = wrapper.find("input[data-testid='step-description']");
      await descInput.setValue("New description");

      expect((wrapper.vm as any).stepDescription).toBe("New description");
    });
  });

  describe("Model value watcher", () => {
    it("updates editModel when modelValue prop changes", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-1",
        },
        pluginDetails: mockPluginProvider,
      });

      expect((wrapper.vm as any).editModel.type).toBe("script-inline");

      await wrapper.setProps({
        modelValue: {
          type: "script-inline",
          config: { adhocLocalString: "new script" },
          nodeStep: true,
          id: "test-1",
        },
      });
      await flushPromises();

      expect((wrapper.vm as any).editModel.config.adhocLocalString).toBe(
        "new script",
      );
    });

    it("separates description from editModel for regular steps", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-1",
          description: "Step desc",
        },
        pluginDetails: mockPluginProvider,
      });

      expect((wrapper.vm as any).stepDescription).toBe("Step desc");
      expect((wrapper.vm as any).editModel.description).toBeUndefined();
    });

    it("merges with defaults for job reference model values", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "job.reference",
          id: "test-jobref-1",
          nodeStep: true,
          jobref: {
            name: "My Job",
            uuid: "",
          },
        },
        serviceName: "WorkflowNodeStep",
      });

      const jobref = (wrapper.vm as any).editModel.jobref;
      expect(jobref).toBeDefined();
      expect(jobref.name).toBe("My Job");
      expect(jobref.nodefilters).toBeDefined();
      expect(jobref.nodefilters.dispatch).toBeDefined();
    });
  });

  describe("Props", () => {
    it("accepts showNavigation prop", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-1",
        },
        pluginDetails: mockPluginProvider,
        showNavigation: true,
      });

      expect(wrapper.props("showNavigation")).toBe(true);
    });

    it("accepts extraAutocompleteVars prop", async () => {
      const vars = [{ name: "myVar", type: "option", title: "My Var" }];
      const wrapper = await createWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-1",
        },
        pluginDetails: mockPluginProvider,
        extraAutocompleteVars: vars,
      });

      expect(wrapper.props("extraAutocompleteVars")).toEqual(vars);
    });
  });

  describe("Footer buttons", () => {
    it("renders Save and Cancel buttons in the footer", async () => {
      const wrapper = await createWrapper({
        modelValue: {
          type: "script-inline",
          config: {},
          nodeStep: true,
          id: "test-1",
        },
        pluginDetails: mockPluginProvider,
      });

      const footer = wrapper.find(".p-card-footer");
      expect(footer.exists()).toBe(true);

      // shallowMount renders PtButton as stubs; verify they exist
      const buttons = footer.findAllComponents({ name: "PtButton" });
      expect(buttons.length).toBe(2);
    });
  });
});
