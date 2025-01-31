const mockEventBus = {
  emit: jest.fn(),
  on: jest.fn(),
  off: jest.fn(),
};
import { mount, VueWrapper } from "@vue/test-utils";
import NodeDefaultFilterDropdown from "../NodeDefaultFilterDropdown.vue";

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mockRdBase",
    eventBus: mockEventBus,
  }),
}));
const mockNodeSummary = {
  filters: [
    { filterName: "filter1", filter: "filter1" },
    { filterName: "filter2", filter: "filter2" },
  ],
  totalCount: 2,
};

const mountNodeDefaultFilterDropdown = async (props = {}) => {
  const wrapper = mount(NodeDefaultFilterDropdown, {
    props: {
      isDefaultFilter: false,
      nodeSummary: mockNodeSummary,
      ...props,
    },
    global: {
      stubs: {
        dropdown: {
          template: '<div><slot></slot><slot name="dropdown"></slot></div>',
        },
        btn: true,
      },
      mocks: {
        $t: (msg) => msg,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper as VueWrapper<InstanceType<typeof NodeDefaultFilterDropdown>>;
};
describe("NodeDefaultFilterDropdown Component", () => {
  beforeEach(() => {
    jest.clearAllMocks(); // Clear mocks to ensure isolated tests
  });
  it("emits filter event when a saved filter is selected", async () => {
    const wrapper = await mountNodeDefaultFilterDropdown();
    const filterLink = wrapper.find('a[data-testid="saved-filter"]');
    await filterLink.trigger("click");
    expect(wrapper.emitted().filter[0]).toEqual([
      { filterName: "filter1", filter: "filter1" },
    ]);
  });
  describe("sets a filter as default", () => {
    it("when starting without a default filter", async () => {
      const wrapper = await mountNodeDefaultFilterDropdown();
      const setDefaultLink = wrapper.find(
        'a[data-testid="set-default-filter-link"]',
      );
      await setDefaultLink.trigger("click");
      expect(mockEventBus.emit).toHaveBeenCalledWith(
        "nodefilter:action:setDefault",
        "filter1",
      );
    });
    it("when starting with a default filter", async () => {
      const wrapper = await mountNodeDefaultFilterDropdown({
        isDefaultFilter: true,
        nodeSummary: {
          ...mockNodeSummary,
          defaultFilter: "filter1",
        },
      });

      const removeDefaultLink = wrapper.find(
        'a[data-testid="remove-default-filter-link"]',
      );
      expect(removeDefaultLink.text()).toBe("remove.default.filter");
      await removeDefaultLink.trigger("click");
      expect(mockEventBus.emit).toHaveBeenCalledWith(
        "nodefilter:action:removeDefault",
      );
      const setDefaultLink = wrapper.find(
        'a[data-testid="set-default-filter-link"]',
      );
      expect(setDefaultLink.text()).toBe("set.as.default.filter");
    });
  });
});
