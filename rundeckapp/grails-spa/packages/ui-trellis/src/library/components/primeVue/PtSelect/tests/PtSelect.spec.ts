import { mount } from "@vue/test-utils";
import Select from "primevue/select";

import { PtSelect } from "../../index";

const createWrapper = async (props = {}): Promise<any> => {
  const wrapper = mount(PtSelect, {
    props: {
      modelValue: null,
      options: ["Option 1", "Option 2", "Option 3"],
      placeholder: "Select an option",
      ...props,
    },
    global: {
      components: {
        Select: Select,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("PtSelect", () => {
  it("renders the component", async () => {
    const wrapper = await createWrapper();
    expect(wrapper.findComponent(Select).exists()).toBe(true);
  });

  it("updates value and emits 'update:modelValue'", async () => {
    const wrapper = await createWrapper();
    const select = wrapper.findComponent(Select);
    await select.vm.$emit("update:model-value", "Option 1");
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    expect(wrapper.emitted("update:modelValue")[0]).toEqual(["Option 1"]);
  });

  it("emits 'change' when selection changes", async () => {
    const wrapper = await createWrapper();
    const select = wrapper.findComponent(Select);
    await select.vm.$emit("change", { value: "Option 1" });
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted("change")).toBeTruthy();
    expect(wrapper.emitted("change")[0]).toEqual([{ value: "Option 1" }]);
  });

  it("emits 'focus' when input is focused", async () => {
    const wrapper = await createWrapper();
    const select = wrapper.findComponent(Select);
    const focusEvent = new FocusEvent("focus");
    await select.vm.$emit("focus", focusEvent);
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted("focus")).toBeTruthy();
  });

  it("emits 'blur' when input loses focus", async () => {
    const wrapper = await createWrapper();
    const select = wrapper.findComponent(Select);
    const blurEvent = new FocusEvent("blur");
    await select.vm.$emit("blur", blurEvent);
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted("blur")).toBeTruthy();
  });

  it("emits 'show' when dropdown opens", async () => {
    const wrapper = await createWrapper();
    const select = wrapper.findComponent(Select);
    await select.vm.$emit("show");
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted("show")).toBeTruthy();
  });

  it("emits 'hide' when dropdown closes", async () => {
    const wrapper = await createWrapper();
    const select = wrapper.findComponent(Select);
    await select.vm.$emit("hide");
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted("hide")).toBeTruthy();
  });

  it("emits 'filter' when filter is used", async () => {
    const wrapper = await createWrapper({ filter: true });
    const select = wrapper.findComponent(Select);
    await select.vm.$emit("filter", { value: "test" });
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted("filter")).toBeTruthy();
    expect(wrapper.emitted("filter")[0]).toEqual([{ value: "test" }]);
  });

  it("passes placeholder prop correctly", async () => {
    const wrapper = await createWrapper({ placeholder: "Choose option" });
    const select = wrapper.findComponent(Select);
    expect(select.props("placeholder")).toBe("Choose option");
  });

  it("passes disabled prop correctly", async () => {
    const wrapper = await createWrapper({ disabled: true });
    const select = wrapper.findComponent(Select);
    expect(select.props("disabled")).toBe(true);
  });

  it("passes invalid prop correctly", async () => {
    const wrapper = await createWrapper({ invalid: true });
    const select = wrapper.findComponent(Select);
    expect(select.props("invalid")).toBe(true);
  });

  it("passes filter prop correctly", async () => {
    const wrapper = await createWrapper({ filter: true });
    const select = wrapper.findComponent(Select);
    expect(select.props("filter")).toBe(true);
  });

  it("passes showClear prop correctly", async () => {
    const wrapper = await createWrapper({ showClear: true });
    const select = wrapper.findComponent(Select);
    expect(select.props("showClear")).toBe(true);
  });
});
