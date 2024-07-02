import { mount } from "@vue/test-utils";
import NodeTable from "../NodeTable.vue";
import NodeDetailsSimple from "../NodeDetailsSimple.vue";
import * as nodeUI from "../../../../utilities/nodeUi";
jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mockRdBase",
    projectName: "test-project",
  }),
  url: jest.fn().mockReturnValue({ href: "mockHref" }),
}));
jest.mock("../services/nodeServices", () => ({
  getNodes: jest.fn(),
}));
jest.mock("../../../../utilities/nodeUi", () => ({
  ...jest.requireActual("../../../../utilities/nodeUi"),
  glyphiconForName: jest.fn().mockReturnValue("fas fa-hdd"),
}));
const mockNodeSet = {
  nodes: [
    {
      name: "node1",
      nodename: "localhost",
      attributes: {
        description: "Rundeck server node",
        hostname: "node2",
        osArch: "aarch64",
        osFamily: "Unix",
        osName: "agent",
        osVersion: "14.5",
        tags: "tag1,tag2",
        authrun: true,
        isLocalNode: true,
        "ui:status:text": "Healthy",
        "ui:status:icon": ["fas", "fa-hdd"],
        color: "red",
      },
    },
    {
      name: "node2",
      nodename: "node2",
      attributes: {
        description: "local node",
        hostname: "node3",
        osArch: "x86_64",
        osFamily: "linux",
        osName: "Ubuntu",
        osVersion: "20.04",
        tags: "tag3,tag4",
        authrun: false,
        isLocalNode: false,
        "ui:status:text": "Unhealthy",
        "ui:status:icon": ["fas", "fa-hdd"],
      },
    },
  ],
};
const mountNodeTable = (propsData = {}): any => {
  return mount(NodeTable, {
    props: {
      nodeSet: mockNodeSet,
      filterColumns: [
        "description",
        "hostname",
        "osArch",
        "osFamily",
        "osName",
        "osVersion",
        "tags",
      ],
      ...propsData,
    },
    global: {
      stubs: {
        NodeDetailsSimple,
      },
      mocks: {
        $t: (msg) => msg,
        $tc: (msg) => msg,
      },
    },
  });
};
describe("NodeTable Component", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });
  it("renders health status from attributes", () => {
    const wrapper = mountNodeTable();
    const statusTexts = wrapper.findAll(".node-status-text");
    expect(statusTexts.length).toBe(mockNodeSet.nodes.length);
    expect(statusTexts.at(0).text()).toContain("Healthy");
    expect(statusTexts.at(1).text()).toContain("Unhealthy");
  });
  it("renders node icon, color, and badge correctly", async () => {
    const wrapper = await mountNodeTable();
    const nodeIcons = wrapper.findAll('[data-test-id="node-icon"] i');
    nodeIcons.forEach((iconWrapper) => {
      expect(iconWrapper.classes()).toContain("fas");
      expect(iconWrapper.classes()).toContain("fa-hdd");
    });
    const nodeColors = wrapper.findAll('[data-test-id="node-color"]');
    nodeColors.forEach((colorWrapper, index) => {
      expect(colorWrapper.attributes("style")).toContain(
        `color: ${mockNodeSet.nodes[index].attributes.color}`,
      );
    });
  });
  it("filters nodes by attribute when an attribute is clicked", async () => {
    const wrapper = mountNodeTable();
    const attributeLink = wrapper.find(
      '[data-test-id="node-attribute-link-description"]',
    );
    await attributeLink.trigger("click");
    expect(wrapper.emitted().filter).toBeTruthy();
    expect(wrapper.emitted().filter[0][0]).toEqual({
      filter: 'description: "Rundeck server node"',
    });
  });
  it("filters nodes by name when the hostname is clicked", async () => {
    const wrapper = mountNodeTable();
    const hostnameFilter = wrapper.find(
      '[data-test-id="node-attribute-link-hostname"]',
    );
    await hostnameFilter.trigger("click");
    expect(wrapper.emitted().filter).toBeTruthy();
    expect(wrapper.emitted().filter[0][0]).toEqual({
      filter: 'hostname: "node2"',
    });
  });
  it("renders expandable nested attributes", async () => {
    const wrapper = mountNodeTable();
    const collapseLink = wrapper.find('[data-test-id="node-collapse-link"]');
    await collapseLink.trigger("click");
    const nestedAttributes = wrapper.findComponent(NodeDetailsSimple);
    expect(nestedAttributes.exists()).toBe(true);
  });
});
