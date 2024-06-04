import { mount, VueWrapper } from "@vue/test-utils";
import DateFilter from "../dateFilter.vue";
import DateTimePicker from "../dateTimePicker.vue";
import _ from "lodash";
let idCounter = 0;
jest.spyOn(_, "uniqueId").mockImplementation(() => `uniqueId_${idCounter++}`);

interface MountOptions {
  props?: Record<string, unknown>;
  slots?: { default?: string };
  global?: {
    stubs?: Record<string, unknown>;
    mocks?: Record<string, unknown>;
  };
}
const mountDateFilter = (options: MountOptions = {}): VueWrapper<any> => {
  return mount(DateFilter, {
    props: {
      modelValue: { enabled: false, datetime: "" },
      ...options.props,
    },
    slots: {
      default: options.slots?.default || "slotContent",
    },
    global: {
      stubs: {
        btn: true,
        DateTimePicker: true,
        Dropdown: {
          template: '<div class="dropdown"><slot name="dropdown"></slot></div>',
        },
      },
      mocks: {
        $t: (msg) => msg,
      },
    },
  });
};
describe("DateFilter", () => {
  beforeEach(() => {
    idCounter = 0;
    jest.spyOn(_, "uniqueId").mockImplementation(() => {
      const id = `uniqueId_${idCounter}`;
      idCounter++;
      return id;
    });
  });
  afterEach(() => {
    jest.clearAllMocks();
  });
  describe("Initialization", () => {
    it("initializes with correct data", () => {
      const wrapper = mountDateFilter();
      expect(wrapper.vm.uid).toBeTruthy();
      expect(wrapper.vm.enabled).toBe(false);
      expect(wrapper.vm.datetime).toBe("");
    });
    it("sets uid correctly", () => {
      const wrapper = mountDateFilter();
      expect(wrapper.vm.uid).toBe(`uniqueId_${idCounter - 1}`);
    });
    it("checkbox is initially in correct state", () => {
      const wrapper = mountDateFilter();
      const checkbox = wrapper.find('input[type="checkbox"]');
      expect((checkbox.element as HTMLInputElement).checked).toBe(false);
    });
  });
  describe("Rendering", () => {
    it("renders checkbox and dropdown based on enabled property", async () => {
      const wrapper = mountDateFilter({
        props: {
          modelValue: {
            enabled: true,
            datetime: "2022-01-02T00:00:00",
          },
        },
      });
      await wrapper.vm.$nextTick();
      const checkbox = wrapper.find('input[type="checkbox"]');
      expect(checkbox.exists()).toBe(true);
      const dropdown = wrapper.find(".dropdown");
      expect(dropdown.exists()).toBe(true);
    });
    it("renders provided slot content in label", async () => {
      const slotContent = "Provided slot content";
      const wrapper = mountDateFilter({ slots: { default: slotContent } });
      await wrapper.vm.$nextTick();
      const label = wrapper.find("label");
      expect(label.text()).toBe(slotContent);
    });
    it("binds datetime to text input", async () => {
      const wrapper = mountDateFilter({
        props: {
          modelValue: {
            enabled: true,
            datetime: "2022-01-01T00:00:00",
          },
        },
      });
      await wrapper.vm.$nextTick();
      const dateTimePicker = wrapper.findComponent({ name: "DateTimePicker" });
      expect(dateTimePicker.exists()).toBe(true);
      expect(dateTimePicker.props("modelValue")).toBe("2022-01-01T00:00:00");
    });
  });
  describe("User Interactions", () => {
    it("toggles enabled state when checkbox is clicked", async () => {
      const wrapper = mountDateFilter();
      const checkbox = wrapper.find('input[type="checkbox"]');
      await checkbox.setValue(true);
      await wrapper.vm.$nextTick();
      expect(wrapper.vm.enabled).toBe(true);
      await checkbox.setValue(false);
      await wrapper.vm.$nextTick();
      expect(wrapper.vm.enabled).toBe(false);
    });
    it("updates datetime when DateTimePicker emits update:modelValue", async () => {
      const wrapper = mountDateFilter({
        props: {
          modelValue: { enabled: true },
        },
      });
      await wrapper.vm.$nextTick();
      const dateTimePicker = wrapper.findComponent(DateTimePicker);
      const newDate = "2022-01-02T00:00:00";
      dateTimePicker.vm.$emit("update:modelValue", newDate);
      await wrapper.vm.$nextTick();
      expect(wrapper.vm.datetime).toBe(newDate);
    });
    it("emits update:modelValue event when enabled changes", async () => {
      const wrapper = mountDateFilter();
      const checkbox = wrapper.find('input[type="checkbox"]');
      await checkbox.setValue(true);
      await wrapper.vm.$nextTick();
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      if (emitted) {
        expect(emitted[0]).toEqual([{ enabled: true, datetime: "" }]);
      }
    });
    it("emits update:modelValue event when datetime changes", async () => {
      const wrapper = mountDateFilter({
        props: {
          modelValue: {
            enabled: true,
            datetime: "2022-01-02T00:00:00",
          },
        },
      });
      await wrapper.vm.$nextTick();
      const dateTimePicker = wrapper.findComponent(DateTimePicker);
      expect(dateTimePicker.exists()).toBe(true);
      await dateTimePicker.vm.$emit("update:modelValue", "2022-01-03T00:00:00");
      await wrapper.vm.$nextTick();
      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toBeTruthy();
      if (emitted) {
        expect(emitted[0]).toEqual([
          { enabled: true, datetime: "2022-01-03T00:00:00" },
        ]);
      }
    });
  });
  describe("Event Emission", () => {
    it("handles custom event and updates state correctly", async () => {
      const newDate = "2022-01-02T00:00:00";
      const wrapper = mountDateFilter({
        props: {
          modelValue: { enabled: true },
        },
      });
      await wrapper.vm.$nextTick();
      const dateTimePicker = wrapper.findComponent(DateTimePicker);
      dateTimePicker.vm.$emit("update:modelValue", newDate);
      await wrapper.vm.$nextTick();
      expect(wrapper.vm.datetime).toBe(newDate);
    });
  });
});
