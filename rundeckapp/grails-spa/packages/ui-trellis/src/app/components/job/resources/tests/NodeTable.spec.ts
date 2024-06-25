import { mount, VueWrapper } from "@vue/test-utils";
import NodeTable from "../NodeTable.vue";
import NodeFilterLink from "../NodeFilterLink.vue";
import NodeDetailsSimple from "../NodeDetailsSimple.vue";
import NodeRemoteEdit from "../NodeRemoteEdit.vue";
rom "../services/nodeServices";
jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mockRdBase",
    projectName: "test-project",
  }),
  url: jest.fn().mockReturnValue({ href: "mockHref" }),
}));
jest.mock("../services/nodeServices", () => ({
  getNodeSummary: jest.fn(),
  getNodes: jest.fn(),
}));
describe("NodeTable Component", () => {
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
          "ui:status:icon": "icon-class",
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
          "ui:status:icon": "icon-class",
        },
      },
    ],
  };
  const mountNodeTable = (propsData = {}) => {
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
          NodeFilterLink,
          NodeDetailsSimple,
          NodeRemoteEdit,
        },
        mocks: {
          $t: (msg) => msg,
          $tc: (msg) => msg,
        },
      },
    });
  };
  beforeEach(() => {
    jest.clearAllMocks();
  });
  it("renders node attributes correctly", () => {
    const wrapper = mountNodeTable();
    const rows = wrapper.findAll('[data-test-id^="node-entry-"]');
    expect(rows.length).toBe(mockNodeSet.nodes.length);
    mockNodeSet.nodes.forEach((node, index) => {
      const row = rows.at(index);
      expect(row.find('[data-test-id="node-name"]').text()).toContain(
        node.nodename,
      );
      expect(
        row.find('[data-test-id="node-attribute-description"]').text(),
      ).toContain(node.attributes.description);
      expect(
        row.find('[data-test-id="node-attribute-hostname"]').text(),
      ).toContain(node.attributes.hostname);
      expect(
        row.find('[data-test-id="node-attribute-osArch"]').text(),
      ).toContain(node.attributes.osArch);
      expect(
        row.find('[data-test-id="node-attribute-osFamily"]').text(),
      ).toContain(node.attributes.osFamily);
      expect(
        row.find('[data-test-id="node-attribute-osName"]').text(),
      ).toContain(node.attributes.osName);
      expect(
        row.find('[data-test-id="node-attribute-osVersion"]').text(),
      ).toContain(node.attributes.osVersion);
    });
  });
  it("renders health status from attributes", () => {
    const wrapper = mountNodeTable();
    const statusTexts = wrapper.findAll(".node-status-text");
    expect(statusTexts.length).toBe(mockNodeSet.nodes.length);
    expect(statusTexts.at(0).text()).toContain("Healthy");
    expect(statusTexts.at(1).text()).toContain("Unhealthy");
  });
  it("filters nodes by attribute when an attribute is clicked", async () => {
    const wrapper = mountNodeTable();
    const attributeLink = wrapper.find(
      '[data-test-id="node-attribute-link-description"]',
    );
    await attributeLink.trigger("click");
    expect(wrapper.emitted().filter).toBeTruthy();
    // Update the expectation to match the actual emitted event structure
    expect(wrapper.emitted().filter[0][0]).toEqual({
      filter: 'description: "Rundeck server node"',
    });
  });
  it("filters nodes by name when the hostname is clicked", async () => {
    const wrapper = mountNodeTable();
    const hostnameFilter = wrapper.find('[data-test-id="node-name"]');
    await hostnameFilter.trigger("click");
    expect(wrapper.emitted().filter).toBeTruthy();
    // Update the expectation to match the actual emitted event structure
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
