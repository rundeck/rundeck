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
    global: {
      mocks: {
        $t: (msg: string) => msg,
      },
    },
  });
};
describe("PluginPropVal.vue", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });
  it("renders 'Yes' or 'No' for Boolean type properties", () => {
    const wrapperTrue = createWrapper(
      { type: "Boolean", options: { booleanTrueDisplayValue: "Yes" } },
      "true",
    );
    expect(wrapperTrue.text()).toBe("Yes");
    const wrapperFalse = createWrapper(
      { type: "Boolean", options: { booleanFalseDisplayValue: "No" } },
      "false",
    );
    expect(wrapperFalse.text()).toBe("No");
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

  it("renders custom attributes for DYNAMIC_FORM type", () => {
    const wrapper = createWrapper(
      {
        options: { displayType: "DYNAMIC_FORM" },
      },
      JSON.stringify([
        { label: "Attribute 1", value: "Value 1" },
        { label: "Attribute 2", value: "Value 2" },
      ]),
    );
    expect(wrapper.text()).toContain("Attribute 1");
    expect(wrapper.text()).toContain("Value 1");
    expect(wrapper.text()).toContain("Attribute 2");
    expect(wrapper.text()).toContain("Value 2");
  });
});
