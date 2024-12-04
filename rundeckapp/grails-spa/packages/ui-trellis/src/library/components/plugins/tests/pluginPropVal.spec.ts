import { mount } from "@vue/test-utils";
import PluginPropVal from "../pluginPropVal.vue";

const createWrapper = (prop = {}, value: string | boolean = "") => {
  return mount(PluginPropVal, {
    props: {
      prop: {
        type: "String",
        ...prop,
      },
      value,
    },
  });
};
describe("PluginPropVal.vue", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });
  it("renders 'Add' or 'Don't Add' for Boolean type properties", () => {
    const wrapperTrue = createWrapper(
      { type: "Boolean", options: { booleanTrueDisplayValue: "Add" } },
      "true",
    );
    expect(wrapperTrue.text()).toBe("Add");
    const wrapperFalse = createWrapper(
      { type: "Boolean", options: { booleanFalseDisplayValue: "Don't Add" } },
      "false",
    );
    expect(wrapperFalse.text()).toBe("Don't Add");
  });
  it("renders the correct icon for Options type with valueDisplayType as 'icon'", () => {
    const wrapper = createWrapper(
      {
        type: "Options",
        options: { valueDisplayType: "icon" },
      },
      "glyphicon-ok",
    );
    const icon = wrapper.find("i");
    expect(icon.classes()).toContain("glyphicon-ok");
  });
  it("displays the mapped label for Select type using selectLabels", () => {
    const wrapper = createWrapper(
      {
        type: "Select",
        selectLabels: { "option-1": "Option One" },
      },
      "option-1",
    );
    expect(wrapper.text()).toBe("Option One");
  });
  it("renders obfuscated text for Password type", () => {
    const wrapper = createWrapper(
      {
        options: { displayType: "PASSWORD" },
      },
      "mypassword",
    );
    expect(wrapper.text()).toBe("••••••••••••");
  });

  it("renders multi-line values with line count when displayType is 'MULTI_LINE'", () => {
    const wrapper = createWrapper(
      {
        options: { displayType: "MULTI_LINE" },
      },
      "line1\nline2\nline3",
    );
    expect(wrapper.text()).toContain("line1");
    expect(wrapper.text()).toContain("line2");
    expect(wrapper.text()).toContain("line3");
  });

  test.each([
    {
      description:
        "renders custom attributes for DYNAMIC_FORM with two attributes",
      prop: { options: { displayType: "DYNAMIC_FORM" } },
      value: JSON.stringify([
        { label: "Attribute 1", value: "Value 1" },
        { label: "Attribute 2", value: "Value 2" },
      ]),
      expectedTexts: ["Attribute 1", "Value 1", "Attribute 2", "Value 2"],
    },
    {
      description:
        "renders custom attributes for DYNAMIC_FORM with one attribute",
      prop: { options: { displayType: "DYNAMIC_FORM" } },
      value: JSON.stringify([{ label: "Attribute 3", value: "Value 3" }]),
      expectedTexts: ["Attribute 3", "Value 3"],
    },
  ])("$description", ({ prop, value, expectedTexts }) => {
    const wrapper = createWrapper(prop, value);
    expectedTexts.forEach((text) => {
      expect(wrapper.text()).toContain(text);
    });
  });
});
