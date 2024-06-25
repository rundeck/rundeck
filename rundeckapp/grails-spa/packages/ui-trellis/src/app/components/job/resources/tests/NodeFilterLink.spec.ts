import { mount, VueWrapper } from "@vue/test-utils";
import NodeFilterLink from "../NodeFilterLink.vue";

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mockRdBase",
    projectName: "test-project",
  }),
  url: jest.fn().mockReturnValue({ href: "mockHref" }),
}));

describe("NodeFilterLink Component", () => {
  let wrapper: VueWrapper<any>;
  const mountNodeFilterLink = (propsData = {}) => {
    wrapper = mount(NodeFilterLink, {
      props: {
        nodeFilterName: "testFilterName",
        nodeFilter: "testFilter",
        filterKey: "tags",
        filterVal: "testTag",
        text: "testText",
        exclude: false,
        ...propsData,
      },
    });
    return wrapper;
  };
  it("emits nodefilterclick event with the correct payload when clicked", async () => {
    const wrapper = mountNodeFilterLink();
    await wrapper.trigger("click");
    expect(wrapper.emitted().nodefilterclick).toBeTruthy();
    expect(wrapper.emitted().nodefilterclick[0][0]).toEqual({
      filter: "testFilter",
      filterName: "testFilterName",
    });
  });

  it("renders correct href attribute", () => {
    const wrapper = mountNodeFilterLink();
    const link = wrapper.find("a");
    expect(link.attributes("href")).toBe("mockHref");
  });
  it("renders the correct text", () => {
    const wrapper = mountNodeFilterLink();
    expect(wrapper.text()).toContain("testText");
  });
  it("handles exclude prop correctly", async () => {
    const wrapper = mountNodeFilterLink({ exclude: true });
    await wrapper.trigger("click");
    expect(wrapper.emitted().nodefilterclick[0][0]).toEqual({
      filterExclude: "testFilter",
      filterNameExclude: "testFilterName",
    });
  });
  it("constructs filter param correctly based on props", () => {
    const wrapper = mountNodeFilterLink();
    expect(wrapper.vm.filterParamValues).toEqual({
      filter: "testFilter",
      filterName: "testFilterName",
    });
  });

  it("calls getText method and renders the correct text", () => {
    const wrapper = mountNodeFilterLink({ text: "Custom Text" });
    expect(wrapper.vm.getText()).toBe("Custom Text");
    expect(wrapper.text()).toContain("Custom Text");
  });
  it("calls getFilter method and constructs correct filter string", () => {
    const wrapper = mountNodeFilterLink();
    expect(wrapper.vm.getFilter()).toBe("testFilter");
  });
});
