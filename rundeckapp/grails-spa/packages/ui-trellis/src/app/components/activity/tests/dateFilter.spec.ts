import { mount } from "@vue/test-utils";
import dateFilter from "../dateFilter.vue";
import _ from "lodash";

let idCounter = 0;
jest.spyOn(_, "uniqueId").mockImplementation(() => `uniqueId_${idCounter++}`);

let wrapper;
const defaultProps = {
  modelValue: { enabled: false, datetime: "" },
};

const mountDateFilter = (propsData = {}) => {
  return mount(dateFilter, {
    props: {
      ...defaultProps,
      ...propsData,
    },
    global: {
      mocks: {
        $t: (msg) => msg,
      },
    },
  });
};

describe("dateFilter.vue", () => {
  beforeEach(() => {
    wrapper = mountDateFilter();
  });

  afterEach(() => {
    jest.clearAllMocks();
    if (wrapper) {
      wrapper.unmount();
    }
  });

  const findDropdown = () => wrapper.find("dropdown");
  const findCheckbox = () => wrapper.find('input[type="checkbox"]');

  it("initializes with correct data", () => {
    expect(wrapper.vm.uid).toBeTruthy();
    expect(wrapper.vm.enabled).toBe(false);
    expect(wrapper.vm.datetime).toBe("");
  });

  it("renders checkbox and dropdown based on enabled property", () => {
    expect(findCheckbox().exists()).toBe(true);
    expect(findDropdown().exists()).toBe(false);

    wrapper.unmount();
    wrapper = mountDateFilter({ modelValue: { enabled: true, datetime: "" } });

    expect(findDropdown().exists()).toBe(true);

    wrapper.unmount();
    wrapper = mountDateFilter({ modelValue: { enabled: false, datetime: "" } });

    expect(findDropdown().exists()).toBe(false);
  });
  it("emits update:modelValue event when enabled changes", async () => {
    await wrapper.setProps({ modelValue: { enabled: true, datetime: "" } });

    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    expect(wrapper.emitted("update:modelValue")[0]).toEqual([
      { enabled: true, datetime: "" },
    ]);
  });

  it("emits update:modelValue event when datetime prop changes", async () => {
    await wrapper.setProps({
      modelValue: { enabled: true, datetime: "2022-01-02T00:00:00" },
    });

    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    expect(wrapper.emitted("update:modelValue")[0]).toEqual([
      { enabled: true, datetime: "2022-01-02T00:00:00" },
    ]);
  });

  it("updates enabled and datetime when modelValue changes", () => {
    wrapper.unmount();
    wrapper = mountDateFilter({
      modelValue: { enabled: false, datetime: "2022-01-02T00:00:00" },
    });

    expect(wrapper.vm.enabled).toBe(false);
    expect(wrapper.vm.datetime).toBe("2022-01-02T00:00:00");
  });

  it("sets uid correctly", () => {
    expect(wrapper.vm.uid).toBe(`uniqueId_${idCounter - 1}`);
  });

  it("picker is initialized correctly", () => {
    expect(wrapper.vm.picker).toBe(false);
  });

  it("renders checkbox and label", () => {
    const checkboxSpan = wrapper.find('[data-testid="checkbox-span"]');
    expect(checkboxSpan.exists()).toBe(true);
    expect(checkboxSpan.find('input[type="checkbox"]').exists()).toBe(true);
    expect(checkboxSpan.find("label").exists()).toBe(true);
  });

  it("does not render dropdown when checkbox is not checked", async () => {
    await wrapper.setProps({ modelValue: { enabled: false, datetime: "" } });
    await wrapper.vm.$nextTick();

    expect(findDropdown().exists()).toBe(false);
  });
  it("renders dropdown when checkbox is checked", async () => {
    await wrapper.setProps({ modelValue: { enabled: true, datetime: "" } });
    await wrapper.vm.$nextTick();
    const findDropdown = () => wrapper.find("dropdown");
    expect(findDropdown().exists()).toBe(true);
  });

  it("emits update:modelValue when datetime data changes", async () => {
    await wrapper.setProps({
      modelValue: { enabled: false, datetime: "2022-01-02T00:00:00" },
    });
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    expect(wrapper.emitted("update:modelValue")[0]).toEqual([
      { enabled: false, datetime: "2022-01-02T00:00:00" },
    ]);
  });
  it("passes through invalid datetime inputs as-is", async () => {
    const invalidDate = "not a date";
    await wrapper.setProps({
      modelValue: { enabled: true, datetime: invalidDate },
    });
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.datetime).toBe(invalidDate);
    expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    expect(wrapper.emitted("update:modelValue")[0]).toEqual([
      { enabled: true, datetime: invalidDate },
    ]);
  });
  it("generates unique IDs for multiple instances", () => {
    const firstInstance = mountDateFilter();
    const secondInstance = mountDateFilter();
    expect(firstInstance.vm.uid).not.toBe(secondInstance.vm.uid);
  });
  it("maintains correct state with rapid prop changes", async () => {
    await wrapper.setProps({
      modelValue: { enabled: true, datetime: "2022-01-02T00:00:00" },
    });
    await wrapper.vm.$nextTick();

    await wrapper.setProps({
      modelValue: { enabled: false, datetime: "2023-01-01T12:00:00" },
    });
    await wrapper.vm.$nextTick();
    await wrapper.setProps({
      modelValue: { enabled: true, datetime: "2024-01-01T12:00:00" },
    });
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.enabled).toBe(true);
    expect(wrapper.vm.datetime).toBe("2024-01-01T12:00:00");
  });
});
