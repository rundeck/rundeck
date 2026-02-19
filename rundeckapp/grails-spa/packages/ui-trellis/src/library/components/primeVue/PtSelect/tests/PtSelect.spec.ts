import { mount } from "@vue/test-utils";
import Select from "primevue/select";
import { PtSelect } from "../../index";

const createWrapper = async (props = {}): Promise<any> => {
  const wrapper = mount(PtSelect, {
    props: {
      modelValue: null,
      options: ["Option 1", "Option 2", "Option 3"],
      ...props,
    },
    global: { components: { Select } },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("PtSelect", () => {
  afterEach(() => {
    jest.useRealTimers();
  });

  describe("label", () => {
    it("does not show a label when none is provided", async () => {
      const wrapper = await createWrapper();
      expect(wrapper.find('[data-testid="pt-select-label"]').exists()).toBe(false);
    });

    it("shows the label text above the dropdown so users know what to pick", async () => {
      const wrapper = await createWrapper({ label: "Environment" });
      const label = wrapper.find('[data-testid="pt-select-label"]');
      expect(label.exists()).toBe(true);
      expect(label.text()).toBe("Environment");
    });

    it("links the label to the dropdown via the for attribute so clicking the label opens it", async () => {
      const wrapper = await createWrapper({ label: "Environment", inputId: "env-field" });
      expect(wrapper.find('[data-testid="pt-select-label"]').attributes("for")).toBe("env-field");
    });
  });

  describe("error message", () => {
    it("does not show an error when the field is valid", async () => {
      const wrapper = await createWrapper({ invalid: false, errorText: "Required" });
      expect(wrapper.find('[data-testid="pt-select-error"]').exists()).toBe(false);
    });

    it("shows the error message when the field is invalid so users know what to fix", async () => {
      const wrapper = await createWrapper({ invalid: true, errorText: "Please select an option" });
      const error = wrapper.find('[data-testid="pt-select-error"]');
      expect(error.exists()).toBe(true);
      expect(error.text()).toBe("Please select an option");
    });

    it("does not show an error even when invalid if no errorText is provided", async () => {
      const wrapper = await createWrapper({ invalid: true });
      expect(wrapper.find('[data-testid="pt-select-error"]').exists()).toBe(false);
    });
  });

  describe("when user picks an option", () => {
    it("emits the selected value so the parent can update its state", async () => {
      const wrapper = await createWrapper();
      await wrapper.findComponent(Select).vm.$emit("update:modelValue", "Option 2");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:modelValue")).toHaveLength(1);
      expect(wrapper.emitted("update:modelValue")![0]).toEqual(["Option 2"]);
    });

    it("debounces the update:modelValue emission when debounceMs is set so rapid selections do not flood the parent", async () => {
      jest.useFakeTimers();
      const wrapper = await createWrapper({ debounceMs: 300 });

      await wrapper.findComponent(Select).vm.$emit("update:modelValue", "Option 1");

      // Should not emit immediately
      expect(wrapper.emitted("update:modelValue")).toBeFalsy();

      jest.advanceTimersByTime(300);
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:modelValue")).toHaveLength(1);
      expect(wrapper.emitted("update:modelValue")![0]).toEqual(["Option 1"]);
    });

    it("only emits the last selection when the user changes rapidly within the debounce window", async () => {
      jest.useFakeTimers();
      const wrapper = await createWrapper({ debounceMs: 300 });

      await wrapper.findComponent(Select).vm.$emit("update:modelValue", "Option 1");
      await wrapper.findComponent(Select).vm.$emit("update:modelValue", "Option 2");
      await wrapper.findComponent(Select).vm.$emit("update:modelValue", "Option 3");

      jest.advanceTimersByTime(300);
      await wrapper.vm.$nextTick();

      // Only the last value should be emitted once
      expect(wrapper.emitted("update:modelValue")).toHaveLength(1);
      expect(wrapper.emitted("update:modelValue")![0]).toEqual(["Option 3"]);
    });
  });

  describe("focus and blur", () => {
    it("forwards the focus event with the native event payload to the parent", async () => {
      const wrapper = await createWrapper();
      const focusEvent = new FocusEvent("focus");
      await wrapper.findComponent(Select).vm.$emit("focus", focusEvent);
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("focus")).toHaveLength(1);
      expect(wrapper.emitted("focus")![0][0]).toBe(focusEvent);
    });

    it("forwards the blur event with the native event payload to the parent", async () => {
      const wrapper = await createWrapper();
      const blurEvent = new FocusEvent("blur");
      await wrapper.findComponent(Select).vm.$emit("blur", blurEvent);
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("blur")).toHaveLength(1);
      expect(wrapper.emitted("blur")![0][0]).toBe(blurEvent);
    });
  });

  describe("filter event", () => {
    it("forwards the filter event with the search query so the parent can fetch filtered options", async () => {
      const wrapper = await createWrapper({ filter: true });
      const filterPayload = { value: "prod" };
      await wrapper.findComponent(Select).vm.$emit("filter", filterPayload);
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("filter")).toHaveLength(1);
      expect(wrapper.emitted("filter")![0]).toEqual([filterPayload]);
    });
  });
});
