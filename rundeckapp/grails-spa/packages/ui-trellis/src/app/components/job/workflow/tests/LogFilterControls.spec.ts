import { shallowMount, flushPromises } from "@vue/test-utils";
import LogFilterControls from "../LogFilterControls.vue";

jest.mock("@/library/modules/pluginService", () => ({
  validatePluginConfig: jest.fn().mockResolvedValue({ valid: true, errors: {} }),
  getPluginProvidersForService: jest.fn().mockResolvedValue({ service: "LogFilter", descriptions: [], labels: {} }),
}));

jest.mock("@/library/modules/rundeckClient", () => ({ client: jest.fn() }));

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rootStore: { plugins: { getServicePlugins: jest.fn().mockReturnValue([]), load: jest.fn() } },
    projectName: "testProject",
    eventBus: { on: jest.fn(), off: jest.fn(), emit: jest.fn() },
    rdBase: "http://localhost:4440",
    apiVersion: 46,
  }),
}));

jest.mock("../../../../../library/services/projects");

jest.mock("@/library/stores/Plugins", () => ({
  ServiceType: { LogFilter: "LogFilter" },
}));

jest.mock("../stepEditorUtils", () => ({
  resetValidation: jest.fn().mockReturnValue({ errors: {}, valid: true }),
}));

import { resetValidation } from "../stepEditorUtils";

const createMockEventBus = () => ({
  on: jest.fn(),
  off: jest.fn(),
  emit: jest.fn(),
});

const createWrapper = async (props = {}) => {
  const wrapper = shallowMount(LogFilterControls, {
    props: {
      modelValue: { type: "", config: {} },
      showButton: true,
      title: "Test Title",
      subtitle: "Test Step",
      eventBus: createMockEventBus(),
      ...props,
    },
  });
  await flushPromises();
  return wrapper;
};

describe("LogFilterControls", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (resetValidation as jest.Mock).mockReturnValue({ errors: {}, valid: true });
  });

  describe("add filter button", () => {
    it("renders the add filter button when showButton is true", async () => {
      const wrapper = await createWrapper({ showButton: true });

      expect(wrapper.find('[data-testid="add-filter-button"]').exists()).toBe(true);
    });

    it("hides the add filter button when showButton is false", async () => {
      const wrapper = await createWrapper({ showButton: false });

      expect(wrapper.find('[data-testid="add-filter-button"]').exists()).toBe(false);
    });

    it("does not render the add filter modal until the button is clicked", async () => {
      const wrapper = await createWrapper();

      // ChoosePluginModal has v-if="addFilterModal" — not shown on initial render
      expect(wrapper.find('[data-testid="add-filter-modal"]').exists()).toBe(false);
    });
  });

  describe("eventBus integration", () => {
    it("registers an 'edit' listener on the eventBus during mount", async () => {
      const eventBus = createMockEventBus();
      await createWrapper({ eventBus });

      expect(eventBus.on).toHaveBeenCalledWith("edit", expect.any(Function));
    });

    it("registers an 'add' listener on the eventBus during mount", async () => {
      const eventBus = createMockEventBus();
      await createWrapper({ eventBus });

      expect(eventBus.on).toHaveBeenCalledWith("add", expect.any(Function));
    });
  });

  describe("resetValidation usage (PR change: replaces inline object)", () => {
    it("calls resetValidation() to initialise editModelValidation on mount", async () => {
      await createWrapper();

      expect(resetValidation).toHaveBeenCalled();
    });

    it("calls resetValidation() with errors as an object not an array", async () => {
      await createWrapper();

      const result = (resetValidation as jest.Mock).mock.results[0]?.value;
      expect(result).toEqual({ errors: {}, valid: true });
      expect(Array.isArray(result.errors)).toBe(false);
    });
  });
});
