import { mount, VueWrapper } from "@vue/test-utils";
import NodeCard from "../NodeCard.vue";
import NodeFilterLink from "../NodeFilterLink.vue";
import * as nodeServices from "../services/nodeServices";
import {
  getNodeSummary as mockGetNodeSummary,
  getNodes as mockGetNodes,
} from "../services/nodeServices";
function createMockEventBus() {
  return {
    on: jest.fn(),
    emit: jest.fn(),
    off: jest.fn(),
    all: new Map(),
  };
}
jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mockRdBase",
    projectName: "test-project",
    eventBus: createMockEventBus(),
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
const mockNodeFilterStore = {
  filter: "",
  selectedFilter: "",
  loadStoredProjectNodeFilters: jest.fn(() => {}),
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
        "ui:status:icon": "some-icon-class",
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
        "ui:status:icon": "some-icon-class",
      },
    },
  ],
};
const mountNodeCard = async (propsData = {}): Promise<VueWrapper<any>> => {
  (
    mockGetNodeSummary as jest.MockedFunction<typeof mockGetNodeSummary>
  ).mockResolvedValue(mockNodeSummary);
  (mockGetNodes as jest.MockedFunction<typeof mockGetNodes>).mockResolvedValue({
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
      nodeFilterStore: mockNodeFilterStore,
      ...propsData,
    },
    global: {
      mocks: {
        $t: (msg) => msg,
        $tc: (msg) => msg,
      },
      stubs: {
        NodeStatus: {
          template: `<span><i :class="node.attributes['ui:status:icon']" class="node-status-icon"></i><span v-if="showText" class="node-status-text">{{ node.attributes['ui:status:text'] }}</span></span>`,
          props: ["node", "showText"],
        },
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};
beforeEach(() => {
  jest.clearAllMocks();
  (
    mockGetNodeSummary as jest.MockedFunction<
      typeof nodeServices.getNodeSummary
    >
  ).mockResolvedValue(mockNodeSummary);
  (
    mockGetNodes as jest.MockedFunction<typeof nodeServices.getNodes>
  ).mockResolvedValue({
    allnodes: mockNodeSet.nodes,
    tagsummary: mockNodeSummary.tags,
    allcount: 2,
    total: 2,
    truncated: false,
    colkeys: ["name", "attributes"],
    max: 20,
  });
});
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
  });
});
