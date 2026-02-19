import { mount } from "@vue/test-utils";
import Chip from "primevue/chip";
import ConfigSection from "../ConfigSection.vue";

const createWrapper = async (props = {}): Promise<any> => {
  const wrapper = mount(ConfigSection, {
    props: {
      title: "Log Filters",
      tooltip: "Configure log filters for this step",
      modelValue: [],
      ...props,
    },
    global: { components: { Chip } },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("ConfigSection", () => {
  describe("title", () => {
    it("shows the section title so users know what they are configuring", async () => {
      const wrapper = await createWrapper({ title: "Error Handlers" });
      expect(wrapper.find('[data-testid="config-section-title"]').text()).toContain("Error Handlers");
    });
  });

  describe("add button", () => {
    it("shows the add button when the list is empty so users can add the first item", async () => {
      const wrapper = await createWrapper({ modelValue: [] });
      expect(wrapper.find('[data-testid="config-section-add-btn"]').exists()).toBe(true);
    });

    it("hides the add button when items are already present", async () => {
      const wrapper = await createWrapper({ modelValue: [{ type: "logging/mask-passwords" }] });
      expect(wrapper.find('[data-testid="config-section-add-btn"]').exists()).toBe(false);
    });

    it("emits addElement when the user clicks the add button", async () => {
      const wrapper = await createWrapper({ modelValue: [] });
      await wrapper.find('[data-testid="config-section-add-btn"]').trigger("click");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("addElement")).toHaveLength(1);
    });
  });

  describe("chips row", () => {
    it("does not show the chips row when the list is empty", async () => {
      const wrapper = await createWrapper({ modelValue: [] });
      expect(wrapper.find('[data-testid="config-section-chips-row"]').exists()).toBe(false);
    });

    it("shows the chips row when items are present so users can see and manage them", async () => {
      const wrapper = await createWrapper({ modelValue: [{ type: "logging/mask-passwords" }] });
      expect(wrapper.find('[data-testid="config-section-chips-row"]').exists()).toBe(true);
    });

    it("renders one chip per item in the list", async () => {
      const wrapper = await createWrapper({
        modelValue: [
          { type: "logging/mask-passwords" },
          { type: "logging/jsonData" },
        ],
      });
      expect(wrapper.findAllComponents(Chip)).toHaveLength(2);
    });
  });

  describe("chip labels", () => {
    it("uses the element title as the chip label when available", async () => {
      const wrapper = await createWrapper({
        modelValue: [{ title: "My Filter", type: "logging/mask-passwords" }],
      });
      expect(wrapper.findAllComponents(Chip)[0].props("label")).toBe("My Filter");
    });

    it("falls back to the element type as chip label when there is no title", async () => {
      const wrapper = await createWrapper({
        modelValue: [{ type: "logging/jsonData" }],
      });
      expect(wrapper.findAllComponents(Chip)[0].props("label")).toBe("logging/jsonData");
    });

    it("shows Unknown as chip label when the element has neither title nor type", async () => {
      const wrapper = await createWrapper({ modelValue: [{}] });
      expect(wrapper.findAllComponents(Chip)[0].props("label")).toBe("Unknown");
    });
  });

  describe("editing an item", () => {
    it("emits editElement with the element and its index when the user clicks a chip", async () => {
      const element = { title: "My Filter", type: "logging/mask-passwords" };
      const wrapper = await createWrapper({ modelValue: [element] });
      await wrapper.findAllComponents(Chip)[0].trigger("click");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("editElement")).toHaveLength(1);
      expect(wrapper.emitted("editElement")![0]).toEqual([element, 0]);
    });

    it("emits editElement with the correct index for each chip position", async () => {
      const first = { title: "First" };
      const second = { title: "Second" };
      const wrapper = await createWrapper({ modelValue: [first, second] });

      await wrapper.findAllComponents(Chip)[1].trigger("click");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("editElement")![0]).toEqual([second, 1]);
    });
  });

  describe("removing an item", () => {
    it("emits update:modelValue with the item removed so the parent list stays in sync", async () => {
      const first = { title: "First" };
      const second = { title: "Second" };
      const wrapper = await createWrapper({ modelValue: [first, second] });

      await wrapper.findAllComponents(Chip)[0].vm.$emit("remove");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:modelValue")).toHaveLength(1);
      expect(wrapper.emitted("update:modelValue")![0]).toEqual([[second]]);
    });

    it("emits removeElement with the removed index so the parent can react to the removal", async () => {
      const wrapper = await createWrapper({
        modelValue: [{ title: "First" }, { title: "Second" }],
      });

      await wrapper.findAllComponents(Chip)[0].vm.$emit("remove");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("removeElement")).toHaveLength(1);
      expect(wrapper.emitted("removeElement")![0]).toEqual([0]);
    });

    it("removes the correct item when the second chip is removed", async () => {
      const first = { title: "First" };
      const second = { title: "Second" };
      const wrapper = await createWrapper({ modelValue: [first, second] });

      await wrapper.findAllComponents(Chip)[1].vm.$emit("remove");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:modelValue")![0]).toEqual([[first]]);
      expect(wrapper.emitted("removeElement")![0]).toEqual([1]);
    });
  });

  describe("add more button", () => {
    it("shows the add more button when items are present and hideWhenSingle is false", async () => {
      const wrapper = await createWrapper({
        modelValue: [{ title: "First" }, { title: "Second" }],
        hideWhenSingle: false,
      });
      expect(wrapper.find('[data-testid="config-section-add-more-btn"]').exists()).toBe(true);
    });

    it("hides the add more button entirely when hideWhenSingle is true", async () => {
      const wrapper = await createWrapper({
        modelValue: [{ title: "First" }],
        hideWhenSingle: true,
      });
      expect(wrapper.find('[data-testid="config-section-add-more-btn"]').exists()).toBe(false);
    });

    it("emits addElement when the user clicks the add more button", async () => {
      const wrapper = await createWrapper({
        modelValue: [{ title: "First" }],
        hideWhenSingle: false,
      });
      await wrapper.find('[data-testid="config-section-add-more-btn"]').trigger("click");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("addElement")).toHaveLength(1);
    });
  });
});
