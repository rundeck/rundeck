import { mount, VueWrapper } from "@vue/test-utils";
import HomeSearchBar from "../HomeSearchBar.vue";

const mountHomeSearchBar = async (
  props?: Record<string, any>,
): Promise<VueWrapper<any>> => {
  return mount(HomeSearchBar, {
    props: {
      placeholder: "Search",
      ...props,
    },
  });
};

describe("HomeSearchBar", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders the input with the correct placeholder", async () => {
    const placeholder = "Custom Placeholder";
    const wrapper = await mountHomeSearchBar({ placeholder });

    const inputElement = wrapper.find("input");
    expect(inputElement.attributes("placeholder")).toBe("Custom Placeholder");
  });

  it('emits "update:modelValue" event when search input changes', async () => {
    const wrapper = await mountHomeSearchBar();

    await wrapper.setData({ search: "Test Input" });

    expect(wrapper.emitted("update:modelValue")).toHaveLength(1);
    expect(wrapper.emitted("update:modelValue")[0]).toEqual(["Test Input"]);
  });

  it('emits "onEnter" event when enter key is pressed in the search input', async () => {
    const wrapper = await mountHomeSearchBar();
    const inputElement = wrapper.find("input");

    await inputElement.setValue("Test Input");
    await inputElement.trigger("keyup.enter");

    expect(wrapper.emitted("onEnter")).toHaveLength(1);
  });

  it('emits "onBlur" event when the search input loses focus', async () => {
    const wrapper = await mountHomeSearchBar();
    const inputElement = wrapper.find("input");

    await inputElement.trigger("blur");

    expect(wrapper.emitted("onBlur")).toHaveLength(1);
  });

  it('emits "onFocus" event when the search input is focused', async () => {
    const wrapper = await mountHomeSearchBar();
    const inputElement = wrapper.find("input");

    await inputElement.trigger("focus");

    expect(wrapper.emitted("onFocus")).toHaveLength(1);
  });

  it("updates search value when modelValue prop changes", async () => {
    const wrapper = await mountHomeSearchBar();
    await wrapper.setProps({ modelValue: "New Model Value" });

    expect(wrapper.vm.search).toBe("New Model Value");
  });
});
