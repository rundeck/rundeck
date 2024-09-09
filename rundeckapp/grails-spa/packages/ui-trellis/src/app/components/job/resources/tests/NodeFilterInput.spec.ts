import { mount, VueWrapper } from "@vue/test-utils";
import mitt, { Emitter, EventType } from "mitt";
import NodeFilterInput from "../NodeFilterInput.vue";
import { NodeSummary } from "../types/nodeTypes";

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mockRdBase",
    projectName: "test-project",
    eventBus: {
      on: jest.fn(),
      emit: jest.fn(),
      off: jest.fn(),
    },
  }),
  url: jest.fn().mockReturnValue({ href: "mockHref" }),
}));
const mockNodeFilterStore = {
  saveFilter: jest.fn(),
  removeFilter: jest.fn(),
  setStoredDefaultFilter: jest.fn(),
  loadStoredProjectNodeFilters: jest.fn().mockReturnValue({
    filters: [{ filter: ".*", filterName: "All Nodes" }],
    defaultFilter: null,
  }),
};
jest.mock("../../../../../library/stores/NodeFilterLocalstore", () => ({
  NodeFilterStore: jest.fn().mockImplementation(() => mockNodeFilterStore),
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
const mountNodeFilterInput = async (
  props = {},
  options: Record<string, any> = {},
) => {
  const wrapper = mount(NodeFilterInput, {
    props: {
      nodeSummary: mockNodeSummary,
      modelValue: "",
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
      mocks: {
        $t: (msg) => msg,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper as VueWrapper<InstanceType<typeof NodeFilterInput>>;
};
describe("NodeFilterInput Component", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });
  it("updates modelValue on input and performs search on enter keydown", async () => {
    const wrapper = await mountNodeFilterInput({
      modelValue: "initial filter",
    });
    const input = wrapper.find('input[data-testid="filter-input"]');
    await input.setValue("new filter");
    await input.trigger("blur");
    expect(wrapper.emitted()["update:modelValue"][0]).toEqual(["new filter"]);
    await input.setValue("updated filter");
    await input.trigger("keydown.enter");
    expect(wrapper.emitted()["update:modelValue"][1]).toEqual([
      "updated filter",
    ]);
  });
  it("saves a filter", async () => {
    const eventBus: Emitter<Record<EventType, any>> = mitt();
    const emitSpy = jest.spyOn(eventBus, "emit");
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
