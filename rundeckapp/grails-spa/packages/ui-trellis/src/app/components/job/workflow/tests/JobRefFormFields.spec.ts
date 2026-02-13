import { shallowMount, flushPromises, VueWrapper } from "@vue/test-utils";

// --- Mock setup (before imports) ---

const mockEventBusOn = jest.fn();
const mockEventBusOff = jest.fn();
const mockEventBusEmit = jest.fn();
const mockSetSelectedFilter = jest.fn();
const mockFetchNodes = jest.fn();

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    rootStore: {
      projects: {
        projects: [
          { name: "testProject" },
          { name: "otherProject" },
        ],
        loaded: true,
        load: jest.fn(),
      },
    },
    projectName: "testProject",
    eventBus: {
      on: mockEventBusOn,
      off: mockEventBusOff,
      emit: mockEventBusEmit,
    },
    rdBase: "http://localhost:4440",
    data: null,
  })),
}));

jest.mock("@/library/stores/NodesStorePinia", () => ({
  useNodesStore: () => ({
    total: 5,
    nodeFilterStore: {
      filter: "",
      setSelectedFilter: mockSetSelectedFilter,
    },
    currentNodes: [
      { nodename: "node1", hostname: "host1" },
      { nodename: "node2", hostname: "host2" },
    ],
    lastCountFetched: 5,
    isResultsTruncated: false,
    fetchNodes: mockFetchNodes,
  }),
}));

jest.mock("@/library/components/utils/contextVariableUtils", () => ({
  getContextVariables: jest.fn().mockReturnValue([]),
  transformVariables: jest.fn().mockReturnValue([]),
}));

jest.mock("@/library/stores/contextVariables", () => ({
  ContextVariable: {},
}));

// --- Import component after mocks ---
import JobRefFormFields from "../JobRefFormFields.vue";

// Stubs for child components
const NodeFilterInputStub = {
  name: "NodeFilterInput",
  template: `<div class="node-filter-input-stub">
    <input :value="value" @input="$emit('update:value', $event.target.value)" />
    <button @click="$emit('filter', { filter: value })">Search</button>
  </div>`,
  props: ["value", "project", "filterFieldName", "filterFieldId"],
  emits: ["update:value", "filter"],
};

const NodeListEmbedStub = {
  name: "NodeListEmbed",
  template: `<div class="node-list-embed-stub">
    <div v-for="node in nodes" :key="node.nodename">{{ node.nodename }}</div>
    <slot />
  </div>`,
  props: ["nodes"],
};

const PtAutoCompleteStub = {
  name: "PtAutoComplete",
  template: `<input :value="modelValue" @input="$emit('update:modelValue', $event.target.value)" />`,
  props: ["modelValue", "suggestions", "readOnly", "placeholder"],
  emits: ["update:modelValue"],
};

const UiSocketStub = {
  name: "UiSocket",
  template: `<div class="ui-socket-stub"></div>`,
  props: ["section", "location", "socketData"],
};

const ModalStub = {
  name: "Modal",
  template: `<div class="modal-stub" v-if="modelValue"><slot /></div>`,
  props: ["modelValue", "title", "size"],
  emits: ["update:modelValue"],
};

describe("JobRefFormFields", () => {
  let wrapper: VueWrapper<any>;

  const createWrapper = (props = {}) => {
    return shallowMount(JobRefFormFields, {
      props: {
        modelValue: {
          nodeStep: false,
          name: "",
          uuid: "",
          project: "testProject",
          group: "",
          args: "",
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
        showValidation: false,
        extraAutocompleteVars: [],
        ...props,
      },
      global: {
        stubs: {
          NodeFilterInput: NodeFilterInputStub,
          NodeListEmbed: NodeListEmbedStub,
          PtAutoComplete: PtAutoCompleteStub,
          UiSocket: UiSocketStub,
          modal: ModalStub,
          Teleport: true, // Stub teleport to prevent body mutations during tests
        },
        mocks: {
          $t: (key: string, args?: any) => {
            if (args && Array.isArray(args)) return `${key}[${args.join(",")}]`;
            return key;
          },
        },
      },
    });
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
    }
  });

  describe("Component rendering", () => {
    it("renders the component", async () => {
      wrapper = createWrapper();
      await flushPromises();

      expect(wrapper.find(".jobref-form-fields").exists()).toBe(true);
    });

    it("renders job reference radio buttons", async () => {
      wrapper = createWrapper();
      await flushPromises();

      const nameRadio = wrapper.find("#useNameTrue");
      const uuidRadio = wrapper.find("#useNameFalse");

      expect(nameRadio.exists()).toBe(true);
      expect(uuidRadio.exists()).toBe(true);
    });

    it("renders project selector with projects", async () => {
      wrapper = createWrapper();
      await flushPromises();

      const projectSelect = wrapper.find("#jobProjectField");
      expect(projectSelect.exists()).toBe(true);

      const options = projectSelect.findAll("option");
      expect(options).toHaveLength(2);
      expect(options[0].text()).toContain("testProject");
      expect(options[1].text()).toBe("otherProject");
    });

    it("renders job name and group inputs", async () => {
      wrapper = createWrapper();
      await flushPromises();

      expect(wrapper.find('[data-testid="jobNameField"]').exists()).toBe(true);
      expect(wrapper.find('[data-testid="jobGroupField"]').exists()).toBe(true);
    });

    it("renders job uuid input", async () => {
      wrapper = createWrapper();
      await flushPromises();

      expect(wrapper.find('[data-testid="jobUuidField"]').exists()).toBe(true);
    });

    it("renders choose job button", async () => {
      wrapper = createWrapper();
      await flushPromises();

      const chooseBtn = wrapper.find(".act_choose_job");
      expect(chooseBtn.exists()).toBe(true);
      expect(chooseBtn.text()).toBe("choose.a.job...");
    });

    it("renders node step radio buttons", async () => {
      wrapper = createWrapper();
      await flushPromises();

      expect(wrapper.find("#jobNodeStepFieldTrue").exists()).toBe(true);
      expect(wrapper.find("#jobNodeStepFieldFalse").exists()).toBe(true);
    });

    it("renders checkboxes for job options", async () => {
      wrapper = createWrapper();
      await flushPromises();

      expect(wrapper.find("#importOptionsCheck").exists()).toBe(true);
      expect(wrapper.find("#ignoreNotificationsCheck").exists()).toBe(true);
      expect(wrapper.find("#failOnDisableCheck").exists()).toBe(true);
      expect(wrapper.find("#childNodesCheck").exists()).toBe(true);
    });
  });

  describe("Props handling", () => {
    it("displays modelValue data in fields", async () => {
      wrapper = createWrapper({
        modelValue: {
          nodeStep: true,
          name: "Test Job",
          uuid: "abc-123",
          project: "testProject",
          group: "test/group",
          args: "-flag value",
          failOnDisable: true,
          childNodes: true,
          importOptions: true,
          ignoreNotifications: true,
          nodefilters: {
            filter: "name: node.*",
            dispatch: {
              threadcount: 2,
              keepgoing: true,
              rankAttribute: "rank",
              rankOrder: "ascending",
              nodeIntersect: false,
            },
          },
        },
      });
      await flushPromises();

      const nameField = wrapper.find('[data-testid="jobNameField"]') as any;
      expect(nameField.element.value).toBe("Test Job");

      const groupField = wrapper.find('[data-testid="jobGroupField"]') as any;
      expect(groupField.element.value).toBe("test/group");

      const projectField = wrapper.find("#jobProjectField") as any;
      expect(projectField.element.value).toBe("testProject");

      const nodeStepTrue = wrapper.find("#jobNodeStepFieldTrue") as any;
      expect(nodeStepTrue.element.checked).toBe(true);
    });

    it("shows validation errors when showValidation is true", async () => {
      wrapper = createWrapper({
        showValidation: true,
        modelValue: {
          nodeStep: false,
          name: "",
          uuid: "",
          project: "testProject",
          group: "",
          args: "",
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
      });
      await flushPromises();

      wrapper.vm.isUseName = true;
      await wrapper.vm.$nextTick();

      const nameGroup = wrapper.find('[data-testid="jobNameField"]').element
        .closest(".form-group");
      expect(nameGroup?.classList.contains("has-error")).toBe(true);
    });
  });

  describe("Direct modelValue mutation", () => {
    it("mutates modelValue.name directly", async () => {
      const modelValue = {
        nodeStep: false,
        name: "Original",
        uuid: "",
        project: "testProject",
        group: "",
        args: "",
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
      };

      wrapper = createWrapper({ modelValue });
      await flushPromises();

      const nameField = wrapper.find('[data-testid="jobNameField"]');
      await nameField.setValue("Updated Job");

      // Direct mutation - modelValue is mutated in place
      expect(modelValue.name).toBe("Updated Job");
    });

    it("mutates modelValue.project directly", async () => {
      const modelValue = {
        nodeStep: false,
        name: "",
        uuid: "",
        project: "testProject",
        group: "",
        args: "",
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
      };

      wrapper = createWrapper({ modelValue });
      await flushPromises();

      const projectSelect = wrapper.find("#jobProjectField");
      await projectSelect.setValue("otherProject");

      expect(modelValue.project).toBe("otherProject");
    });

    it("mutates modelValue.nodeStep directly", async () => {
      const modelValue = {
        nodeStep: false,
        name: "",
        uuid: "",
        project: "testProject",
        group: "",
        args: "",
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
      };

      wrapper = createWrapper({ modelValue });
      await flushPromises();

      const nodeStepTrue = wrapper.find("#jobNodeStepFieldTrue");
      await nodeStepTrue.setValue(true);

      expect(modelValue.nodeStep).toBe(true);
    });
  });

  describe("Job browser modal", () => {
    it("opens job browser modal when choose button is clicked", async () => {
      wrapper = createWrapper();
      await flushPromises();

      expect((wrapper.vm as any).openJobSelectionModal).toBe(false);

      const chooseBtn = wrapper.find(".act_choose_job");
      await chooseBtn.trigger("click");

      expect((wrapper.vm as any).openJobSelectionModal).toBe(true);
    });

    it("registers event bus listeners on mount", async () => {
      wrapper = createWrapper();
      await flushPromises();

      expect(mockEventBusOn).toHaveBeenCalledWith(
        "browser-job-item-selection",
        expect.any(Function),
      );
      expect(mockEventBusOn).toHaveBeenCalledWith(
        "browser-jobs-empty",
        expect.any(Function),
      );
    });

    it("unregisters event bus listeners on unmount", async () => {
      wrapper = createWrapper();
      await flushPromises();

      wrapper.unmount();

      expect(mockEventBusOff).toHaveBeenCalledWith(
        "browser-job-item-selection",
        expect.any(Function),
      );
      expect(mockEventBusOff).toHaveBeenCalledWith(
        "browser-jobs-empty",
        expect.any(Function),
      );
    });

    it("updates job selection when browser emits selection", async () => {
      const modelValue = {
        nodeStep: false,
        name: "",
        uuid: "",
        project: "testProject",
        group: "",
        args: "",
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
      };

      wrapper = createWrapper({ modelValue });
      await flushPromises();

      const job = {
        id: "job-uuid-123",
        jobName: "Selected Job",
        groupPath: "my/group",
      };

      (wrapper.vm as any).updateJobSelection(job);

      expect(modelValue.uuid).toBe("job-uuid-123");
      expect(modelValue.name).toBe("Selected Job");
      expect(modelValue.group).toBe("my/group");
      expect((wrapper.vm as any).openJobSelectionModal).toBe(false);
    });
  });

  describe("Node filter functionality", () => {
    it("expands node filter section when clicking expand button", async () => {
      wrapper = createWrapper();
      await flushPromises();

      expect((wrapper.vm as any).nodeFilterOverrideExpanded).toBeFalsy();

      const expandBtn = wrapper.find('[data-testid="expandNodeFilterBtn"]');
      await expandBtn.trigger("click");

      expect((wrapper.vm as any).nodeFilterOverrideExpanded).toBe(true);
    });

    it("renders node filter section when expanded", async () => {
      wrapper = createWrapper();
      await flushPromises();

      (wrapper.vm as any).nodeFilterOverrideExpanded = true;
      await wrapper.vm.$nextTick();

      const nodeFilterContainer = wrapper.find(
        '[data-testid="nodeFilterContainer"]',
      );
      expect(nodeFilterContainer.classes()).toContain("in");
    });

    it("displays matched nodes count when filter is set", async () => {
      wrapper = createWrapper({
        modelValue: {
          nodeStep: false,
          name: "",
          uuid: "",
          project: "testProject",
          group: "",
          args: "",
          failOnDisable: false,
          childNodes: false,
          importOptions: false,
          ignoreNotifications: false,
          nodefilters: {
            filter: "name: node.*",
            dispatch: {
              threadcount: null,
              keepgoing: null,
              rankAttribute: null,
              rankOrder: null,
              nodeIntersect: null,
            },
          },
        },
      });
      await flushPromises();

      (wrapper.vm as any).filterLoaded = true;
      (wrapper.vm as any).nodeFilterOverrideExpanded = true;
      await wrapper.vm.$nextTick();

      expect((wrapper.vm as any).hasFilter).toBe(true);
      expect((wrapper.vm as any).total).toBe(5);
    });

    it("calls fetchNodes when triggerFetchNodes is called with a filter", async () => {
      wrapper = createWrapper({
        modelValue: {
          nodeStep: false,
          name: "",
          uuid: "",
          project: "testProject",
          group: "",
          args: "",
          failOnDisable: false,
          childNodes: false,
          importOptions: false,
          ignoreNotifications: false,
          nodefilters: {
            filter: "name: node.*",
            dispatch: {
              threadcount: null,
              keepgoing: null,
              rankAttribute: null,
              rankOrder: null,
              nodeIntersect: null,
            },
          },
        },
      });
      await flushPromises();

      await (wrapper.vm as any).triggerFetchNodes();

      expect(mockFetchNodes).toHaveBeenCalled();
    });

    it("updates node filter when NodeFilterInput emits update", async () => {
      const modelValue = {
        nodeStep: false,
        name: "",
        uuid: "",
        project: "testProject",
        group: "",
        args: "",
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
      };

      wrapper = createWrapper({ modelValue });
      await flushPromises();

      (wrapper.vm as any).updatedValue("name: updated.*");

      expect(mockSetSelectedFilter).toHaveBeenCalledWith("name: updated.*");
    });
  });

  describe("Lifecycle and initialization", () => {
    it("initializes with expanded node filter if filter exists", async () => {
      wrapper = createWrapper({
        modelValue: {
          nodeStep: false,
          name: "",
          uuid: "",
          project: "testProject",
          group: "",
          args: "",
          failOnDisable: false,
          childNodes: false,
          importOptions: false,
          ignoreNotifications: false,
          nodefilters: {
            filter: "name: node.*",
            dispatch: {
              threadcount: null,
              keepgoing: null,
              rankAttribute: null,
              rankOrder: null,
              nodeIntersect: null,
            },
          },
        },
      });
      await flushPromises();

      expect((wrapper.vm as any).nodeFilterOverrideExpanded).toBe(true);
    });

    it("initializes with expanded node filter if nodeIntersect is set", async () => {
      wrapper = createWrapper({
        modelValue: {
          nodeStep: false,
          name: "",
          uuid: "",
          project: "testProject",
          group: "",
          args: "",
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
              nodeIntersect: true,
            },
          },
        },
      });
      await flushPromises();

      expect((wrapper.vm as any).nodeFilterOverrideExpanded).toBe(true);
    });
  });

  describe("Use name vs UUID toggle", () => {
    it("defaults to using name", async () => {
      wrapper = createWrapper();
      await flushPromises();

      expect((wrapper.vm as any).isUseName).toBe(false);
    });

    it("disables name/group inputs when using UUID", async () => {
      wrapper = createWrapper();
      await flushPromises();

      (wrapper.vm as any).isUseName = false;
      await wrapper.vm.$nextTick();

      const nameField = wrapper.find('[data-testid="jobNameField"]');
      const groupField = wrapper.find('[data-testid="jobGroupField"]');

      expect((nameField.element as HTMLInputElement).readOnly).toBe(true);
      expect((groupField.element as HTMLInputElement).readOnly).toBe(true);
    });

    it("enables name/group inputs when using name", async () => {
      wrapper = createWrapper();
      await flushPromises();

      (wrapper.vm as any).isUseName = true;
      await wrapper.vm.$nextTick();

      const nameField = wrapper.find('[data-testid="jobNameField"]');
      const groupField = wrapper.find('[data-testid="jobGroupField"]');

      expect((nameField.element as HTMLInputElement).readOnly).toBe(false);
      expect((groupField.element as HTMLInputElement).readOnly).toBe(false);
    });
  });
});
