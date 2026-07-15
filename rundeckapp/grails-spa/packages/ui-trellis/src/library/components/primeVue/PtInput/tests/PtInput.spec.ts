import { mount } from "@vue/test-utils";
import InputText from "primevue/inputtext";
import IconField from "primevue/iconfield";
import InputIcon from "primevue/inputicon";
import PtInput from "../PtInput.vue";

const createWrapper = async (props = {}) => {
  const wrapper = mount(PtInput, {
    props: { modelValue: "", ...props },
    global: { components: { InputText, IconField, InputIcon } },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("PtInput", () => {
  describe("label", () => {
    it("does not show a label when no label prop is given", async () => {
      const wrapper = await createWrapper();
      expect(wrapper.find('[data-testid="pt-input-label"]').exists()).toBe(false);
    });

    it("shows the label text so users know what the field is for", async () => {
      const wrapper = await createWrapper({ label: "Username" });
      const label = wrapper.find('[data-testid="pt-input-label"]');
      expect(label.exists()).toBe(true);
      expect(label.text()).toBe("Username");
    });

    it("links the label to the input via for attribute so clicking the label focuses the field", async () => {
      const wrapper = await createWrapper({ label: "Username", inputId: "username-field" });
      expect(
        wrapper.find('[data-testid="pt-input-label"]').attributes("for"),
      ).toBe("username-field");
    });
  });

  describe("help text", () => {
    it("does not show help text when none is provided", async () => {
      const wrapper = await createWrapper();
      expect(wrapper.find('[data-testid="pt-input-help"]').exists()).toBe(false);
    });

    it("shows the help text below the field so users understand how to fill it", async () => {
      const wrapper = await createWrapper({ helpText: "Enter your full username" });
      const help = wrapper.find('[data-testid="pt-input-help"]');
      expect(help.exists()).toBe(true);
      expect(help.text()).toBe("Enter your full username");
    });
  });

  describe("error message", () => {
    it("does not show an error when the field is valid", async () => {
      const wrapper = await createWrapper({ invalid: false, errorText: "Required" });
      expect(wrapper.find('[data-testid="pt-input-error"]').exists()).toBe(false);
    });

    it("shows the error message text when the field is invalid so users know what to fix", async () => {
      const wrapper = await createWrapper({ invalid: true, errorText: "This field is required" });
      const error = wrapper.find('[data-testid="pt-input-error"]');
      expect(error.exists()).toBe(true);
      expect(error.text()).toBe("This field is required");
    });

    it("does not show an error even when invalid if no errorText is provided", async () => {
      const wrapper = await createWrapper({ invalid: true });
      expect(wrapper.find('[data-testid="pt-input-error"]').exists()).toBe(false);
    });
  });

  describe("typing in the field", () => {
    it("emits the new value when the user types", async () => {
      const wrapper = await createWrapper();
      await wrapper.find('[data-testid="pt-input-field"]').setValue("hello");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:modelValue")).toHaveLength(1);
      expect(wrapper.emitted("update:modelValue")![0]).toEqual(["hello"]);
    });
  });

  describe("focus and blur events", () => {
    it("emits a focus event with the native event when the field gains focus", async () => {
      const wrapper = await createWrapper();
      await wrapper.find('[data-testid="pt-input-field"]').trigger("focus");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("focus")).toHaveLength(1);
      expect(wrapper.emitted("focus")![0][0]).toBeInstanceOf(Event);
    });

    it("emits a blur event with the native event when the field loses focus", async () => {
      const wrapper = await createWrapper();
      await wrapper.find('[data-testid="pt-input-field"]').trigger("blur");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("blur")).toHaveLength(1);
      expect(wrapper.emitted("blur")![0][0]).toBeInstanceOf(Event);
    });

    it("emits an input event with the native event on each keystroke", async () => {
      const wrapper = await createWrapper();
      await wrapper.find('[data-testid="pt-input-field"]').trigger("input");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("input")).toHaveLength(1);
      expect(wrapper.emitted("input")![0][0]).toBeInstanceOf(Event);
    });
  });

  describe("icon variants", () => {
    it("does not render an icon container when no icons are configured", async () => {
      const wrapper = await createWrapper();
      expect(wrapper.find('[data-testid="pt-input-icon-container"]').exists()).toBe(false);
    });

    it("renders an icon container when a left icon is provided so users see a visual indicator", async () => {
      const wrapper = await createWrapper({ leftIcon: "pi pi-search" });
      expect(wrapper.find('[data-testid="pt-input-icon-container"]').exists()).toBe(true);
    });

    it("renders an icon container when a right icon is provided so users see a visual indicator", async () => {
      const wrapper = await createWrapper({ rightIcon: "pi pi-times" });
      expect(wrapper.find('[data-testid="pt-input-icon-container"]').exists()).toBe(true);
    });
  });

  describe("placeholder text", () => {
    it("shows placeholder text inside the empty field", async () => {
      const wrapper = await createWrapper({ placeholder: "Enter your username" });
      expect(
        wrapper.find('[data-testid="pt-input-field"]').attributes("placeholder"),
      ).toBe("Enter your username");
    });
  });
});
