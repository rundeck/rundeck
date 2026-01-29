import { mount } from "@vue/test-utils";
import InputText from "primevue/inputtext";
import IconField from "primevue/iconfield";
import InputIcon from "primevue/inputicon";
import PtInput from "../PtInput.vue";

const createWrapper = async (props = {}) => {
  const wrapper = mount(PtInput, {
    props: {
      modelValue: "",
      ...props,
    },
    global: {
      components: {
        InputText,
        IconField,
        InputIcon,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("PtInput", () => {
  describe("Rendering", () => {
    it("renders an input field", async () => {
      const wrapper = await createWrapper();
      
      const input = wrapper.find("input");
      expect(input.exists()).toBe(true);
    });

    it("displays label when label prop provided", async () => {
      const wrapper = await createWrapper({ label: "Username" });
      
      const label = wrapper.find("label");
      expect(label.exists()).toBe(true);
      expect(label.text()).toBe("Username");
    });

    it("displays helper text when helpText prop provided", async () => {
      const wrapper = await createWrapper({ helpText: "Enter your username" });
      
      const helpText = wrapper.find(".pt-input__help");
      expect(helpText.exists()).toBe(true);
      expect(helpText.text()).toBe("Enter your username");
    });

    it("displays error text when invalid and errorText provided", async () => {
      const wrapper = await createWrapper({
        invalid: true,
        errorText: "This field is required",
      });
      
      const errorText = wrapper.find(".pt-input__error");
      expect(errorText.exists()).toBe(true);
      expect(errorText.text()).toBe("This field is required");
    });

    it("does not display error text when not invalid", async () => {
      const wrapper = await createWrapper({
        invalid: false,
        errorText: "This field is required",
      });
      
      const errorText = wrapper.find(".pt-input__error");
      expect(errorText.exists()).toBe(false);
    });

    it("displays left icon when leftIcon prop provided", async () => {
      const wrapper = await createWrapper({ leftIcon: "pi pi-search" });
      
      const icons = wrapper.findAll(".pi");
      expect(icons.length).toBeGreaterThan(0);
      expect(icons[0].classes()).toContain("pi-search");
    });

    it("displays right icon when rightIcon prop provided", async () => {
      const wrapper = await createWrapper({ rightIcon: "pi pi-times" });
      
      const icons = wrapper.findAll(".pi");
      expect(icons.length).toBeGreaterThan(0);
    });

    it("displays both icons when both props provided", async () => {
      const wrapper = await createWrapper({
        leftIcon: "pi pi-search",
        rightIcon: "pi pi-times",
      });
      
      const icons = wrapper.findAll(".pi");
      expect(icons.length).toBeGreaterThanOrEqual(2);
    });
  });

  describe("User Interactions", () => {
    it("allows user to type and updates value", async () => {
      const wrapper = await createWrapper();
      
      const input = wrapper.find("input");
      await input.setValue("test input");
      
      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
      expect(wrapper.emitted("update:modelValue")![0]).toEqual(["test input"]);
    });

    it("displays initial value when modelValue is provided", async () => {
      const wrapper = await createWrapper({ modelValue: "initial value" });
      
      const input = wrapper.find("input");
      expect((input.element as HTMLInputElement).value).toBe("initial value");
    });

    it("emits focus event when input is focused", async () => {
      const wrapper = await createWrapper();
      
      const input = wrapper.find("input");
      await input.trigger("focus");
      
      expect(wrapper.emitted("focus")).toBeTruthy();
    });

    it("emits blur event when input loses focus", async () => {
      const wrapper = await createWrapper();
      
      const input = wrapper.find("input");
      await input.trigger("blur");
      
      expect(wrapper.emitted("blur")).toBeTruthy();
    });

    it("emits input event when user types", async () => {
      const wrapper = await createWrapper();
      
      const input = wrapper.find("input");
      await input.setValue("test");
      
      expect(wrapper.emitted("input")).toBeTruthy();
    });

    it("prevents input when disabled", async () => {
      const wrapper = await createWrapper({ disabled: true });
      
      const input = wrapper.find("input");
      expect((input.element as HTMLInputElement).disabled).toBe(true);
    });

    it("shows placeholder text", async () => {
      const wrapper = await createWrapper({ placeholder: "Enter text..." });
      
      const input = wrapper.find("input");
      expect((input.element as HTMLInputElement).placeholder).toBe("Enter text...");
    });

    it("respects readonly state", async () => {
      const wrapper = await createWrapper({ readonly: true, modelValue: "readonly value" });
      
      const input = wrapper.find("input");
      expect((input.element as HTMLInputElement).readOnly).toBe(true);
    });

    it("respects maxlength attribute", async () => {
      const wrapper = await createWrapper({ maxlength: 5 });
      
      const input = wrapper.find("input");
      expect((input.element as HTMLInputElement).maxLength).toBe(5);
    });

    it("respects type attribute", async () => {
      const wrapper = await createWrapper({ type: "password" });
      
      const input = wrapper.find("input");
      expect((input.element as HTMLInputElement).type).toBe("password");
    });
  });

  describe("Accessibility", () => {
    it("associates label with input via for attribute", async () => {
      const wrapper = await createWrapper({
        label: "Username",
        inputId: "username-input",
      });
      
      const label = wrapper.find("label");
      expect(label.attributes("for")).toBe("username-input");
      
      const input = wrapper.find("input");
      expect(input.attributes("id")).toBe("username-input");
    });

    it("sets aria-label attribute", async () => {
      const wrapper = await createWrapper({ ariaLabel: "Search input" });
      
      const input = wrapper.find("input");
      expect(input.attributes("aria-label")).toBe("Search input");
    });

    it("sets aria-labelledby attribute", async () => {
      const wrapper = await createWrapper({ ariaLabelledby: "label-id" });
      
      const input = wrapper.find("input");
      expect(input.attributes("aria-labelledby")).toBe("label-id");
    });
  });
});
