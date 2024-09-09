import { mount } from "@vue/test-utils";
import NodeCard from "../NodeCard.vue";
import NodeTable from "../NodeTable.vue";
import NodeFilterLink from "../NodeFilterLink.vue";
import * as nodeServices from "../services/nodeServices";

const mockedGetNodeSummary = nodeServices.getNodeSummary as jest.MockedFunction<
  typeof nodeServices.getNodeSummary
>;
const mockedGetNodes = nodeServices.getNodes as jest.MockedFunction<
  typeof nodeServices.getNodes
>;
jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mockRdBase",
    projectName: "test-project",
    eventBus: {
      on: jest.fn(),
      off: jest.fn(),
      emit: jest.fn(),
    },
  }),
  url: jest.fn().mockReturnValue({ href: "mockHref" }),
}));
jest.mock("../services/nodeServices", () => ({
  getNodeSummary: jest.fn(),
  getNodes: jest.fn(),
}));
const mockNodeSummary = {
  tags: [
    { tag: "Tag1", count: 5 },
    { tag: "Tag2", count: 3 },
  ],
};
const mockNodeSet = {
  nodes: [
    {
      name: "node1",
      nodename: "node1",
      attributes: {
        color: "red",
        icon: "icon1",
        description: "desc1",
        "ui:status:text": "Healthy",
      },
    },
    {
      name: "node2",
      nodename: "node2",
      attributes: {
        color: "blue",
        icon: "icon2",
        description: "desc2",
        "ui:status:text": "Unhealthy",
      },
    },
  ],
};
const mountNodeCard = async (propsData = {}) => {
  const wrapper = mount(NodeCard, {
    props: {
      project: "test-project",
      runAuthorized: true,
      jobCreateAuthorized: true,
      nodeFilterStore: {
        filter: "hostname:node1",
        selectedFilter: "hostname:node1",
      },
      ...propsData,
    },
    data() {
      return {
        allCount: 2,
        nodeSet: mockNodeSet,
      };
    },
    global: {
      mocks: {
        $t: (msg) => msg,
        $tc: (msg) => msg,
      },
      stubs: {
        btn: true,
        dropdown: true,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};
describe("NodeCard Component", () => {
  beforeEach(() => {
    mockedGetNodeSummary.mockResolvedValue(mockNodeSummary);
    mockedGetNodes.mockResolvedValue({
      allnodes: mockNodeSet.nodes,
      tagsummary: mockNodeSummary.tags,
      allcount: 2,
      total: 2,
      truncated: false,
      colkeys: ["name", "attributes"],
      max: 20,
    });
  });
  afterEach(() => {
    jest.clearAllMocks();
  });
  describe("Browse Tags Functionality", () => {
    it("displays the tags correctly", async () => {
      const wrapper = await mountNodeCard();
      const tags = wrapper.findAll(".label-muted");
      expect(tags.length).toBe(mockNodeSummary.tags.length);
      expect(tags.at(0).text()).toContain("Tag1 (5)");
      expect(tags.at(1).text()).toContain("Tag2 (3)");
    });
    it("emits filter event when a tag is clicked", async () => {
      const wrapper = await mountNodeCard();
      const tagLink = wrapper.findAllComponents(NodeFilterLink).at(0);
      await tagLink.trigger("click");
      const emittedEvent = wrapper.emitted().filter;
      expect(emittedEvent).toBeTruthy();
      expect(emittedEvent.length).toBe(1);
      expect(emittedEvent[0]).toEqual([{ filter: 'tags: "Tag1"' }]);
    });
    it("displays the node list results and verifies active tab", async () => {
      const wrapper = await mountNodeCard();
      await wrapper.vm.$nextTick();
      const nodeTableTab = wrapper.find('[data-testid="node-table-tab"]');
      expect(nodeTableTab.classes()).toContain("active");
      const summaryTab = wrapper.find('[data-testid="summary-tab"]');
      expect(summaryTab.classes()).not.toContain("active");
      const nodeTable = wrapper.findComponent(NodeTable);
      const rows = nodeTable.findAll("tr.node_entry");
      expect(rows.length).toBe(2);
    });
  });
});
