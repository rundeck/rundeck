import { mount, VueWrapper } from "@vue/test-utils";
import NodeFilterSection from "../NodeFilterSection.vue";
import { NodeFilterStore } from "../../../../library/stores/NodeFilterLocalstore";

jest.mock("../../../../library", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "http://localhost:4440",
    projectName: "test-project",
  }),
  getAppLinks: jest.fn().mockReturnValue({
    frameworkNodesQueryAjax: "/api/nodes/query",
  }),
  url: jest.fn((url: string) => ({ href: url })),
}));

describe("NodeFilterSection", () => {
  const mockNodeFilterStore = {
    selectedFilter: "name: test",
    setSelectedFilter: jest.fn(),
  } as unknown as NodeFilterStore;

  it("should render correctly", () => {
    const wrapper = mount(NodeFilterSection, {
      props: {
        nodeFilterStore: mockNodeFilterStore,
        maxShown: 50,
        project: "test-project",
      },
    });

    expect(wrapper.find(".card").exists()).toBe(true);
  });

  it("should show empty state when no nodes", () => {
    const wrapper = mount(NodeFilterSection, {
      props: {
        nodeFilterStore: mockNodeFilterStore,
        maxShown: 50,
        project: "test-project",
      },
      data() {
        return {
          loading: false,
          error: null,
          total: 0,
        };
      },
    }) as VueWrapper<any>;

    // Empty state should show when total is 0 and has filter
    expect(wrapper.vm.showEmptyState).toBe(true);
  });

  it("should emit node-total-changed when total changes", () => {
    const wrapper = mount(NodeFilterSection, {
      props: {
        nodeFilterStore: mockNodeFilterStore,
        maxShown: 50,
        project: "test-project",
      },
    }) as VueWrapper<any>;

    wrapper.vm.handleTotalUpdate(5);
    expect(wrapper.emitted("node-total-changed")).toBeTruthy();
    expect(wrapper.emitted("node-total-changed")?.[0]).toEqual([5]);
  });
});

