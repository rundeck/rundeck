import { mount } from "@vue/test-utils";
import { EventBus } from "../../../../../library";
import { RootStore } from "../../../../../library/stores/RootStore";
import { RundeckToken } from "../../../../../library/interfaces/rundeckWindow";
import NodeFilterLink from "../NodeFilterLink.vue";

describe("NodeFilterLink Component", () => {
  beforeAll(() => {
    window._rundeck = {
      eventBus: jest.fn() as unknown as typeof EventBus,
      rdBase: "http://localhost",
      apiVersion: "mockApiVersion",
      projectName: "test-project",
      activeTour: "mockActiveTour",
      activeTourStep: "mockActiveTourStep",
      appMeta: {},
      token: { TOKEN: "mockToken", URI: "mockUri" } as RundeckToken,
      tokens: {
        mockToken1: { TOKEN: "mockToken1", URI: "mockUri1" },
        mockToken2: { TOKEN: "mockToken2", URI: "mockUri2" },
      },
      navbar: { items: [] as any },
      feature: {},
      data: {},
      rundeckClient: {} as any,
      rootStore: {} as unknown as RootStore,
    };
  });
  afterAll(() => {
    Reflect.deleteProperty(window, '_rundeck');
  });
  const mountNodeFilterLink = async (propsData = {}) => {
    const wrapper = mount(NodeFilterLink, {
      props: {
        filterKey: "tags",
        filterVal: "testTag",
        text: "testText",
        exclude: false,
        ...propsData,
      },

    });
    await wrapper.vm.$nextTick();
    return wrapper;
  };
  it("emits nodefilterclick event with the correct payload when clicked", async () => {
    const wrapper = await mountNodeFilterLink();
    await wrapper.trigger("click");
    await wrapper.vm.$nextTick();
    const emitted = wrapper.emitted("nodefilterclick");
    expect(emitted).toBeTruthy();
    expect(emitted![0][0]).toEqual({
      filter: 'tags: "testTag"',
    });
  });
  it("renders correct href attribute", async () => {
    const wrapper = await mountNodeFilterLink();
    const link = wrapper.find("a");
    const expectedHref = new URL(
      "/project/test-project/nodes?filter=tags%3A%20%22testTag%22",
      "http://localhost",
    ).href;
    expect(link.attributes("href")).toBe(expectedHref);
  });
  it("handles exclude prop correctly", async () => {
    const wrapper = await mountNodeFilterLink({ exclude: true });
    await wrapper.trigger("click");
    await wrapper.vm.$nextTick();
    const emitted = wrapper.emitted("nodefilterclick");
    expect(emitted![0][0]).toEqual({
      filterExclude: 'tags: "testTag"',
    });
  });
  it("constructs filter param correctly based on props", async () => {
    const wrapper = await mountNodeFilterLink();
    const link = wrapper.find("a");
    expect(link.text()).toContain("testText");
  });
  it("handles nodeFilter prop correctly", async () => {
    const wrapper = await mountNodeFilterLink({
      nodeFilter: "hostname: node1",
    });
    await wrapper.trigger("click");
    await wrapper.vm.$nextTick();
    const emitted = wrapper.emitted("nodefilterclick");
    expect(emitted![0][0]).toEqual({
      filter: "hostname: node1",
    });
  });

  describe("getFilter — multi-attribute and quoting", () => {
    const filterCases: [string, string, string][] = [
      // [description, nodeFilter input, expected output]
      [
        "does not quote multi-attribute filter (no-space values)",
        "osFamily: unix name:localhost",
        "osFamily: unix name:localhost",
      ],
      [
        "does not quote multi-attribute filter (space after colon)",
        "osFamily: unix name: localhost",
        "osFamily: unix name: localhost",
      ],
      [
        "does not quote multi-attribute filter with three attributes",
        "name: node1 tags: linux osFamily: unix",
        "name: node1 tags: linux osFamily: unix",
      ],
      [
        "does not double-quote when values are already quoted",
        'osArch: "aarch64" osName: "Mac OS X"',
        'osArch: "aarch64" osName: "Mac OS X"',
      ],
      [
        "does not double-quote single attribute with quoted value",
        'osName: "Mac OS X"',
        'osName: "Mac OS X"',
      ],
      [
        "quotes single attribute whose unquoted value contains spaces",
        "osName: Mac OS X",
        'osName: "Mac OS X"',
      ],
      [
        "does not modify single attribute without spaces",
        "osFamily: unix",
        "osFamily: unix",
      ],
      [
        "does not modify the all-nodes wildcard",
        ".*",
        ".*",
      ],
      [
        "does not quote multi-attribute exclude filter",
        "name: node1 !tags: windows",
        "name: node1 !tags: windows",
      ],
      [
        "does not quote namespace attribute with multi-attribute filter",
        "ui:status:text: active name: node1",
        "ui:status:text: active name: node1",
      ],
    ];

    it.each(filterCases)("%s", async (_, nodeFilter, expected) => {
      const wrapper = await mountNodeFilterLink({ nodeFilter });
      await wrapper.trigger("click");
      await wrapper.vm.$nextTick();
      const emitted = wrapper.emitted("nodefilterclick");
      expect(emitted![0][0]).toEqual({ filter: expected });
    });
  });
});
