import { shallowMount, flushPromises } from "@vue/test-utils";
import LogFilters from "../LogFilters.vue";
import LogFilterButton from "@/app/components/job/workflow/LogFilterButton.vue";
import LogFilterControls from "../LogFilterControls.vue";
import { getRundeckContext } from "../../../../../library";

jest.mock("@/library/modules/rundeckClient", () => ({
  client: jest.fn(),
}));

jest.mock("@/library/rundeckService", () => {
  const on = jest.fn();
  const off = jest.fn();
  const emit = jest.fn();
  return {
    getRundeckContext: jest.fn().mockReturnValue({
      eventBus: { on, off, emit },
      rdBase: "http://localhost:4440/",
      projectName: "testProject",
      apiVersion: "44",
    }),
  };
});

jest.mock("../../../../../library/services/projects");

jest.mock("@/library/modules/pluginService", () => ({
  getPluginProvidersForService: jest.fn().mockResolvedValue({
    service: true,
    descriptions: [{ name: "filterType" }],
    labels: ["Filter Type"],
  }),
}));

jest.mock("../stepEditorUtils", () => ({
  resetValidation: jest.fn().mockReturnValue({ errors: {}, valid: true }),
}));

import { resetValidation } from "../stepEditorUtils";

const getMockEventBus = () => getRundeckContext().eventBus as any;

const createWrapper = async (props: Record<string, any> = {}) => {
  const wrapper = shallowMount(LogFilters, {
    props: {
      modelValue: [],
      title: "Log Filters",
      subtitle: "Manage your log filters",
      conditionalEnabled: false,
      editEvent: undefined,
      ...props,
    },
    global: {
      stubs: {
        LogFilterButton: false,
      },
    },
  });
  await flushPromises();
  return wrapper;
};

describe("LogFilters", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (resetValidation as jest.MockedFunction<typeof resetValidation>).mockReturnValue(
      { errors: {}, valid: true },
    );
  });

  describe("title rendering", () => {
    it("renders the title when showIfEmpty is true or model is not empty", async () => {
      const wrapper = await createWrapper({ showIfEmpty: true });
      expect(
        wrapper.find('[data-testid="log-filters-container"]').text(),
      ).toContain("Log Filters");

      const wrapperWithModel = await createWrapper({
        modelValue: [{ type: "filterType" }],
      });
      expect(
        wrapperWithModel.find('[data-testid="log-filters-container"]').text(),
      ).toContain("Log Filters");
    });
  });

  describe("LogFilterButton rendering", () => {
    it("renders LogFilterButton components for each model entry", async () => {
      const wrapper = await createWrapper({
        modelValue: [{ type: "filterType" }],
      });
      expect(wrapper.findAllComponents(LogFilterButton).length).toBe(1);
    });

    it("does not render LogFilterButton if provider is not found", async () => {
      const wrapper = await createWrapper({
        modelValue: [{ type: "unknownType" }],
      });
      expect(wrapper.findComponent(LogFilterButton).exists()).toBe(false);
    });

    it("renders LogFilterButton components in inline mode when model has entries", async () => {
      const wrapper = await createWrapper({
        mode: "inline",
        modelValue: [{ type: "filterType" }],
      });
      expect(
        wrapper.find('[data-testid="log-filters-inline-buttons"]').exists(),
      ).toBe(true);
      expect(wrapper.findAllComponents(LogFilterButton).length).toBe(1);
    });
  });

  describe("filter editing", () => {
    it("loads the filter into LogFilterControls when edit is triggered on a LogFilterButton", async () => {
      const filterEntry = { type: "filterType", config: {} };
      const wrapper = await createWrapper({ modelValue: [filterEntry] });

      await wrapper.findComponent(LogFilterButton).vm.$emit("editFilter");
      await wrapper.vm.$nextTick();

      expect(
        wrapper.findComponent(LogFilterControls).props("modelValue"),
      ).toEqual(filterEntry);
    });

    it("emits update:modelValue without the removed filter when removeFilter is triggered", async () => {
      const wrapper = await createWrapper({
        modelValue: [{ type: "filterType" }],
      });

      await wrapper.findComponent(LogFilterButton).vm.$emit("removeFilter");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
      expect(wrapper.emitted("update:modelValue")![0][0]).toEqual([]);
    });
  });

  describe("LogFilterControls interaction", () => {
    it("appends a new filter when LogFilterControls emits update:modelValue with no filter selected", async () => {
      const wrapper = await createWrapper({
        modelValue: [{ type: "filterType" }],
      });

      await wrapper.findComponent(LogFilterControls).vm.$emit(
        "update:modelValue",
        { type: "filterType", config: { randomValue: true } },
      );
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
      expect(wrapper.emitted("update:modelValue")![0][0][1]).toEqual({
        type: "filterType",
        config: { randomValue: true },
      });
    });

    it("resets the LogFilterControls model when cancel is emitted", async () => {
      const filterEntry = { type: "filterType", config: {} };
      const wrapper = await createWrapper({ modelValue: [filterEntry] });

      await wrapper.findComponent(LogFilterButton).vm.$emit("editFilter");
      await wrapper.vm.$nextTick();

      await wrapper.findComponent(LogFilterControls).vm.$emit("cancel");
      await wrapper.vm.$nextTick();

      expect(
        wrapper.findComponent(LogFilterControls).props("modelValue"),
      ).toEqual({ type: "", config: {} });
    });
  });

  describe("conditionalEnabled prop (PR change)", () => {
    it("hides log-filters-container when conditionalEnabled is true", async () => {
      const wrapper = await createWrapper({ conditionalEnabled: true });
      expect(
        wrapper.find('[data-testid="log-filters-container"]').exists(),
      ).toBe(false);
    });

    it("shows log-filters-container when conditionalEnabled is false", async () => {
      const wrapper = await createWrapper({ conditionalEnabled: false });
      expect(
        wrapper.find('[data-testid="log-filters-container"]').exists(),
      ).toBe(true);
    });

    it("hides log-filters-button-container when conditionalEnabled is true", async () => {
      const wrapper = await createWrapper({
        conditionalEnabled: true,
        modelValue: [{ type: "filterType" }],
      });
      expect(
        wrapper.find('[data-testid="log-filters-button-container"]').exists(),
      ).toBe(false);
    });

    it("passes show-button=false to LogFilterControls when conditionalEnabled is true", async () => {
      const wrapper = await createWrapper({
        conditionalEnabled: true,
        showIfEmpty: true,
      });
      expect(
        wrapper.findComponent(LogFilterControls).props("showButton"),
      ).toBe(false);
    });

    it("passes show-button=true to LogFilterControls when conditionalEnabled is false and showIfEmpty is true", async () => {
      const wrapper = await createWrapper({
        conditionalEnabled: false,
        showIfEmpty: true,
      });
      expect(
        wrapper.findComponent(LogFilterControls).props("showButton"),
      ).toBe(true);
    });
  });

  describe("editEvent prop (PR change)", () => {
    it("registers editEvent handler on global eventBus when editEvent prop is provided", async () => {
      const mockEventBus = getMockEventBus();
      await createWrapper({ editEvent: "editMyFilter" });
      expect(mockEventBus.on).toHaveBeenCalledWith(
        "editMyFilter",
        expect.any(Function),
      );
    });

    it("loads the correct filter into LogFilterControls when editEvent fires with a valid index", async () => {
      const filterEntry = { type: "filterType", config: { k: "v" } };
      const mockEventBus = getMockEventBus();
      const wrapper = await createWrapper({
        editEvent: "editMyFilter",
        modelValue: [filterEntry],
      });

      const editCall = mockEventBus.on.mock.calls.find(
        ([event]: [string]) => event === "editMyFilter",
      );
      const handler = editCall![1];
      handler(0);
      await wrapper.vm.$nextTick();

      expect(
        wrapper.findComponent(LogFilterControls).props("modelValue"),
      ).toEqual(filterEntry);
    });

    it("does not update LogFilterControls model when editEvent fires with an out-of-bounds index", async () => {
      const mockEventBus = getMockEventBus();
      const wrapper = await createWrapper({
        editEvent: "editMyFilter",
        modelValue: [{ type: "filterType" }],
      });

      const editCall = mockEventBus.on.mock.calls.find(
        ([event]: [string]) => event === "editMyFilter",
      );
      const handler = editCall![1];
      handler(5);
      await wrapper.vm.$nextTick();

      expect(
        wrapper.findComponent(LogFilterControls).props("modelValue"),
      ).toEqual({ type: "", config: {} });
    });

    it("deregisters editEvent handler on beforeUnmount", async () => {
      const mockEventBus = getMockEventBus();
      const wrapper = await createWrapper({ editEvent: "editMyFilter" });

      const editCall = mockEventBus.on.mock.calls.find(
        ([event]: [string]) => event === "editMyFilter",
      );
      const registeredHandler = editCall![1];

      wrapper.unmount();

      expect(mockEventBus.off).toHaveBeenCalledWith(
        "editMyFilter",
        registeredHandler,
      );
    });
  });

  describe("addEvent cleanup (PR change: stored handler reference for proper off)", () => {
    it("deregisters addEvent handler with the specific function reference on beforeUnmount", async () => {
      const mockEventBus = getMockEventBus();
      const wrapper = await createWrapper({ addEvent: "addMyFilter" });

      const addCall = mockEventBus.on.mock.calls.find(
        ([event]: [string]) => event === "addMyFilter",
      );
      const registeredHandler = addCall![1];

      wrapper.unmount();

      expect(mockEventBus.off).toHaveBeenCalledWith(
        "addMyFilter",
        registeredHandler,
      );
    });
  });

  describe("editModelValidation initialization (PR change: resetValidation())", () => {
    it("calls resetValidation() to initialize editModelValidation on mount", async () => {
      await createWrapper();
      expect(resetValidation).toHaveBeenCalled();
    });
  });
});
