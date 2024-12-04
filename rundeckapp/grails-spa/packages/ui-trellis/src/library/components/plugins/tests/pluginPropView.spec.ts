import { mount } from "@vue/test-utils";
import PluginPropView from "../pluginPropView.vue";
import PluginPropVal from "../pluginPropVal.vue";
import Expandable from "../../utils/Expandable.vue";
import AceEditor from "../../utils/AceEditor.vue";
const createWrapper = (props = {}) => {
  return mount(PluginPropView, {
    props: {
      prop: {
        type: "Boolean",
        title: "Enable Feature",
        desc: "Enable or disable the feature",
        options: {
          booleanTrueDisplayValueClass: "text-success",
          booleanFalseDisplayValueClass: "text-danger",
        },
      },
      value: "true",
      ...props,
    },
    global: {
      components: { PluginPropVal, Expandable, AceEditor },
    },
  });
};
describe("pluginPropView.vue", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });
  it("renders a boolean property with correct title and 'true' value styling", async () => {
    const wrapper = createWrapper();
    const title = wrapper.find("[title='Enable or disable the feature']");
    expect(title.text()).toContain("Enable Feature:");
    const value = wrapper.find(".text-success");
    expect(value.text()).toContain("yes");
  });
  it("renders an Integer property with correct title and value", async () => {
    const wrapper = createWrapper({
      prop: {
        type: "Integer",
        title: "Max Count",
        desc: "Maximum count allowed",
      },
      value: "10",
    });
    const title = wrapper.find('[data-testid="integer-prop-title"]');
    expect(title.text()).toContain("Max Count:");
    const value = wrapper.find('[data-testid="integer-prop-value"]');
    expect(value.text()).toBe("10");
  });
  it("renders options type property with multiple selected values", async () => {
    const wrapper = createWrapper({
      prop: {
        type: "Options",
        title: "Options",
        desc: "Select options",
      },
      value: "Option1, Option2",
    });
    const title = wrapper.find("[title='Select options']");
    expect(title.text()).toContain("Options:");
    const selectedValues = wrapper.findAll(".text-success");
    expect(selectedValues.length).toBe(2);
    expect(selectedValues[0].text()).toContain("Option1");
    expect(selectedValues[1].text()).toContain("Option2");
  });
  it("renders multi-line values using the Expandable component", async () => {
    const wrapper = createWrapper({
      prop: {
        type: "String",
        title: "Multiline Value",
        desc: "Description for multiline",
        options: { displayType: "MULTI_LINE" },
      },
      value: "Line 1\nLine 2\nLine 3",
    });
    const expandable = wrapper.findComponent(Expandable);
    const label = expandable.find("[title='Description for multiline']");
    expect(label.text()).toContain("Multiline Value:");
    const lineCount = expandable.find(".text-info");
    expect(lineCount.text()).toContain("3 lines");
  });
  it("renders ACE Editor for code syntax mode", async () => {
    const wrapper = createWrapper({
      prop: {
        type: "String",
        title: "Code Snippet",
        desc: "Code description",
        options: { displayType: "CODE", codeSyntaxMode: "javascript" },
      },
      value: "console.log('Hello World');",
    });
    const expandable = wrapper.findComponent(Expandable);
    const aceEditor = expandable.findComponent(AceEditor);
    expect(aceEditor.props("lang")).toBe("javascript");
    expect(aceEditor.props("readOnly")).toBe(true);
    expect(aceEditor.props("modelValue")).toBe("console.log('Hello World');");
  });
  it("renders DYNAMIC_FORM type property with custom attributes", async () => {
    const wrapper = createWrapper({
      prop: {
        type: "String",
        title: "Dynamic Form",
        desc: "Dynamic description",
        options: { displayType: "DYNAMIC_FORM" },
      },
      value: JSON.stringify([
        { label: "Custom Label 1", value: "Custom Value 1" },
        { label: "Custom Label 2", value: "Custom Value 2" },
      ]),
    });
    const customPairs = wrapper.findAll('[data-testid="configpair"]');
    expect(customPairs.length).toBe(2);
    expect(customPairs[0].text()).toContain("Custom Label 1:");
    expect(customPairs[0].text()).toContain("Custom Value 1");
    expect(customPairs[1].text()).toContain("Custom Label 2:");
    expect(customPairs[1].text()).toContain("Custom Value 2");
  });
  it("renders password type with obfuscated value", async () => {
    const wrapper = createWrapper({
      prop: {
        type: "String",
        title: "Password",
        desc: "Sensitive data",
        options: { displayType: "PASSWORD" },
      },
      value: "secret",
    });
    const title = wrapper.find("[title='Sensitive data']");
    expect(title.exists()).toBe(true);
    expect(title.text()).toContain("Password:");
    const obfuscatedText = wrapper.find(".text-success");
    expect(obfuscatedText.text()).toBe("••••••••••••");
  });
  it("renders boolean true value with an icon and custom class", async () => {
    const wrapper = createWrapper({
      prop: {
        type: "Boolean",
        title: "Show Icon",
        desc: "Boolean property with icon",
        options: {
          booleanTrueDisplayValueClass: "text-success glyphicon glyphicon-ok",
        },
      },
      value: "true",
    });
    const title = wrapper.find("[title='Boolean property with icon']");
    expect(title.text()).toContain("Show Icon:");
    const icon = wrapper.find(".glyphicon.glyphicon-ok");
    expect(icon.classes()).toContain("text-success");
  });
});
