import { mount, VueWrapper } from "@vue/test-utils";
import CommonUndoRedoDraggableList from "../CommonUndoRedoDraggableList.vue";
import UndoRedo from "../../util/UndoRedo.vue";
jest.mock("@/library", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: { on: jest.fn(), emit: jest.fn() },
  })),
}));

const createWrapper = async (props = {}): Promise<VueWrapper<any>> => {
  const wrapper = mount(CommonUndoRedoDraggableList, {
    props: {
      revertAllEnabled: false,
      mode: "edit",
      buttonLabel: "",
      modelValue: [],
      itemKey: "name",
      handle: ".dragHandle",
      draggableTag: "div",
      loading: false,
      conditionalEnabled: false,
      addButtonDisabled: false,
      draggableDisabled: false,
      ...props,
    },
    global: {},
    slots: {
      item: `<template #item="{ item }"><div class="item-content" data-testid="rendered-items">{{ item.element.name }}</div></template>`,
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

const assertRenderedItems = (wrapper: VueWrapper<any>, itemsArray: any[] = []) => {
  expect(wrapper.find('[data-testid="item-container"]').exists()).toBe(true);
  const allRenderedItems = wrapper.findAll('[data-testid="rendered-items"]');
  expect(allRenderedItems).toHaveLength(itemsArray.length);
  expect(allRenderedItems.map((e) => e.text())).toStrictEqual(
    itemsArray.map((item) => item.name),
  );
};

describe("CommonUndoRedoDraggableList", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders loading spinner when loading is true", async () => {
    const wrapper = await createWrapper({ loading: true });
    expect(wrapper.find('[data-testid="loading-spinner"]').exists()).toBe(true);
  });

  it("renders internal data when loading is false and internalData is not null", async () => {
    const items = [{ name: "Item 1" }];
    const wrapper = await createWrapper({ modelValue: items });
    await wrapper.vm.$nextTick();
    assertRenderedItems(wrapper, items);
  });

  it("renders empty slot when internalData is empty", async () => {
    const wrapper = await createWrapper({ modelValue: [] });
    await wrapper.vm.$nextTick();
    expect(
      wrapper.find('[data-testid="draggable-container"]').exists(),
    ).toBeFalsy();
  });

  it("renders add button when mode is 'edit' and loading is false", async () => {
    const wrapper = await createWrapper({ mode: "edit", loading: false });
    expect(wrapper.find('[data-testid="add-button"]').exists()).toBe(true);
  });

  it("does not render add button when mode is not 'edit'", async () => {
    const wrapper = await createWrapper({ mode: "view", loading: false });
    expect(wrapper.find('[data-testid="add-button"]').exists()).toBe(false);
  });

  it("emits addButtonClick event when add button is clicked", async () => {
    const wrapper = await createWrapper();
    await wrapper.find('[data-testid="add-button"]').trigger("click");
    expect(wrapper.emitted().addButtonClick).toBeTruthy();
  });

  it("handles drag and drop operations correctly", async () => {
    const initialData = [{ name: "Item 1" }, { name: "Item 2" }];
    const expectedResult = [...initialData].reverse();
    const wrapper = await createWrapper({ modelValue: initialData });
    const draggable = wrapper.findComponent({ name: "draggable" });
    await draggable.vm.$emit("update:modelValue", [
      { name: "Item 2" },
      { name: "Item 1" },
    ]);
    await draggable.vm.$emit("update", { oldIndex: 0, newIndex: 1 });

    await wrapper.vm.$nextTick();
    expect(wrapper.emitted()["update:modelValue"][0][0]).toEqual(
      expectedResult,
    );
  });

  it("does not emit update:modelValue when data changes from parent", async () => {
    const wrapper = await createWrapper({ modelValue: [{ name: "Item 1" }] });
    assertRenderedItems(wrapper, [{ name: "Item 1" }]);
    await wrapper.setProps({ modelValue: [{ name: "Item 2" }] });
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted()).toEqual({});
    assertRenderedItems(wrapper, [{ name: "Item 2" }]);
  });

  it("reverts all changes via drag then revertAll button click", async () => {
    const items = [{ name: "Item 1" }, { name: "Item 2" }];
    const wrapper = await createWrapper({
      modelValue: items,
      revertAllEnabled: true,
    });

    // Populate undo stack by triggering a drag operation
    const draggable = wrapper.findComponent({ name: "draggable" });
    await draggable.vm.$emit("update", { oldIndex: 0, newIndex: 1 });
    await wrapper.vm.$nextTick();

    // UndoRedo now has items in stack (hasUndo: true), revertAll button is visible
    const undoRedo = wrapper.findComponent(UndoRedo);
    await undoRedo.find('[data-testid="revertAll-btn"]').trigger("click");
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted()["update:modelValue"]).toBeTruthy();
  });

  describe("conditionalEnabled prop", () => {
    it("does not add ea-mode class to list-container by default", async () => {
      const wrapper = await createWrapper({ modelValue: [] });

      expect(
        wrapper.find('[data-testid="list-container"]').classes(),
      ).not.toContain("ea-mode");
    });

    it("adds ea-mode class to list-container when conditionalEnabled is true", async () => {
      const wrapper = await createWrapper({
        modelValue: [],
        conditionalEnabled: true,
      });

      expect(
        wrapper.find('[data-testid="list-container"]').classes(),
      ).toContain("ea-mode");
    });

    it("adds right class to UndoRedo when conditionalEnabled is true", async () => {
      const wrapper = await createWrapper({
        modelValue: [],
        conditionalEnabled: true,
      });

      expect(wrapper.findComponent(UndoRedo).classes()).toContain("right");
    });

    it("does not add right class to UndoRedo by default", async () => {
      const wrapper = await createWrapper({ modelValue: [] });

      expect(wrapper.findComponent(UndoRedo).classes()).not.toContain("right");
    });
  });

  describe("addButtonDisabled prop", () => {
    it("add button is not disabled by default", async () => {
      const wrapper = await createWrapper({ mode: "edit" });

      expect(
        wrapper.find('[data-testid="add-button"]').attributes("disabled"),
      ).toBeUndefined();
    });

    it("add button is disabled when addButtonDisabled is true", async () => {
      const wrapper = await createWrapper({
        mode: "edit",
        addButtonDisabled: true,
      });

      expect(
        wrapper.find('[data-testid="add-button"]').attributes("disabled"),
      ).toBeDefined();
    });
  });

  describe("draggableDisabled prop", () => {
  describe("eventBus null guard in wasChanged", () => {
    it("does not throw and still emits update:modelValue when eventBus is undefined", async () => {
      const { getRundeckContext } = require("@/library");
      (getRundeckContext as jest.Mock).mockImplementationOnce(() => ({
        eventBus: undefined,
      }));

      const wrapper = await createWrapper({
        modelValue: [{ name: "Item 1" }],
      });

      const draggable = wrapper.findComponent({ name: "draggable" });
      await draggable.vm.$emit("update", { oldIndex: 0, newIndex: 0 });
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted()["update:modelValue"]).toBeTruthy();
    });
  });
});
