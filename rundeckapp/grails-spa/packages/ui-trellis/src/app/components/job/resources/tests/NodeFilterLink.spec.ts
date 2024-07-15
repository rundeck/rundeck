import { mount } from "@vue/test-utils";
import NodeFilterLink from "../NodeFilterLink.vue";
import { getRundeckContext, url } from "../../../../../library";
jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({}),
  url: jest.fn().mockReturnValue({ href: "mockHref" }),
}));

beforeEach(() => {
  delete window._rundeck;

  // Set the rdBase in the window._rundeck object
  window._rundeck = {
    activeTour: "",
    activeTourStep: "",
    apiVersion: "",
    appMeta: {},
    data: {},
    eventBus: undefined,
    feature: {},
    navbar: { items: undefined },
    projectName: "",
    rootStore: undefined,
    rundeckClient: undefined,
    token: undefined,
    tokens: {},
    rdBase: "mockRdBase",
  };
});

afterEach(() => {
  // Clean up the window._rundeck object after each test to ensure no test pollution
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
describe("NodeFilterLink Component", () => {
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
    expect(link.attributes("href")).toBe("mockHref");
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
