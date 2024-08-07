import { mount } from "@vue/test-utils";
import NodeTable from "../NodeTable.vue";
import NodeDetailsSimple from "../NodeDetailsSimple.vue";
import { mockNodeSet } from "./mocks/apiData";

jest.mock("@/library", () => ({
  getAppLinks: jest
    .fn()
    .mockReturnValue({ frameworkNodes: "/resources/nodes" }),
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mockRdBase",
  }),
  url: jest.fn().mockReturnValue({ href: "mockHref" }),
}));

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
      pagingMax: 5,
      page: 0,
      hasPaging: true,
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
    expect(nodeColors.length).toBe(6);
    // Check node badges
    const nodeBadges = wrapper.findAll('[data-testid="node-badge-icon"]');
    expect(nodeBadges.length).toBe(12);
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
    const collapseLink = wrapper.find('[data-testid="node-collapse-link"]');
    await collapseLink.trigger("click");
    const nestedAttributes = wrapper.findComponent(NodeDetailsSimple);
    expect(nestedAttributes.text()).toMatch(/status:text: Healthy/);
  });

  it("renders pagination component when the number of results is bigger than the maximum entries per page", async () => {
    const wrapper = await mountNodeTable({
      pagingMax: 2,
      maxPages: 3,
    });
    await wrapper.vm.$nextTick();
    const pages = wrapper.findAll("#nodesPaging li");
    expect(pages.length).toBe(5);

    const arrayOfPageTexts = pages.map((page) => page.text());
    expect(arrayOfPageTexts).toEqual([
      "default.paginate.prev",
      "1",
      "2",
      "3",
      "default.paginate.next",
    ]);
  });

  it("renders pagination component and skip pages if the number of pages is bigger than the max", async () => {
    const wrapper = await mountNodeTable({
      page: 1,
      pagingMax: 3,
      maxPages: 11,
    });
    await wrapper.vm.$nextTick();
    const pages = wrapper.findAll("#nodesPaging li");

    const arrayOfPageTexts = pages.map((page) => page.text());
    expect(arrayOfPageTexts).toEqual([
      "default.paginate.prev",
      "1",
      "2",
      "3",
      "4",
      "5",
      "…",
      "9",
      "10",
      "11",
      "default.paginate.next",
    ]);
  });
});
