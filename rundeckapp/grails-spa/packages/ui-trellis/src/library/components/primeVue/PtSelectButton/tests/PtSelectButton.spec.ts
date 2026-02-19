import { mount } from "@vue/test-utils";
import SelectButton from "primevue/selectbutton";
import PtSelectButton from "../PtSelectButton.vue";

const OPTIONS = ["Option A", "Option B", "Option C"];

const createWrapper = async (props = {}): Promise<any> => {
  const wrapper = mount(PtSelectButton, {
    props: {
      modelValue: null,
      options: OPTIONS,
      ...props,
    },
    global: { components: { SelectButton } },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("PtSelectButton", () => {
  describe("when the user selects an option", () => {
    it("emits update:modelValue with the selected value so v-model binding works in the parent", async () => {
      const wrapper = await createWrapper();
      await wrapper.findComponent(SelectButton).vm.$emit("update:modelValue", "Option A");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:modelValue")).toHaveLength(1);
      expect(wrapper.emitted("update:modelValue")![0]).toEqual(["Option A"]);
    });

    it("also emits a change event with the same value so parents listening to change are notified", async () => {
      const wrapper = await createWrapper();
      await wrapper.findComponent(SelectButton).vm.$emit("update:modelValue", "Option A");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("change")).toHaveLength(1);
      expect(wrapper.emitted("change")![0]).toEqual(["Option A"]);
    });

    it("emits both update:modelValue and change in a single selection so the parent never needs to listen to two separate events", async () => {
      const wrapper = await createWrapper();
      await wrapper.findComponent(SelectButton).vm.$emit("update:modelValue", "Option B");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:modelValue")).toHaveLength(1);
      expect(wrapper.emitted("change")).toHaveLength(1);
      expect(wrapper.emitted("update:modelValue")![0]).toEqual(wrapper.emitted("change")![0]);
    });
  });

  describe("clearing the selection", () => {
    it("emits null when the user deselects the current option so the parent knows nothing is selected", async () => {
      const wrapper = await createWrapper({ modelValue: "Option A" });
      await wrapper.findComponent(SelectButton).vm.$emit("update:modelValue", null);
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:modelValue")![0]).toEqual([null]);
      expect(wrapper.emitted("change")![0]).toEqual([null]);
    });
  });

  describe("multiple selection", () => {
    it("emits an array when multiple is enabled and the user picks several options", async () => {
      const wrapper = await createWrapper({ multiple: true, modelValue: [] });
      await wrapper.findComponent(SelectButton).vm.$emit("update:modelValue", ["Option A", "Option C"]);
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:modelValue")![0]).toEqual([["Option A", "Option C"]]);
      expect(wrapper.emitted("change")![0]).toEqual([["Option A", "Option C"]]);
    });
  });

  describe("object options with optionValue", () => {
    it("emits the extracted value when optionValue is configured so the parent receives the id not the full object", async () => {
      const objectOptions = [
        { name: "Node Steps", value: "node" },
        { name: "Workflow Steps", value: "workflow" },
      ];
      const wrapper = await createWrapper({
        options: objectOptions,
        optionLabel: "name",
        optionValue: "value",
        modelValue: null,
      });

      await wrapper.findComponent(SelectButton).vm.$emit("update:modelValue", "workflow");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:modelValue")![0]).toEqual(["workflow"]);
      expect(wrapper.emitted("change")![0]).toEqual(["workflow"]);
    });
  });
});
