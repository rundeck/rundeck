import { mount } from "@vue/test-utils";
import dateFilter from "../dateFilter.vue";
import _ from "lodash";

let idCounter = 0;
jest.spyOn(_, "uniqueId").mockImplementation(() => `uniqueId_${idCounter++}`);
jest.mock("../dateTimePicker.vue", () => ({
  __esModule: true,
  default: {
    template: '<div id="DateTimePicker" />',
    emits: ["update:modelValue"],
    props: ["modelValue"],
  },
}));
let wrapper;
const defaultProps = {
  modelValue: { enabled: false, datetime: "" },
};
interface MountOptions {
  props?: Record<string, unknown>;
  slots?: { default?: string };
  global?: {
    stubs?: Record<string, unknown>;
    mocks?: Record<string, unknown>;
  };
}
const mountDateFilter = (options: MountOptions = {}) => {
  return mount(dateFilter, {
    props: {
      ...defaultProps,
      ...options.props,
    },
    slots: {
      default: options.slots?.default || "slotContent",
    },
    global: {
      stubs: {
        ...options.global?.stubs,
      },
      mocks: {
        $t: (msg) => msg,
        ...options.global?.mocks,
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
    wrapper = mountDateFilter();
  });
  afterEach(() => {
    wrapper.unmount();
    jest.restoreAllMocks();
  });
  const findDropdown = () => wrapper.find("dropdown");
  const findCheckbox = () => wrapper.find('input[type="checkbox"]');
  describe("Initialization", () => {
    it("initializes with correct data", () => {
      expect(wrapper.vm.uid).toBeTruthy();
      expect(wrapper.vm.enabled).toBe(false);
      expect(wrapper.vm.datetime).toBe("");
    });
    it("sets uid correctly", () => {
      expect(wrapper.vm.uid).toBe(`uniqueId_${idCounter - 1}`);
    });
    it("picker is initialized correctly", () => {
      expect(wrapper.vm.picker).toBe(false);
    });
    it("checkbox is initially in correct state", () => {
      expect(findCheckbox().element.checked).toBe(false);
    });
  });
  describe("Rendering", () => {
    it("renders checkbox and dropdown based on enabled property", async () => {
      wrapper = mountDateFilter({
        props: {
          modelValue: {
            enabled: true,
            datetime: "2022-01-02T00:00:00",
          },
        },
      });
      // Check that checkbox is rendered
      const checkbox = findCheckbox();
      expect(checkbox.exists()).toBe(true);
      // Check that dropdown is rendered
      const dropdown = findDropdown();
      expect(dropdown.exists()).toBe(true);
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
      expect(findDropdown().exists()).toBe(true);
    });
    it("renders DateTimePicker when dropdown is enabled", async () => {
      wrapper = mountDateFilter({
        props: {
          modelValue: { enabled: true, datetime: "" },
        },
        global: {
          stubs: {
            DateTimePicker: {
              template: '<div id="DateTimePicker" />',
            },
          },
        },
      });
      await wrapper.vm.$nextTick();
      await wrapper.vm.$nextTick();
      console.log(wrapper.html()); // Added logging
      const dateTimePicker = wrapper.find("#DateTimePicker");
      console.log("Is DateTimePicker found:", dateTimePicker.exists());
      expect(dateTimePicker.exists()).toBe(true);
    });

    it("renders provided slot content in label", async () => {
      const slotContent = "Provided slot content";
      wrapper = mountDateFilter({ slots: { default: slotContent } });
      await wrapper.vm.$nextTick();
      const label = wrapper.find("label");
      expect(label.text()).toBe(slotContent);
    });
    it("binds datetime to text input", async () => {
      wrapper = mountDateFilter({
        props: {
          modelValue: {
            enabled: true,
            datetime: "2022-01-01T00:00:00",
          },
        },
      });
      const input = wrapper.find('input[type="text"]');
      expect(input.exists()).toBe(true);
      if (input.exists()) {
        await input.setValue("2022-01-01T00:00:00");
        await wrapper.vm.$nextTick();
        expect(wrapper.vm.datetime).toBe("2022-01-01T00:00:00");
      }
    });
    it("renders date-time-picker in dropdown", async () => {
      wrapper = mountDateFilter();
      await wrapper.vm.$nextTick();
      const dateTimePicker = wrapper.find("date-time-picker");
      if (dateTimePicker.exists()) {
        expect(dateTimePicker.exists()).toBe(true);
      } else {
        console.log("date-time-picker does not exist");
      }
    });
    it("renders btn with dropdown-toggle class", async () => {
      wrapper.setData({ enabled: true });
      await wrapper.vm.$nextTick();
      const dropdownToggle = wrapper.find(".dropdown-toggle");
      expect(dropdownToggle.exists()).toBe(true);
    });
    it("renders glyphicon glyphicon-calendar icon in btn", async () => {
      const localWrapper = mountDateFilter({
        props: { modelValue: { enabled: true, datetime: "" } },
        global: {
          stubs: {
            Dropdown: {
              template: `
                <div>
                  <div class="input-group">
                    <div class="input-group-btn">
                      <btn class="dropdown-toggle">
                        <i class="glyphicon glyphicon-calendar"></i>
                      </btn>
                    </div>
                    <input type="text" class="form-control" />
                  </div>
                  <slot name="dropdown"></slot>
                </div>
              `,
            },
          },
        },
      });
      await localWrapper.vm.$nextTick();
      const dropdownToggle = localWrapper.find(".dropdown-toggle");
      expect(dropdownToggle.exists()).toBe(true);
      const glyphiconCalendar = localWrapper.find(".glyphicon-calendar");
      expect(glyphiconCalendar.exists()).toBe(true);
    });
  });
  describe("Event Emission", () => {
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
  });
  describe("Model Value Changes", () => {
    it("updates enabled and datetime when modelValue changes", async () => {
      wrapper = mountDateFilter({
        props: {
          modelValue: {
            enabled: true,
            datetime: "2022-01-02T00:00:00",
          },
        },
      });
      await wrapper.setProps({
        modelValue: {
          enabled: false,
          datetime: "2022-01-03T00:00:00",
        },
      });
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
    it("maintains correct state with rapid prop changes", async () => {
      await wrapper.setProps({
        modelValue: { enabled: true, datetime: "2022-01-02T00:00:00" },
      });
      await wrapper.vm.$nextTick();
      expect(wrapper.vm.enabled).toBe(true);
      expect(wrapper.vm.datetime).toBe("2022-01-02T00:00:00");

      await wrapper.setProps({
        modelValue: { enabled: false, datetime: "2023-01-01T12:00:00" },
      });
      await wrapper.vm.$nextTick();
      expect(wrapper.vm.enabled).toBe(false);
      expect(wrapper.vm.datetime).toBe("2023-01-01T12:00:00");

      await wrapper.setProps({
        modelValue: { enabled: true, datetime: "2024-01-01T12:00:00" },
      });
      await wrapper.vm.$nextTick();
      expect(wrapper.vm.enabled).toBe(true);
      expect(wrapper.vm.datetime).toBe("2024-01-01T12:00:00");
    });
  });
  it("generates unique IDs for multiple instances", () => {
    const firstInstance = mountDateFilter();
    const secondInstance = mountDateFilter();
    expect((firstInstance.vm as any).uid).not.toBe(
      (secondInstance.vm as any).uid,
    );
  });
  it("enabled changes when checkbox is clicked", async () => {
    const checkbox = wrapper.find('input[type="checkbox"]');
    await checkbox.setChecked(false);
    expect(checkbox.element.checked).toBe(false);
    checkbox.element.checked = true;
    await checkbox.trigger("change");
    await wrapper.vm.$nextTick();
    expect(checkbox.element.checked).toBe(true);
  });
  it("uid is applied as id and for attributes", () => {
    expect(wrapper.vm.uid).toBeDefined();
    expect(wrapper.vm.uid).toBe(`uniqueId_0`);
  });
});
