import { mount } from "@vue/test-utils";
import Chip from "primevue/chip";
import ConfigSection from "./ConfigSection.vue";

const createWrapper = async (props = {}) => {
  const wrapper = mount(ConfigSection, {
    props: {
      title: "Log Filters",
      tooltip: "Manage log filters",
      modelValue: [],
      ...props,
    },
    global: {
      directives: { tooltip: {} },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

const mockItems = [
  { name: "filter-1", type: "mask-passwords", title: "Mask Passwords" },
  { name: "filter-2", type: "render-formatted", title: "Render Formatted" },
];

describe("ConfigSection", () => {
  describe("section title", () => {
    it("shows the section title so users know what this area manages", async () => {
      const wrapper = await createWrapper({ title: "Log Filters" });
      expect(wrapper.text()).toContain("Log Filters");
    });
  });

  describe("empty state — no items yet", () => {
    it("shows an Add button when there are no items yet", async () => {
      const wrapper = await createWrapper({ modelValue: [] });
      expect(wrapper.find('[data-testid="config-section-add-btn"]').exists()).toBe(true);
    });

    it("shows '+ Add' as the button text", async () => {
      const wrapper = await createWrapper({ modelValue: [] });
      expect(wrapper.find('[data-testid="config-section-add-btn"]').text()).toContain("+ Add");
    });

    it("does not show the chips area when there are no items", async () => {
      const wrapper = await createWrapper({ modelValue: [] });
      expect(wrapper.find('[data-testid="config-section-chips-row"]').exists()).toBe(false);
    });

    it("notifies the parent to open the add dialog when the Add button is clicked", async () => {
      const wrapper = await createWrapper({ modelValue: [] });
      await wrapper.find('[data-testid="config-section-add-btn"]').trigger("click");
      await wrapper.vm.$nextTick();
      expect(wrapper.emitted("addElement")).toHaveLength(1);
    });
  });

  describe("with existing items", () => {
    it("hides the initial Add button when items are already present", async () => {
      const wrapper = await createWrapper({ modelValue: mockItems });
      expect(wrapper.find('[data-testid="config-section-add-btn"]').exists()).toBe(false);
    });

    it("shows the items area when there are items", async () => {
      const wrapper = await createWrapper({ modelValue: mockItems });
      expect(wrapper.find('[data-testid="config-section-chips-row"]').exists()).toBe(true);
    });

    it("renders a chip for each item with the item label as text", async () => {
      const wrapper = await createWrapper({ modelValue: mockItems });
      const chips = wrapper.findAllComponents(Chip);
      expect(chips).toHaveLength(2);
      expect(chips[0].props("label")).toBe("Mask Passwords");
      expect(chips[1].props("label")).toBe("Render Formatted");
    });

    it("falls back to the item type as the chip label when no title is provided", async () => {
      const wrapper = await createWrapper({
        modelValue: [{ type: "mask-passwords" }],
      });
      expect(wrapper.findComponent(Chip).props("label")).toBe("mask-passwords");
    });

    it("shows an Add more button so users can add additional items", async () => {
      const wrapper = await createWrapper({ modelValue: mockItems, hideWhenSingle: false });
      expect(wrapper.find('[data-testid="config-section-add-more-btn"]').exists()).toBe(true);
    });

    it("hides the Add more button when hideWhenSingle is true", async () => {
      const wrapper = await createWrapper({ modelValue: mockItems, hideWhenSingle: true });
      expect(wrapper.find('[data-testid="config-section-add-more-btn"]').exists()).toBe(false);
    });

    it("notifies the parent to open the add dialog when Add more is clicked", async () => {
      const wrapper = await createWrapper({ modelValue: mockItems, hideWhenSingle: false });
      await wrapper.find('[data-testid="config-section-add-more-btn"]').trigger("click");
      await wrapper.vm.$nextTick();
      expect(wrapper.emitted("addElement")).toHaveLength(1);
    });
  });

  describe("removing an item", () => {
    it("removes the item from the list and notifies the parent with the updated list", async () => {
      const wrapper = await createWrapper({ modelValue: mockItems });
      await wrapper.findComponent(Chip).vm.$emit("remove");
      await wrapper.vm.$nextTick();

      const updatedList = (wrapper.emitted("update:modelValue")![0][0] as any[]);
      expect(updatedList).toHaveLength(1);
      expect(updatedList[0].title).toBe("Render Formatted");
    });

    it("emits removeElement with the index of the removed item", async () => {
      const wrapper = await createWrapper({ modelValue: mockItems });
      await wrapper.findComponent(Chip).vm.$emit("remove");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("removeElement")).toHaveLength(1);
      expect(wrapper.emitted("removeElement")![0]).toEqual([0]);
    });
  });

  describe("editing an item", () => {
    it("emits editElement with the item data and its index when a chip is clicked", async () => {
      const wrapper = await createWrapper({ modelValue: mockItems });
      await wrapper.findComponent(Chip).vm.$emit("click");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("editElement")).toHaveLength(1);
      const [emittedElement, emittedIndex] = wrapper.emitted("editElement")![0] as [any, number];
      expect(emittedElement).toEqual(mockItems[0]);
      expect(emittedIndex).toBe(0);
    });
  });
});
