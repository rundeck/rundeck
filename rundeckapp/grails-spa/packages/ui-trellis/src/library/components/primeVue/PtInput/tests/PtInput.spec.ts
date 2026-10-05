import { mount } from "@vue/test-utils";
import InputText from "primevue/inputtext";
import Textarea from "primevue/textarea";
import IconField from "primevue/iconfield";
import InputIcon from "primevue/inputicon";
import PtInput from "../PtInput.vue";

const createWrapper = async (props = {}) => {
  const wrapper = mount(PtInput, {
    props: { modelValue: "", ...props },
    global: { components: { InputText, Textarea, IconField, InputIcon } },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("PtInput", () => {
  describe("label", () => {
    it("does not show a label when no label prop is given", async () => {
      const wrapper = await createWrapper();
      expect(wrapper.find('[data-testid="pt-input-label"]').exists()).toBe(
        false,
      );
    });

    it("shows the label text so users know what the field is for", async () => {
      const wrapper = await createWrapper({ label: "Username" });
      const label = wrapper.find('[data-testid="pt-input-label"]');
      expect(label.exists()).toBe(true);
      expect(label.text()).toBe("Username");
    });

    it("links the label to the input via for attribute so clicking the label focuses the field", async () => {
      const wrapper = await createWrapper({
        label: "Username",
        inputId: "username-field",
      });
      expect(
        wrapper.find('[data-testid="pt-input-label"]').attributes("for"),
      ).toBe("username-field");
    });
  });

  describe("help text", () => {
    it("does not show help text when none is provided", async () => {
      const wrapper = await createWrapper();
      expect(wrapper.find('[data-testid="pt-input-help"]').exists()).toBe(
        false,
      );
    });

    it("shows the help text below the field so users understand how to fill it", async () => {
      const wrapper = await createWrapper({
        helpText: "Enter your full username",
      });
      const help = wrapper.find('[data-testid="pt-input-help"]');
      expect(help.exists()).toBe(true);
      expect(help.text()).toBe("Enter your full username");
    });
  });

  describe("error message", () => {
    it("does not show an error when the field is valid", async () => {
      const wrapper = await createWrapper({
        invalid: false,
        errorText: "Required",
      });
      expect(wrapper.find('[data-testid="pt-input-error"]').exists()).toBe(
        false,
      );
    });

    it("shows the error message text when the field is invalid so users know what to fix", async () => {
      const wrapper = await createWrapper({
        invalid: true,
        errorText: "This field is required",
      });
      const error = wrapper.find('[data-testid="pt-input-error"]');
      expect(error.exists()).toBe(true);
      expect(error.text()).toBe("This field is required");
    });

    it("does not show an error even when invalid if no errorText is provided", async () => {
      const wrapper = await createWrapper({ invalid: true });
      expect(wrapper.find('[data-testid="pt-input-error"]').exists()).toBe(
        false,
      );
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
      expect(
        wrapper.find('[data-testid="pt-input-icon-container"]').exists(),
      ).toBe(false);
    });

    it("renders an icon container when a left icon is provided so users see a visual indicator", async () => {
      const wrapper = await createWrapper({ leftIcon: "pi pi-search" });
      expect(
        wrapper.find('[data-testid="pt-input-icon-container"]').exists(),
      ).toBe(true);
    });

    it("renders an icon container when a right icon is provided so users see a visual indicator", async () => {
      const wrapper = await createWrapper({ rightIcon: "pi pi-times" });
      expect(
        wrapper.find('[data-testid="pt-input-icon-container"]').exists(),
      ).toBe(true);
    });
  });

  describe("placeholder text", () => {
    it("shows placeholder text inside the empty field", async () => {
      const wrapper = await createWrapper({
        placeholder: "Enter your username",
      });
      expect(
        wrapper
          .find('[data-testid="pt-input-field"]')
          .attributes("placeholder"),
      ).toBe("Enter your username");
    });
  });

  describe("multiline", () => {
    it("renders a single-line input by default", async () => {
      const wrapper = await createWrapper();

      expect(wrapper.find('[data-testid="pt-input-field"]').exists()).toBe(
        true,
      );
      expect(wrapper.find('[data-testid="pt-input-textarea"]').exists()).toBe(
        false,
      );
    });

    it("renders a textarea instead of the single-line input when multiline", async () => {
      const wrapper = await createWrapper({ multiline: true });

      expect(
        wrapper.find('[data-testid="pt-input-textarea"]').element.tagName,
      ).toBe("TEXTAREA");
      expect(wrapper.find('[data-testid="pt-input-field"]').exists()).toBe(
        false,
      );
    });

    it("shows 3 lines by default and the given number of rows otherwise", async () => {
      const defaults = await createWrapper({ multiline: true });
      const custom = await createWrapper({ multiline: true, rows: 5 });

      expect(
        defaults.find('[data-testid="pt-input-textarea"]').attributes("rows"),
      ).toBe("3");
      expect(
        custom.find('[data-testid="pt-input-textarea"]').attributes("rows"),
      ).toBe("5");
    });

    it("emits the new value when the user types", async () => {
      const wrapper = await createWrapper({ multiline: true });
      await wrapper
        .find('[data-testid="pt-input-textarea"]')
        .setValue("line one\nline two");

      expect(wrapper.emitted("update:modelValue")).toHaveLength(1);
      expect(wrapper.emitted("update:modelValue")![0]).toEqual([
        "line one\nline two",
      ]);
    });

    it("emits focus, blur and input events with the native event", async () => {
      const wrapper = await createWrapper({ multiline: true });
      const textarea = wrapper.find('[data-testid="pt-input-textarea"]');

      await textarea.trigger("focus");
      await textarea.trigger("blur");
      await textarea.trigger("input");

      expect(wrapper.emitted("focus")![0][0]).toBeInstanceOf(Event);
      expect(wrapper.emitted("blur")![0][0]).toBeInstanceOf(Event);
      expect(wrapper.emitted("input")![0][0]).toBeInstanceOf(Event);
    });

    it("shows the placeholder, the label tied to the textarea, and the help text", async () => {
      const wrapper = await createWrapper({
        multiline: true,
        placeholder: "Add a note",
        label: "Notes",
        helpText: "Optional",
        inputId: "notes-field",
      });

      const textarea = wrapper.find('[data-testid="pt-input-textarea"]');
      expect(textarea.attributes("placeholder")).toBe("Add a note");
      expect(textarea.attributes("id")).toBe("notes-field");
      expect(
        wrapper.find('[data-testid="pt-input-label"]').attributes("for"),
      ).toBe("notes-field");
      expect(wrapper.find('[data-testid="pt-input-help"]').text()).toBe(
        "Optional",
      );
    });

    it("shows the error text when invalid", async () => {
      const wrapper = await createWrapper({
        multiline: true,
        invalid: true,
        errorText: "Too long",
      });

      expect(wrapper.find('[data-testid="pt-input-error"]').text()).toBe(
        "Too long",
      );
    });

    it("disables the textarea when disabled", async () => {
      const wrapper = await createWrapper({ multiline: true, disabled: true });

      expect(
        wrapper
          .find('[data-testid="pt-input-textarea"]')
          .attributes("disabled"),
      ).toBeDefined();
    });

    it("ignores the icons", async () => {
      const wrapper = await createWrapper({
        multiline: true,
        leftIcon: "pi pi-search",
      });

      expect(
        wrapper.find('[data-testid="pt-input-icon-container"]').exists(),
      ).toBe(false);
      expect(wrapper.find('[data-testid="pt-input-textarea"]').exists()).toBe(
        true,
      );
    });
  });
});
