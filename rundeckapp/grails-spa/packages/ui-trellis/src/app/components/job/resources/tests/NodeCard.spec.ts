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
  const wrapper = mount(NodeCard, {
    props: {
      project: "test-project",
      runAuthorized: true,
      jobCreateAuthorized: true,
      nodeFilterStore: {
        filter: "",
        selectedFilter: "",
      },
      ...propsData,
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
      expect(wrapper.emitted().filter).toBeTruthy();
    });
    it("displays the node list results", async () => {
      const wrapper = await mountNodeCard();
      await wrapper.vm.$nextTick();
      const nodeTable = wrapper.findComponent(NodeTable);
      expect(nodeTable.exists()).toBe(true);
      const nodeSet = nodeTable.props().nodeSet;
      expect(nodeSet).toBeDefined();
      expect(mockNodeSet.nodes).toBeDefined();
      expect(mockNodeSet.nodes[0].attributes["ui:status:text"]).toBe("Healthy");
      expect(mockNodeSet.nodes[1].attributes["ui:status:text"]).toBe(
        "Unhealthy",
      );
    });
  });
});
