import { mount } from "@vue/test-utils";
import NodeDetailsSimple from "../NodeDetailsSimple.vue";
jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mockRdBase",
    projectName: "test-project",
  }),
  url: jest.fn().mockReturnValue({ href: "mockHref" }),
}));
const mockNodeAttributes = {
  description: "Test Node 1",
  osFamily: "Linux",
  osArch: "x86_64",
  "ui:status:text": "Healthy",
  "ui:status:icon": "fa-check",
};
const mountNodeDetailsSimple = async (propsData = {}) => {
  const wrapper = mount(NodeDetailsSimple, {
    props: {
      attributes: mockNodeAttributes,
      tags: ["Tag1", "Tag2"],
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
describe("NodeDetailsSimple Component", () => {
  it("displays node attributes", async () => {
    const wrapper = await mountNodeDetailsSimple();
    const attributes = wrapper.findAll(".setting");
    const hasExpectedText = attributes.some((attributeWrapper) =>
      attributeWrapper.text().includes("Test Node 1"),
    );
    expect(hasExpectedText).toBe(true);
  });
  it("displays tags", async () => {
    const wrapper = await mountNodeDetailsSimple();
    const tags = wrapper.findAll(".label-muted");
    expect(tags.length).toBe(2);
    expect(tags.at(0).text()).toContain("Tag1");
    expect(tags.at(1).text()).toContain("Tag2");
  });
});
