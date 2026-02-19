import { shallowMount, flushPromises } from "@vue/test-utils";
import { createTestingPinia } from "@pinia/testing";
import ChoosePluginsEAModal from "../ChoosePluginsEAModal.vue";

// Mirror the real enum values — avoids the unresolvable @/library/stores/Plugins alias in test files
const ServiceType = {
  WorkflowStep: "WorkflowStep",
  WorkflowNodeStep: "WorkflowNodeStep",
} as const;

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rootStore: {
      plugins: {
        load: jest.fn().mockResolvedValue(undefined),
        getServicePlugins: jest.fn().mockReturnValue([]),
      },
    },
  }),
}));

jest.mock("@/library/modules/rundeckClient", () => ({
  client: jest.fn(),
}));

// Type-safe reference to the mocked service (RULE-010)
interface MockPlugins {
  load: jest.Mock;
  getServicePlugins: jest.Mock;
}
interface MockRundeckService {
  getRundeckContext: jest.MockedFunction<() => { rootStore: { plugins: MockPlugins } }>;
}
const { getRundeckContext } = jest.requireMock(
  "@/library/rundeckService",
) as MockRundeckService;
const mockGetServicePlugins = getRundeckContext().rootStore.plugins.getServicePlugins;
const mockLoad = getRundeckContext().rootStore.plugins.load;

// makeProvider is small (< 10 lines, single object) — inline is appropriate (RULE-009)
const makeProvider = (overrides: Record<string, any> = {}) => ({
  name: "provider-a",
  title: "Provider A",
  description: "A description",
  isHighlighted: false,
  ...overrides,
});

interface MountOptions {
  props?: Record<string, any>;
}

// Default selected service is WorkflowNodeStep — configure providers for that service
// (second getServicePlugins call) unless preventServiceSwap locks to WorkflowStep
const createWrapper = async (options: MountOptions = {}) => {
  const wrapper = shallowMount(ChoosePluginsEAModal, {
    props: {
      title: "Choose a Step",
      services: [ServiceType.WorkflowStep, ServiceType.WorkflowNodeStep],
      modelValue: true,
      showSearch: true,
      ...options.props,
    },
    global: {
      plugins: [createTestingPinia()],
      stubs: {
        // Modal is globally registered but needs slot passthrough for testing
        Modal: { template: "<div><slot/><slot name='footer'/></div>" },
        PtSelectButton: true,
        PluginSearch: true,
        PluginAccordionList: true,
        GroupedProviderDetail: true,
        PtButton: true,
      },
    },
  });
  await flushPromises();
  return wrapper;
};

describe("ChoosePluginsEAModal", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Restore defaults — clearAllMocks resets calls but not implementations
    mockLoad.mockResolvedValue(undefined);
    mockGetServicePlugins.mockReturnValue([]);
  });

  describe("provider visibility", () => {
    it("passes loading=true to PluginAccordionList while providers are being fetched", async () => {
      // Override load to never resolve so the component stays mid-mount
      mockLoad.mockImplementation(() => new Promise(() => {}));
      // Mount directly — no flushPromises so mounted() stays suspended at the first load() await
      const wrapper = shallowMount(ChoosePluginsEAModal, {
        props: {
          title: "Choose a Step",
          services: [ServiceType.WorkflowStep, ServiceType.WorkflowNodeStep],
          modelValue: true,
          showSearch: true,
        },
        global: {
          plugins: [createTestingPinia()],
          stubs: {
            Modal: { template: "<div><slot/><slot name='footer'/></div>" },
            PtSelectButton: true,
            PluginSearch: true,
            PluginAccordionList: true,
            GroupedProviderDetail: true,
            PtButton: true,
          },
        },
      });
      await wrapper.vm.$nextTick();

      expect(
        wrapper.findComponent({ name: "PluginAccordionList" }).props("loading"),
      ).toBe(true);
    });

    it("passes loading=false to PluginAccordionList after providers have loaded", async () => {
      const wrapper = await createWrapper();

      expect(
        wrapper.findComponent({ name: "PluginAccordionList" }).props("loading"),
      ).toBe(false);
    });

    it("shows the accordion list when providers are available", async () => {
      mockGetServicePlugins
        .mockReturnValueOnce([]) // WorkflowStep
        .mockReturnValueOnce([makeProvider()]); // WorkflowNodeStep (default selected)
      const wrapper = await createWrapper();

      expect(wrapper.findComponent({ name: "PluginAccordionList" }).exists()).toBe(true);
      expect(wrapper.find('[data-testid="no-matches-found"]').exists()).toBe(false);
    });

    it("hides the accordion and shows the no-results message when search matches nothing", async () => {
      mockGetServicePlugins
        .mockReturnValueOnce([])
        .mockReturnValueOnce([makeProvider({ title: "Command Step" })]);
      const wrapper = await createWrapper();

      await wrapper.findComponent({ name: "PluginSearch" }).vm.$emit("search", "script");
      await wrapper.vm.$nextTick();

      expect(wrapper.findComponent({ name: "PluginAccordionList" }).exists()).toBe(false);
      expect(wrapper.find('[data-testid="no-matches-found"]').exists()).toBe(true);
    });

    it("shows the accordion again and hides no-results when the search is cleared", async () => {
      mockGetServicePlugins
        .mockReturnValueOnce([])
        .mockReturnValueOnce([makeProvider({ title: "Command Step" })]);
      const wrapper = await createWrapper();

      await wrapper.findComponent({ name: "PluginSearch" }).vm.$emit("search", "script");
      await wrapper.vm.$nextTick();
      await wrapper.findComponent({ name: "PluginSearch" }).vm.$emit("search", "");
      await wrapper.vm.$nextTick();

      expect(wrapper.findComponent({ name: "PluginAccordionList" }).exists()).toBe(true);
      expect(wrapper.find('[data-testid="no-matches-found"]').exists()).toBe(false);
    });

    it("passes only matching providers to PluginAccordionList when searching", async () => {
      mockGetServicePlugins
        .mockReturnValueOnce([])
        .mockReturnValueOnce([
          makeProvider({ name: "script", title: "Script Step" }),
          makeProvider({ name: "cmd", title: "Command Step" }),
        ]);
      const wrapper = await createWrapper();

      await wrapper.findComponent({ name: "PluginSearch" }).vm.$emit("search", "script");
      await wrapper.vm.$nextTick();

      const { highlighted, nonHighlighted } = wrapper
        .findComponent({ name: "PluginAccordionList" })
        .props("groupedProviders");
      const allTitles = [...Object.keys(highlighted), ...Object.keys(nonHighlighted)];
      expect(allTitles).toContain("Script Step");
      expect(allTitles).not.toContain("Command Step");
    });
  });

  describe("provider grouping", () => {
    it("passes highlighted providers as individual non-group entries", async () => {
      mockGetServicePlugins
        .mockReturnValueOnce([])
        .mockReturnValueOnce([makeProvider({ title: "Command", isHighlighted: true })]);
      const wrapper = await createWrapper();

      const { highlighted, nonHighlighted } = wrapper
        .findComponent({ name: "PluginAccordionList" })
        .props("groupedProviders");
      expect(highlighted["Command"].isGroup).toBe(false);
      expect(highlighted["Command"].providers).toHaveLength(1);
      expect(Object.keys(nonHighlighted)).toHaveLength(0);
    });

    it("groups non-highlighted providers sharing a groupBy key into a single accordion entry", async () => {
      mockGetServicePlugins
        .mockReturnValueOnce([])
        .mockReturnValueOnce([
          makeProvider({ name: "aws-copy", title: "AWS Copy", providerMetadata: { groupBy: "AWS" } }),
          makeProvider({ name: "aws-run", title: "AWS Run", providerMetadata: { groupBy: "AWS" } }),
        ]);
      const wrapper = await createWrapper();

      const { nonHighlighted } = wrapper
        .findComponent({ name: "PluginAccordionList" })
        .props("groupedProviders");
      expect(nonHighlighted["AWS"].isGroup).toBe(true);
      expect(nonHighlighted["AWS"].providers).toHaveLength(2);
    });

    it("breaks groups apart and renders providers individually when a search is active", async () => {
      mockGetServicePlugins
        .mockReturnValueOnce([])
        .mockReturnValueOnce([
          makeProvider({ name: "aws-copy", title: "AWS Copy", providerMetadata: { groupBy: "AWS" } }),
          makeProvider({ name: "aws-run", title: "AWS Run", providerMetadata: { groupBy: "AWS" } }),
        ]);
      const wrapper = await createWrapper();

      await wrapper.findComponent({ name: "PluginSearch" }).vm.$emit("search", "aws");
      await wrapper.vm.$nextTick();

      const { nonHighlighted } = wrapper
        .findComponent({ name: "PluginAccordionList" })
        .props("groupedProviders");
      expect(nonHighlighted["AWS"]).toBeUndefined();
      expect(nonHighlighted["AWS Copy"].isGroup).toBe(false);
      expect(nonHighlighted["AWS Run"].isGroup).toBe(false);
    });
  });

  describe("service filtering", () => {
    it("hides the service tab switcher when preventServiceSwap is true", async () => {
      const wrapper = await createWrapper({
        props: { preventServiceSwap: true, targetService: ServiceType.WorkflowNodeStep },
      });

      expect(wrapper.findComponent({ name: "PtSelectButton" }).exists()).toBe(false);
    });

    it("shows the service tab switcher when preventServiceSwap is false", async () => {
      const wrapper = await createWrapper({ props: { preventServiceSwap: false } });

      expect(wrapper.findComponent({ name: "PtSelectButton" }).exists()).toBe(true);
    });

    it("excludes providers listed in excludeProviders from the accordion", async () => {
      mockGetServicePlugins
        .mockReturnValueOnce([])
        .mockReturnValueOnce([
          makeProvider({ name: "conditional.logic", title: "Conditional" }),
          makeProvider({ name: "script", title: "Script Step" }),
        ]);
      const wrapper = await createWrapper({ props: { excludeProviders: ["conditional.logic"] } });

      const { highlighted, nonHighlighted } = wrapper
        .findComponent({ name: "PluginAccordionList" })
        .props("groupedProviders");
      const allTitles = [...Object.keys(highlighted), ...Object.keys(nonHighlighted)];
      expect(allTitles).not.toContain("Conditional");
      expect(allTitles).toContain("Script Step");
    });
  });

  describe("group navigation", () => {
    it("shows GroupedProviderDetail and hides the accordion when a group is selected", async () => {
      mockGetServicePlugins
        .mockReturnValueOnce([])
        .mockReturnValueOnce([
          makeProvider({ name: "aws-copy", title: "AWS Copy", providerMetadata: { groupBy: "AWS" } }),
        ]);
      const wrapper = await createWrapper();

      await wrapper.findComponent({ name: "PluginAccordionList" }).vm.$emit("select", {
        group: { isGroup: true, providers: [makeProvider()], iconDetail: {} },
        key: "AWS",
      });
      await wrapper.vm.$nextTick();

      expect(wrapper.findComponent({ name: "GroupedProviderDetail" }).exists()).toBe(true);
      expect(wrapper.findComponent({ name: "PluginAccordionList" }).exists()).toBe(false);
    });

    it("emits 'selected' with the correct service and provider when a leaf provider is chosen", async () => {
      mockGetServicePlugins
        .mockReturnValueOnce([])
        .mockReturnValueOnce([makeProvider({ name: "script-step", title: "Script Step" })]);
      const wrapper = await createWrapper();

      await wrapper.findComponent({ name: "PluginAccordionList" }).vm.$emit("select", {
        group: { isGroup: false, providers: [makeProvider({ name: "script-step" })] },
        key: "Script Step",
      });

      expect(wrapper.emitted()).toHaveProperty("selected");
      expect(wrapper.emitted("selected")).toHaveLength(1);
      expect(wrapper.emitted("selected")[0][0]).toEqual({
        service: ServiceType.WorkflowNodeStep,
        provider: "script-step",
      });
    });

    it("passes the correct group name and provider list to GroupedProviderDetail", async () => {
      mockGetServicePlugins
        .mockReturnValueOnce([])
        .mockReturnValueOnce([
          makeProvider({ name: "aws-copy", title: "AWS Copy", providerMetadata: { groupBy: "AWS" } }),
        ]);
      const wrapper = await createWrapper();

      await wrapper.findComponent({ name: "PluginAccordionList" }).vm.$emit("select", {
        group: { isGroup: true, providers: [makeProvider()], iconDetail: {} },
        key: "AWS",
      });
      await wrapper.vm.$nextTick();

      const detail = wrapper.findComponent({ name: "GroupedProviderDetail" });
      expect(detail.props("groupName")).toBe("AWS");
      // currentGroupForService resolves the group from loadedServices — verify the real provider is there
      expect(detail.props("group").isGroup).toBe(true);
      expect(detail.props("group").providers[0].name).toBe("aws-copy");
    });

    it("returns to the accordion view when the back button is triggered in GroupedProviderDetail", async () => {
      mockGetServicePlugins
        .mockReturnValueOnce([])
        .mockReturnValueOnce([makeProvider({ providerMetadata: { groupBy: "AWS" } })]);
      const wrapper = await createWrapper();

      await wrapper.findComponent({ name: "PluginAccordionList" }).vm.$emit("select", {
        group: { isGroup: true, providers: [makeProvider()], iconDetail: {} },
        key: "AWS",
      });
      await wrapper.vm.$nextTick();
      expect(wrapper.findComponent({ name: "GroupedProviderDetail" }).exists()).toBe(true);

      await wrapper.findComponent({ name: "GroupedProviderDetail" }).vm.$emit("back");
      await wrapper.vm.$nextTick();

      expect(wrapper.findComponent({ name: "PluginAccordionList" }).exists()).toBe(true);
      expect(wrapper.findComponent({ name: "GroupedProviderDetail" }).exists()).toBe(false);
    });
  });

  describe("modal lifecycle", () => {
    it("resets group view when the modal is closed and reopened", async () => {
      const wrapper = await createWrapper({ props: { modelValue: true } });

      await wrapper.findComponent({ name: "PluginAccordionList" }).vm.$emit("select", {
        group: { isGroup: true, providers: [makeProvider()], iconDetail: {} },
        key: "AWS",
      });
      await wrapper.vm.$nextTick();
      expect(wrapper.findComponent({ name: "GroupedProviderDetail" }).exists()).toBe(true);

      await wrapper.setProps({ modelValue: false });
      await wrapper.setProps({ modelValue: true });
      await wrapper.vm.$nextTick();

      // After reset: showGroup=false, searchQuery="" → accordion renders (hasSearchQuery=false covers hasNoResults)
      expect(wrapper.findComponent({ name: "GroupedProviderDetail" }).exists()).toBe(false);
      expect(wrapper.findComponent({ name: "PluginAccordionList" }).exists()).toBe(true);
    });

    it("exits group view when searching and the query does not match providers in the current group", async () => {
      // Need a top-level provider matching "script" so the accordion can render after exit
      mockGetServicePlugins
        .mockReturnValueOnce([])
        .mockReturnValueOnce([makeProvider({ title: "Script Step" })]);
      const wrapper = await createWrapper();

      await wrapper.findComponent({ name: "PluginAccordionList" }).vm.$emit("select", {
        group: { isGroup: true, providers: [makeProvider({ title: "AWS Copy" })], iconDetail: {} },
        key: "AWS",
      });
      await wrapper.vm.$nextTick();

      await wrapper.findComponent({ name: "PluginSearch" }).vm.$emit("search", "script");
      await wrapper.vm.$nextTick();

      expect(wrapper.findComponent({ name: "PluginAccordionList" }).exists()).toBe(true);
      expect(wrapper.findComponent({ name: "GroupedProviderDetail" }).exists()).toBe(false);
    });

    it("stays in group view when the search still matches providers in the current group", async () => {
      mockGetServicePlugins
        .mockReturnValueOnce([])
        .mockReturnValueOnce([makeProvider({ title: "Script Step" })]);
      const wrapper = await createWrapper();

      await wrapper.findComponent({ name: "PluginAccordionList" }).vm.$emit("select", {
        group: { isGroup: true, providers: [makeProvider({ title: "Script Step" })], iconDetail: {} },
        key: "Scripts",
      });
      await wrapper.vm.$nextTick();

      await wrapper.findComponent({ name: "PluginSearch" }).vm.$emit("search", "script");
      await wrapper.vm.$nextTick();

      expect(wrapper.findComponent({ name: "GroupedProviderDetail" }).exists()).toBe(true);
    });
  });

  describe("cancel button", () => {
    it("emits 'cancel' when the cancel button is clicked", async () => {
      const wrapper = await createWrapper();

      await wrapper.find('[data-testid="cancel-button"]').trigger("click");

      // cancel carries no payload by design — emission itself is the full contract
      expect(wrapper.emitted()).toHaveProperty("cancel");
      expect(wrapper.emitted("cancel")).toHaveLength(1);
    });
  });
});
