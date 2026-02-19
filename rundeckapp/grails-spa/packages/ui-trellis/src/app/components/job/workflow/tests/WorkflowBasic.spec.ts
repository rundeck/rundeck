import { mount } from "@vue/test-utils";
import WorkflowBasic from "../WorkflowBasic.vue";

const createWrapper = async (props = {}) => {
  return mount(WorkflowBasic, {
    props: {
      modelValue: { keepgoing: false },
      conditionalEnabled: false,
      ...props,
    },
  });
};

describe("WorkflowBasic", () => {
  describe("keepgoing radio buttons", () => {
    it("starts with the 'stop on fail' radio checked when keepgoing is false", async () => {
      const wrapper = await createWrapper({ modelValue: { keepgoing: false } });

      expect((wrapper.find('[data-testid="keepgoing-false-radio"]').element as HTMLInputElement).checked).toBe(true);
      expect((wrapper.find('[data-testid="keepgoing-true-radio"]').element as HTMLInputElement).checked).toBe(false);
    });

    it("starts with the 'continue on fail' radio checked when keepgoing is true", async () => {
      const wrapper = await createWrapper({ modelValue: { keepgoing: true } });

      expect((wrapper.find('[data-testid="keepgoing-false-radio"]').element as HTMLInputElement).checked).toBe(false);
      expect((wrapper.find('[data-testid="keepgoing-true-radio"]').element as HTMLInputElement).checked).toBe(true);
    });

    it("emits update:modelValue with keepgoing=true when the continue radio is selected", async () => {
      const wrapper = await createWrapper({ modelValue: { keepgoing: false } });

      await wrapper.find('[data-testid="keepgoing-true-radio"]').setValue();

      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
      expect(wrapper.emitted("update:modelValue")![0][0]).toEqual({ keepgoing: true });
    });

    it("emits update:modelValue with keepgoing=false when the stop radio is selected", async () => {
      const wrapper = await createWrapper({ modelValue: { keepgoing: true } });

      await wrapper.find('[data-testid="keepgoing-false-radio"]').setValue();

      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
      expect(wrapper.emitted("update:modelValue")![0][0]).toEqual({ keepgoing: false });
    });
  });

  describe("conditionalEnabled prop", () => {
    it("does not show section heading or workflow-section when conditionalEnabled is false", async () => {
      const wrapper = await createWrapper({ conditionalEnabled: false });

      expect(wrapper.find('[data-testid="execution-behavior-heading"]').exists()).toBe(false);
      expect(wrapper.find('[data-testid="workflow-section"]').exists()).toBe(false);
    });

    it("shows the simple prompt when conditionalEnabled is false", async () => {
      const wrapper = await createWrapper({ conditionalEnabled: false });

      expect(wrapper.find('[data-testid="simple-prompt"]').exists()).toBe(true);
    });

    it("shows the section heading and workflow-section when conditionalEnabled is true", async () => {
      const wrapper = await createWrapper({ conditionalEnabled: true });

      expect(wrapper.find('[data-testid="execution-behavior-heading"]').exists()).toBe(true);
      expect(wrapper.find('[data-testid="workflow-section"]').exists()).toBe(true);
    });

    it("hides the simple prompt when conditionalEnabled is true", async () => {
      const wrapper = await createWrapper({ conditionalEnabled: true });

      expect(wrapper.find('[data-testid="simple-prompt"]').exists()).toBe(false);
    });

    it("radio buttons still emit correctly when conditionalEnabled is true", async () => {
      const wrapper = await createWrapper({ conditionalEnabled: true });

      await wrapper.find('[data-testid="keepgoing-true-radio"]').setValue();

      expect(wrapper.emitted("update:modelValue")![0][0]).toEqual({ keepgoing: true });
    });
  });
});
