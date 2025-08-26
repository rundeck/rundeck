import { mount, VueWrapper } from "@vue/test-utils";
import CommonUndoRedoDraggableList from "../CommonUndoRedoDraggableList.vue";
import mitt, { Emitter, EventType } from "mitt";
import { Operation } from "../../job/options/model/ChangeEvents";

jest.mock("@/library", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: { on: jest.fn(), emit: jest.fn() },
  })),
}));
const eventBus: Emitter<Record<EventType, any>> = mitt();

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
      ...props,
    },
    global: {
    },
    slots: {
      item: `<template #item="{ item }"><div class="item-content" data-test="rendered-items">{{ item.element.name }}</div></template>`,
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

const assertRenderedItems = (wrapper, itemsArray = []) => {
  expect(wrapper.find(".item-container").exists()).toBe(true);
  const allRenderedItems = wrapper.findAll('[data-test="rendered-items"]');
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
    expect(wrapper.find(".loading-spinner").exists()).toBe(true);
  });

  it("renders internal data when loading is false and internalData is not null", async () => {
    const items = [{ name: "Item 1" }];
    const wrapper = await createWrapper({
      modelValue: items,
    });
    await wrapper.vm.$nextTick();
    assertRenderedItems(wrapper, items);
  });

  it("renders empty slot when internalData is empty", async () => {
    const wrapper = await createWrapper({ modelValue: [] });
    await wrapper.vm.$nextTick();
    expect(
      wrapper.find("[data-testid='draggable-container']").exists(),
    ).toBeFalsy();
  });

  it("renders add button when mode is 'edit' and loading is false", async () => {
    const wrapper = await createWrapper({ mode: "edit", loading: false });
    expect(wrapper.find("[data-testid='add-button']").exists()).toBe(true);
  });

  it("emits addButtonClick event when add button is clicked", async () => {
    const wrapper = await createWrapper();
    await wrapper.find("[data-testid='add-button']").trigger("click");
    expect(wrapper.emitted().addButtonClick).toBeTruthy();
  });

  it("handles drag and drop operations correctly", async () => {
    const initialData = [{ name: "Item 1" }, { name: "Item 2" }];
    const expectedResult = [...initialData].reverse();
    const wrapper = await createWrapper({
      modelValue: initialData,
    });
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
    wrapper.setProps({ modelValue: [{ name: "Item 2" }] });
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted()).toEqual({});
    assertRenderedItems(wrapper, [{ name: "Item 2" }]);
  });

  it("reverts all changes when doRevertAll is called", async () => {
    const items = [{ name: "Item 1" }];
    const wrapper = await createWrapper({
      modelValue: items,
      revertAllEnabled: true,
    });
    wrapper.setProps({ modelValue: [{ name: "Item 2" }] });
    await wrapper.vm.$nextTick();

    assertRenderedItems(wrapper, [{ name: "Item 2" }]);

    const undoRedo = wrapper.findComponent({ name: "UndoRedo" });
    undoRedo.vm.eventBus.emit("revertAll");

    await wrapper.vm.$nextTick();
    assertRenderedItems(wrapper, items);
  });

  it("handles remove operation correctly", async () => {
    const expectedItems = [{ name: "Item 1" }, { name: "Item 2" }];

    const wrapper = await createWrapper({
      modelValue: expectedItems,
    });
    wrapper.setProps({
      modelValue: [...expectedItems, { name: "Item 3" }],
    });

    wrapper.vm.changeEvent({
      index: 2,
      value: { name: "Item 3" },
      operation: Operation.Insert,
      undo: Operation.Remove,
    });
    await wrapper.vm.$nextTick();

    const undoRedo = wrapper.findComponent({ name: "UndoRedo" });
    undoRedo.vm.doUndo();

    await wrapper.vm.$nextTick();
    assertRenderedItems(wrapper, expectedItems);
  });

  it("handles modify operation correctly", async () => {
    const expectedItems = [{ name: "Item 1" }, { name: "Item 2 - 1" }];

    const wrapper = await createWrapper({
      modelValue: [{ name: "Item 1" }, { name: "Item 2" }],
    });

    wrapper.vm.operationModify(1, expectedItems[1]);
    wrapper.vm.changeEvent({
      index: 1,
      operation: Operation.Modify,
      undo: Operation.Modify,
      orig: { name: "Item 2" },
      value: { name: "Item 2 - 1" },
    });
    await wrapper.vm.$nextTick();

    assertRenderedItems(wrapper, expectedItems);
  });
});
