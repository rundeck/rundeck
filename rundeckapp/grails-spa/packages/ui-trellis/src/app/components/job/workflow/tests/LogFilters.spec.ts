import { flushPromises, shallowMount, VueWrapper } from "@vue/test-utils";
import LogFilters from "../LogFilters.vue";
import LogFilterButton from "@/app/components/job/workflow/LogFilterButton.vue";
import mitt, { Emitter, EventType } from "mitt";
import LogFilterControls from "../LogFilterControls.vue";

jest.mock("@/library/modules/rundeckClient", () => ({
  client: jest.fn(),
}));

jest.mock("@/library", () => {
  return {
    getRundeckContext: jest.fn().mockImplementation(() => ({
      client: jest.fn().mockImplementation(() => ({
        eventBus: { on: jest.fn(), emit: jest.fn() },
      })),
      eventBus: { on: jest.fn(), emit: jest.fn() },
    })),
  };
});

// Mock the plugin service
jest.mock("@/library/modules/pluginService", () => ({
  getPluginProvidersForService: jest.fn().mockResolvedValue({
    service: true,
    descriptions: [{ name: "filterType" }],
    labels: ["Filter Type"],
  }),
}));

const createWrapper = async (props = {}): Promise<VueWrapper<any>> => {
  const wrapper = shallowMount(LogFilters, {
    props: {
      modelValue: [],
      title: "Log Filters",
      subtitle: "Manage your log filters",
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
  const eventBus: Emitter<Record<EventType, any>> = mitt();
  beforeEach(() => {
    jest.clearAllMocks();
  });

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

  it("calls editFilterByIndex when edit-filter event is emitted", async () => {
    const wrapper = await createWrapper({
      modelValue: [{ type: "filterType" }],
    });
    const spy = jest.spyOn(wrapper.vm, "editFilterByIndex");

    const button = wrapper.findComponent(LogFilterButton);
    await button.vm.$emit("editFilter");
    await wrapper.vm.$nextTick();

    expect(spy).toHaveBeenCalled();
  });

  it("removes filter when remove-filter event is emitted", async () => {
    const wrapper = await createWrapper({
      modelValue: [{ type: "filterType" }],
    });
    const button = wrapper.findComponent(LogFilterButton);
    await button.vm.$emit("removeFilter");
    expect(wrapper.emitted()["update:modelValue"][0][0]).toEqual([]);
  });

  it("saves edited filter when saveEditFilter is called", async () => {
    const wrapper = await createWrapper({
      modelValue: [{ type: "filterType" }],
    });
    const button = wrapper.findComponent(LogFilterControls);
    await button.vm.$emit("update:modelValue", {
      type: "filterType",
      config: { randomValue: true },
    });
    await wrapper.vm.$nextTick();

    // need to double-check
    expect(wrapper.emitted("update:modelValue")[0][0][1]).toEqual({
      type: "filterType",
      config: { randomValue: true },
    });
  });

  it("clears edit model when clearEdit is called", async () => {
    const wrapper = await createWrapper();
    const button = wrapper.findComponent(LogFilterControls);
    await button.vm.$emit("cancel");
    expect(wrapper.vm.editFilterIndex).toBe(-1);
    expect(wrapper.vm.editModel).toEqual({ type: "", config: {} });
  });
});
