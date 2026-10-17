import { vi } from "vitest";
import { shallowMount, flushPromises } from "@vue/test-utils";
import LogFilterControls from "../LogFilterControls.vue";
import EditPluginModal from "../../../../../library/components/plugins/EditPluginModal.vue";

vi.mock("@/library/modules/pluginService", () => ({
  validatePluginConfig: vi.fn()
    .mockResolvedValue({ valid: true, errors: {} }),
  getPluginProvidersForService: vi.fn()
    .mockResolvedValue({ service: "LogFilter", descriptions: [], labels: {} }),
}));

vi.mock("@/library/modules/rundeckClient", () => ({ client: vi.fn() }));

vi.mock("@/library/rundeckService", () => ({
  getRundeckContext: vi.fn().mockReturnValue({
    rootStore: {
      plugins: {
        getServicePlugins: vi.fn().mockReturnValue([]),
        load: vi.fn(),
      },
    },
    projectName: "testProject",
    eventBus: { on: vi.fn(), off: vi.fn(), emit: vi.fn() },
    rdBase: "http://localhost:4440",
    apiVersion: 46,
  }),
}));

vi.mock("@/library/services/projects");

vi.mock("@/library/stores/Plugins", () => ({
  ServiceType: { LogFilter: "LogFilter" },
}));

vi.mock("../stepEditorUtils", () => ({
  resetValidation: vi.fn().mockReturnValue({ errors: {}, valid: true }),
}));

import { resetValidation } from "../stepEditorUtils";
import { validatePluginConfig } from "../../../../../library/modules/pluginService";

const createMockEventBus = () => ({
  on: vi.fn(),
  off: vi.fn(),
  emit: vi.fn(),
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
    global: { stubs: { teleport: true } },
  });
  await flushPromises();
  return wrapper;
};

describe("LogFilterControls", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (resetValidation as vi.Mock).mockReturnValue({ errors: {}, valid: true });
  });

  describe("add filter button", () => {
    it("renders the add filter button when showButton is true", async () => {
      const wrapper = await createWrapper({ showButton: true });

      expect(wrapper.find('[data-testid="add-filter-button"]').exists()).toBe(
        true,
      );
    });

    it("hides the add filter button when showButton is false", async () => {
      const wrapper = await createWrapper({ showButton: false });

      expect(wrapper.find('[data-testid="add-filter-button"]').exists()).toBe(
        false,
      );
    });

    it("does not render the add filter modal until the button is clicked", async () => {
      const wrapper = await createWrapper();

      // ChoosePluginModal has v-if="addFilterModal" — not shown on initial render
      expect(wrapper.find('[data-testid="add-filter-modal"]').exists()).toBe(
        false,
      );
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

      const result = (resetValidation as vi.Mock).mock.results[0]?.value;
      expect(result).toEqual({ errors: {}, valid: true });
      expect(Array.isArray(result.errors)).toBe(false);
    });
  });

  describe("saving a filter (RUN-5055)", () => {
    const createEditWrapper = () =>
      createWrapper({
        modelValue: { type: "key-value-data", config: { regex: "a" } },
      });

    it("validates ignoring Project/Framework scoped properties", async () => {
      const wrapper = await createEditWrapper();

      wrapper.findComponent(EditPluginModal).vm.$emit("save");
      await flushPromises();
      await wrapper.vm.$nextTick();

      expect(validatePluginConfig).toHaveBeenCalledWith(
        "LogFilter",
        "key-value-data",
        { regex: "a" },
        "Project",
      );
    });

    it("emits update:modelValue with the filter when validation passes", async () => {
      const wrapper = await createEditWrapper();

      wrapper.findComponent(EditPluginModal).vm.$emit("save");
      await flushPromises();
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:modelValue")).toHaveLength(1);
      expect(wrapper.emitted("update:modelValue")![0]).toEqual([
        { type: "key-value-data", config: { regex: "a" } },
      ]);
    });

    it("does not emit and passes errors to the modal when validation fails", async () => {
      vi.mocked(validatePluginConfig).mockResolvedValueOnce({
        valid: false,
        errors: { regex: "required" },
      });
      const wrapper = await createEditWrapper();

      wrapper.findComponent(EditPluginModal).vm.$emit("save");
      await flushPromises();
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:modelValue")).toBeUndefined();
      expect(
        wrapper.findComponent(EditPluginModal).props("validation"),
      ).toEqual({ valid: false, errors: { regex: "required" } });
    });
  });
});
