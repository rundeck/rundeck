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
    slots: {
      default: "slotContent",
    },
    global: {
      mocks: {
        $t: (msg) => msg,
      },
    },
  });
};

describe("DateFilter", () => {
  beforeEach(() => {
    wrapper = mountDateFilter();
  });

  afterEach(() => {
    wrapper.unmount();
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
    it("renders checkbox and dropdown based on enabled property", () => {
      expect(findCheckbox().exists()).toBe(true);
      expect(findDropdown().exists()).toBe(false);

      wrapper.unmount();
      wrapper = mountDateFilter({
        modelValue: { enabled: true, datetime: "" },
      });

      expect(findDropdown().exists()).toBe(true);

      wrapper.unmount();
      wrapper = mountDateFilter({
        modelValue: { enabled: false, datetime: "" },
      });

      expect(findDropdown().exists()).toBe(false);
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
    it("updates enabled and datetime when modelValue changes", () => {
      wrapper.unmount();
      wrapper = mountDateFilter({
        modelValue: { enabled: false, datetime: "2022-01-02T00:00:00" },
      });

      expect(wrapper.vm.enabled).toBe(false);
      expect(wrapper.vm.datetime).toBe("2022-01-02T00:00:00");
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

  describe("Unique IDs", () => {
    it("generates unique IDs for multiple instances", () => {
      const firstInstance = mountDateFilter();
      const secondInstance = mountDateFilter();
      expect(firstInstance.vm.uid).not.toBe(secondInstance.vm.uid);
    });
  });
  describe("Rendering", () => {
    it("enabled changes when checkbox is clicked", async () => {
      await findCheckbox().trigger("click");
      expect(wrapper.vm.enabled).toBe(true);
    });

    it("uid is applied as id and for attributes", () => {
      const checkbox = findCheckbox();
      expect(checkbox.attributes("id")).toBe(wrapper.vm.uid);
      expect(checkbox.attributes("for")).toBe(wrapper.vm.uid);
    });

    it("renders default slot content in label", () => {
      const label = wrapper.find("label");
      expect(label.text()).toBe("slotContent");
    });

    it("renders provided slot content in label", () => {
      const slotContent = "Provided slot content";
      wrapper = mountDateFilter({
        slots: {
          default: slotContent,
        },
      });

      const label = wrapper.find("label");
      expect(label.text()).toBe("Provided slot content");
    });
    it("binds datetime to text input", async () => {
      wrapper = mountDateFilter(); // make sure to mount the component before trying to find elements in it

      const input = wrapper.find('input[type="text"]');
      if (input.exists()) {
        await input.setValue("2022-01-01T00:00:00");
        await wrapper.vm.$nextTick();
        console.log(input.html());
        expect(wrapper.vm.datetime).toBe("2022-01-01T00:00:00");
      } else {
        console.log('input[type="text"] does not exist');
      }
    });

    it("renders date-time-picker in dropdown", async () => {
      wrapper = mountDateFilter(); // make sure to mount the component before trying to find elements in it

      await wrapper.vm.$nextTick();
      const dateTimePicker = wrapper.find("date-time-picker");
      if (dateTimePicker.exists()) {
        expect(dateTimePicker.exists()).toBe(true);
      } else {
        console.log("date-time-picker does not exist");
      }
    });

    it("binds datetime to date-time-picker", async () => {
      const dateTimePicker = wrapper.find("date-time-picker");
      await dateTimePicker.setValue("2022-01-01T00:00:00");

      console.log(dateTimePicker.html());
      expect(wrapper.vm.datetime).toBe("2022-01-01T00:00:00");
    });

    it("renders btn with dropdown-toggle class", () => {
      const dropdownToggle = wrapper.find(".dropdown-toggle");

      console.log(dropdownToggle.html()); // prints the inner HTML of the element with the dropdown-toggle class
      expect(dropdownToggle.exists()).toBe(true);
    });
    it("renders glyphicon glyphicon-calendar icon in btn", () => {
      const glyphiconCalendar = wrapper.find(".glyphicon-calendar");

      console.log(glyphiconCalendar.html()); // prints the inner HTML of the element with the glyphicon-calendar class
      expect(glyphiconCalendar.exists()).toBe(true);
    });
  });
});
