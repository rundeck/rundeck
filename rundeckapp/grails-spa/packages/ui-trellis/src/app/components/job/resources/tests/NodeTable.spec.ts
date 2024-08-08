import { mount } from "@vue/test-utils";
import NodeTable from "../NodeTable.vue";
import NodeDetailsSimple from "../NodeDetailsSimple.vue";

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mockRdBase",
  }),
  url: jest.fn().mockReturnValue({ href: "mockHref" }),
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
        "ui:status:text": "Healthy",
        "ui:badges": "fa-badge1, fab-badge2",
        "ui:color": "red",
        "ui:icon:name": "fa-server",
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
        "ui:status:text": "Unhealthy",
        "ui:badges": "fa-badge3, fab-badge4",
        "ui:color": "blue",
        "ui:icon:name": "fa-hdd",
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
  it("renders health status from attributes", async () => {
    const wrapper = await mountNodeTable();
    const statusTexts = wrapper.findAll(".node-status-text");
    expect(statusTexts.length).toBe(mockNodeSet.nodes.length);
    expect(statusTexts.at(0).text()).toContain("Healthy");
    expect(statusTexts.at(1).text()).toContain("Unhealthy");
  });
  it("renders node icon, color, and badge correctly", async () => {
    const wrapper = await mountNodeTable();
    // Check node icons
    const nodeIcons = wrapper.findAll('[data-test-id="node-icon"] i');
    expect(nodeIcons.at(0).classes()).toContain("fas");
    expect(nodeIcons.at(0).classes()).toContain("fa-server");
    expect(nodeIcons.at(1).classes()).toContain("fas");
    expect(nodeIcons.at(1).classes()).toContain("fa-hdd");
    // Check node colors
    const nodeColors = wrapper.findAll('[data-test-id="node-icon"]');
    nodeColors.forEach((colorWrapper) => {
      const styleAttribute = colorWrapper.attributes("style");
      expect(styleAttribute).toMatch(/color:\s*(red|blue);?/);
    });
    expect(nodeColors.length).toBe(2);
    // Check node badges
    const nodeBadges = wrapper.findAll('[data-test-id="node-badge-icon"]');
    expect(nodeBadges.length).toBe(4);
  });

  it("filters nodes by attribute when an attribute is clicked", async () => {
    const wrapper = await mountNodeTable();
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
    const wrapper = await mountNodeTable();
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
    const wrapper = await mountNodeTable();
    const collapseLink = wrapper.find('[data-test-id="node-collapse-link"]');
    await collapseLink.trigger("click");
    const nestedAttributes = wrapper.findComponent(NodeDetailsSimple);
    expect(nestedAttributes.text()).toMatch(/status:text: Healthy/);
  });
});
