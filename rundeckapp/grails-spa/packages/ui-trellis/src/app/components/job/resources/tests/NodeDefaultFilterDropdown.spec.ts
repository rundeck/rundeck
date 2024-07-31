import { mount, VueWrapper } from "@vue/test-utils";
import NodeDefaultFilterDropdown from "../NodeDefaultFilterDropdown.vue";
import mitt, { Emitter, EventType } from "mitt";
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
}));

const mockNodeSummary = {
  filters: [
    { filterName: "filter1", filter: "filter1" },
    { filterName: "filter2", filter: "filter2" },
  ],
  totalCount: 2,
  defaultFilter: "someOtherFilter",
};

const mountNodeDefaultFilterDropdown = async (
  props = {},
  options: Record<string, any> = {},
) => {
  const wrapper = mount(NodeDefaultFilterDropdown, {
    props: {
      isDefaultFilter: false,
      nodeSummary: mockNodeSummary,
      ...props,
    },
    global: {
      components: {},
      stubs: {
        dropdown: {
          template: '<div><slot></slot><slot name="dropdown"></slot></div>',
        },
        btn: true,
      },
      mocks: {
        $t: (msg) => msg,
        ...options.mocks,
      },
    },
    ...options,
  });
  await wrapper.vm.$nextTick();
  return wrapper as VueWrapper<InstanceType<typeof NodeDefaultFilterDropdown>>;
};
describe("NodeDefaultFilterDropdown Component", () => {
  let eventBus: Emitter<Record<EventType, any>>;
  beforeAll(() => {
    eventBus = mitt();
  });
  afterEach(() => {
    jest.clearAllMocks();
  });
  it("triggers filter event when a saved filter is selected", async () => {
    const wrapper = await mountNodeDefaultFilterDropdown();
    const filterLink = wrapper.find('a[data-testid="saved-filter"]');
    expect(filterLink.exists()).toBe(true);
    await filterLink.trigger("click");
    expect(wrapper.emitted().filter[0]).toEqual([
      { filterName: "filter1", filter: "filter1" },
    ]);
  });
  it("sets a filter as default", async () => {
    const emitSpy = jest.spyOn(eventBus, "emit");
    const wrapper = await mountNodeDefaultFilterDropdown();
    eventBus.emit("nodefilter:action:setDefault", "filter1");
    const setDefaultLink = wrapper.find(
      'a[data-testid="set-default-filter-link"]',
    );
    expect(setDefaultLink.exists()).toBe(true);
    await setDefaultLink.trigger("click");
    expect(emitSpy).toHaveBeenCalledWith(
      "nodefilter:action:setDefault",
      "filter1",
    );
  });
});
