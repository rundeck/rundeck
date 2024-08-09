import { mount } from "@vue/test-utils";
import NodeDetailsSimple from "../NodeDetailsSimple.vue";

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mockRdBase",
    projectName: "test-project",
  }),
  url: jest.fn().mockReturnValue({ href: "mockHref" }),
}));
const mockAttributes = {
  osFamily: "Linux",
  osName: "Ubuntu",
  osVersion: "20.04",
  osArch: "x86_64",
  description: "Test Node 1",
  "ui:status:text": "Healthy",
  "ui:status:icon": "fa-check",
  "ns1:attr1": "value1",
  "ns1:attr2": "value2",
  "ns1:attr3": "value3",
  "ns1:attr4": "value4",
};
const mountNodeDetailsSimple = async (propsData = {}) => {
  const wrapper = mount(NodeDetailsSimple, {
    props: {
      attributes: mockAttributes,
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
  it("renders node attributes", async () => {
    const wrapper = await mountNodeDetailsSimple();
    const attributes = wrapper.findAll(".setting");
    const attributeTexts = attributes.map((attr) => attr.text());
    const expectedAttributes = {
      osFamily: "Linux",
      osName: "Ubuntu",
      osVersion: "20.04",
      osArch: "x86_64",
      description: "Test Node 1",
      "ui:status:text": "Healthy",
      "ui:status:icon": "fa-check",
      "ns1:attr1": "value1",
      "ns1:attr2": "value2",
      "ns1:attr3": "value3",
      "ns1:attr4": "value4",
    };
    Object.entries(expectedAttributes).forEach(([key, value]) => {
      const attributeFound = attributeTexts.some((text) =>
        text.includes(value),
      );
      expect(attributeFound).toBe(true);
    });
  });
  it("renders tags", async () => {
    const wrapper = await mountNodeDetailsSimple();
    const tags = wrapper.findAll(".label-muted");
    expect(tags.length).toBe(2);
    expect(tags.at(0).text()).toContain("Tag1");
    expect(tags.at(1).text()).toContain("Tag2");
  });
  it("renders expandable attributes using class", async () => {
    const wrapper = await mountNodeDetailsSimple();
    const toggleButton = wrapper.find(".textbtn");
    await toggleButton.trigger("click");
    await wrapper.vm.$nextTick();
    // Adjust the selector if necessary based on the actual HTML structure
    const expandedAttributes = wrapper.findAll(".hover-action-holder");
    expect(expandedAttributes.length).toBe(11);
  });
});
