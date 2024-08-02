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
    delete window._rundeck;
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
      global: {
        mocks: {
          $t: (msg) => msg,
        },
      },
    });
    await wrapper.vm.$nextTick();
    return wrapper;
  };
  it("emits nodefilterclick event with the correct payload when clicked", async () => {
    const wrapper = await mountNodeFilterLink();
    await wrapper.trigger("click");
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted().nodefilterclick).toBeTruthy();
    expect(wrapper.emitted().nodefilterclick[0][0]).toEqual({
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
    expect(wrapper.emitted().nodefilterclick[0][0]).toEqual({
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
    expect(wrapper.emitted().nodefilterclick[0][0]).toEqual({
      filter: "hostname: node1",
    });
  });
});
