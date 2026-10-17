import { vi } from "vitest";
import { mount, VueWrapper } from "@vue/test-utils";
import mitt, { Emitter, EventType } from "mitt";
import NodeFilterInput from "../NodeFilterInput.vue";
import { NodeSummary } from "../types/nodeTypes";

vi.mock("@/library/rundeckService", () => ({
  getRundeckContext: vi.fn().mockReturnValue({
    rdBase: "mockRdBase",
    projectName: "test-project",
    eventBus: {
      on: vi.fn(),
      emit: vi.fn(),
      off: vi.fn(),
    },
  }),
  url: vi.fn().mockReturnValue({ href: "mockHref" }),
}));
const { mockNodeFilterStore } = vi.hoisted(() => ({
  mockNodeFilterStore: {
    saveFilter: vi.fn(),
    removeFilter: vi.fn(),
    setStoredDefaultFilter: vi.fn(),
    loadStoredProjectNodeFilters: vi.fn().mockReturnValue({
      filters: [{ filter: ".*", filterName: "All Nodes" }],
      defaultFilter: null,
    }),
  },
}));
vi.mock("../../../../../library/stores/NodeFilterLocalstore", () => ({
  NodeFilterStore: vi.fn(function NodeFilterStore() {
    return mockNodeFilterStore;
  }),
}));
const eventBus = mitt();
const mockNodeSummary: NodeSummary = {
  filters: [
    { filterName: "filter1", filter: "filter1", isJobEdit: true },
    { filterName: "filter2", filter: "filter2", isJobEdit: true },
  ],
  tags: [
    { count: 5, tag: "Tag1" },
    { count: 3, tag: "Tag2" },
  ],
  totalCount: 2,
  defaultFilter: null,
};
const mountNodeFilterInput = async (props = {}) => {
  const wrapper = mount(NodeFilterInput, {
    props: {
      nodeSummary: mockNodeSummary,
      value: "",
      project: "test-project",
      eventBus,
      ...props,
    },
    global: {
      stubs: {
        btn: {
          template: "<button><slot></slot></button>",
        },
        popover: true,
        modal: {
          template: '<div><slot></slot><slot name="footer"></slot></div>',
        },
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper as VueWrapper<InstanceType<typeof NodeFilterInput>>;
};
describe("NodeFilterInput Component", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });
  it("updates value on input and performs search on enter keydown", async () => {
    const wrapper = await mountNodeFilterInput({
      value: "initial filter",
    });
    const input = wrapper.find('input[data-testid="filter-input"]');
    await input.setValue("new filter");
    await input.trigger("blur");
    expect(wrapper.emitted()["update:value"][0]).toEqual(["new filter"]);
    await input.setValue("updated filter");
    await input.trigger("keydown.enter");
    expect(wrapper.emitted()["update:value"][1]).toEqual(["updated filter"]);
  });
  it("saves a filter", async () => {
    const eventBus: Emitter<Record<EventType, any>> = mitt();
    const emitSpy = vi.spyOn(eventBus, "emit");
    const wrapper = await mountNodeFilterInput({
      eventBus: eventBus,
    });
    eventBus.emit("nodefilter:savedFilters:changed");
    const input = wrapper.find('input[data-testid="new-filter-name-input"]');
    await input.setValue("newFilter");
    await input.trigger("blur");
    const saveFilterButton = wrapper.find(
      'button[data-testid="sfm-button-save"]',
    );
    await saveFilterButton.trigger("click");
    expect(mockNodeFilterStore.saveFilter).toHaveBeenCalledWith(
      "test-project",
      {
        filterName: "newFilter",
        isJobEdit: true,
        filter: "",
      },
    );
    expect(emitSpy).toHaveBeenCalledWith("nodefilter:savedFilters:changed");
  });
});
