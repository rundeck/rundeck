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
    it("renders InputText without icons when no icon props provided", async () => {
      const wrapper = await createWrapper();
      
      expect(wrapper.findComponent(InputText).exists()).toBe(true);
      expect(wrapper.findComponent(IconField).exists()).toBe(false);
    });

    it("renders IconField with left icon when leftIcon prop provided", async () => {
      const wrapper = await createWrapper({ leftIcon: "pi pi-search" });
      
      expect(wrapper.findComponent(IconField).exists()).toBe(true);
      expect(wrapper.findAllComponents(InputIcon).length).toBe(1);
      expect(wrapper.findComponent(InputIcon).classes()).toContain("pi");
      expect(wrapper.findComponent(InputIcon).classes()).toContain("pi-search");
    });

    it("renders IconField with right icon when rightIcon prop provided", async () => {
      const wrapper = await createWrapper({ rightIcon: "pi pi-times" });
      
      expect(wrapper.findComponent(IconField).exists()).toBe(true);
      expect(wrapper.findAllComponents(InputIcon).length).toBe(1);
    });

    it("renders IconField with both icons when both props provided", async () => {
      const wrapper = await createWrapper({
        leftIcon: "pi pi-search",
        rightIcon: "pi pi-times",
      });
      
      expect(wrapper.findComponent(IconField).exists()).toBe(true);
      expect(wrapper.findAllComponents(InputIcon).length).toBe(2);
    });

    it("displays label when label prop provided", async () => {
      const wrapper = await createWrapper({ label: "Username" });
      
      const label = wrapper.find("label");
      expect(label.exists()).toBe(true);
      expect(label.text()).toBe("Username");
      expect(label.classes()).toContain("text-heading--sm");
    });

    it("displays helper text when helpText prop provided", async () => {
      const wrapper = await createWrapper({ helpText: "Enter your username" });
      
      const helpText = wrapper.find(".pt-input__help");
      expect(helpText.exists()).toBe(true);
      expect(helpText.text()).toBe("Enter your username");
      expect(helpText.classes()).toContain("text-body--sm");
    });

    it("displays error text when invalid and errorText provided", async () => {
      const wrapper = await createWrapper({
        invalid: true,
        errorText: "This field is required",
      });
      
      const errorText = wrapper.find(".pt-input__error");
      expect(errorText.exists()).toBe(true);
      expect(errorText.text()).toBe("This field is required");
      expect(errorText.classes()).toContain("text-body--sm");
    });

    it("does not display error text when not invalid", async () => {
      const wrapper = await createWrapper({
        invalid: false,
        errorText: "This field is required",
      });
      
      const errorText = wrapper.find(".pt-input__error");
      expect(errorText.exists()).toBe(false);
    });
  });

  describe("v-model", () => {
    it("binds modelValue correctly", async () => {
      const wrapper = await createWrapper({ modelValue: "test value" });
      
      const input = wrapper.findComponent(InputText);
      expect(input.props("modelValue")).toBe("test value");
    });

    it("emits update:modelValue when value changes", async () => {
      const wrapper = await createWrapper();
      
      const input = wrapper.findComponent(InputText);
      await input.setValue("new value");
      
      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
      expect(wrapper.emitted("update:modelValue")![0]).toEqual(["new value"]);
    });
  });

  describe("Events", () => {
    it("emits focus event", async () => {
      const wrapper = await createWrapper();
      
      const input = wrapper.findComponent(InputText);
      await input.trigger("focus");
      
      expect(wrapper.emitted("focus")).toBeTruthy();
    });

    it("emits blur event", async () => {
      const wrapper = await createWrapper();
      
      const input = wrapper.findComponent(InputText);
      await input.trigger("blur");
      
      expect(wrapper.emitted("blur")).toBeTruthy();
    });

    it("emits input event", async () => {
      const wrapper = await createWrapper();
      
      const input = wrapper.findComponent(InputText);
      await input.trigger("input");
      
      expect(wrapper.emitted("input")).toBeTruthy();
    });
  });

  describe("Props", () => {
    it("passes placeholder to InputText", async () => {
      const wrapper = await createWrapper({ placeholder: "Enter text..." });
      
      const input = wrapper.findComponent(InputText);
      expect(input.props("placeholder")).toBe("Enter text...");
    });

    it("passes disabled to InputText", async () => {
      const wrapper = await createWrapper({ disabled: true });
      
      const input = wrapper.findComponent(InputText);
      expect(input.props("disabled")).toBe(true);
    });

    it("passes invalid to InputText", async () => {
      const wrapper = await createWrapper({ invalid: true });
      
      const input = wrapper.findComponent(InputText);
      expect(input.props("invalid")).toBe(true);
    });

    it("passes name to InputText", async () => {
      const wrapper = await createWrapper({ name: "username" });
      
      const input = wrapper.findComponent(InputText);
      expect(input.props("name")).toBe("username");
    });

    it("passes inputId to InputText as id", async () => {
      const wrapper = await createWrapper({ inputId: "my-input" });
      
      const input = wrapper.findComponent(InputText);
      expect(input.attributes("id")).toBe("my-input");
    });

    it("associates label with input via for attribute", async () => {
      const wrapper = await createWrapper({
        label: "Username",
        inputId: "username-input",
      });
      
      const label = wrapper.find("label");
      expect(label.attributes("for")).toBe("username-input");
    });

    it("passes type to InputText", async () => {
      const wrapper = await createWrapper({ type: "password" });
      
      const input = wrapper.findComponent(InputText);
      expect(input.props("type")).toBe("password");
    });

    it("passes readonly to InputText", async () => {
      const wrapper = await createWrapper({ readonly: true });
      
      const input = wrapper.findComponent(InputText);
      expect(input.props("readonly")).toBe(true);
    });

    it("passes maxlength to InputText", async () => {
      const wrapper = await createWrapper({ maxlength: 50 });
      
      const input = wrapper.findComponent(InputText);
      expect(input.props("maxlength")).toBe(50);
    });

    it("passes autocomplete to InputText", async () => {
      const wrapper = await createWrapper({ autocomplete: "off" });
      
      const input = wrapper.findComponent(InputText);
      expect(input.props("autocomplete")).toBe("off");
    });
  });

  describe("Accessibility", () => {
    it("passes aria-label to InputText", async () => {
      const wrapper = await createWrapper({ ariaLabel: "Search input" });
      
      const input = wrapper.findComponent(InputText);
      expect(input.attributes("aria-label")).toBe("Search input");
    });

    it("passes aria-labelledby to InputText", async () => {
      const wrapper = await createWrapper({ ariaLabelledby: "label-id" });
      
      const input = wrapper.findComponent(InputText);
      expect(input.attributes("aria-labelledby")).toBe("label-id");
    });
  });
});
