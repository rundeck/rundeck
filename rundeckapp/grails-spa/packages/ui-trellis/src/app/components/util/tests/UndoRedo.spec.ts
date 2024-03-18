import { mount, VueWrapper } from "@vue/test-utils";
import mitt, { Emitter, EventType } from "mitt";
import Btn from "uiv";
import UndoRedo from "../UndoRedo.vue";
import { EventBus } from "../../../../library";

const mountUndoRedo = async (options: {
  eventBus: typeof EventBus;
  revertAllEnabled: boolean;
}): Promise<VueWrapper<any>> => {
  const wrapper = mount(UndoRedo, {
    props: {
      eventBus: options.eventBus,
      revertAllEnabled: options.revertAllEnabled,
    },
    global: {
      mocks: {
        $t: jest.fn().mockImplementation((msg) => msg),
        $tc: jest.fn().mockImplementation((msg) => msg),
      },
      stubs: {
        Btn: {
          name: "Btn",
          props: ["size", "title", "disabled"],
          emits: ["click"],
          template: "<button :disabled='disabled' ><slot></slot></button>",
        },
      },
    },

    components: {
      Btn,
    },
  });

  // Wait for the next Vue tick to allow for asynchronous rendering
  await wrapper.vm.$nextTick();

  return wrapper;
};

describe("UndoRedo", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });
  it("undo redo disabled without changes", async () => {
    const eventBus: Emitter<Record<EventType, any>> = mitt();
    const wrapper = await mountUndoRedo({ eventBus, revertAllEnabled: false });

    let undo = wrapper.get("[data-test=undo-btn]");
    expect(undo.classes()).toContain("disabled");
    let redo = wrapper.get("[data-test=redo-btn]");
    expect(redo.classes()).toContain("disabled");
  });
  it.each([true, false])(
    "undo enabled with changes, has revertall %p",
    async (revertAllEnabled) => {
      const eventBus: Emitter<Record<EventType, any>> = mitt();
      const wrapper = await mountUndoRedo({ eventBus, revertAllEnabled });
      eventBus.emit("change", { test: "event" });

      await wrapper.vm.$nextTick();

      let undo = wrapper.get("[data-test=undo-btn]");
      expect(undo.classes()).not.toContain("disabled");
      let redo = wrapper.get("[data-test=redo-btn]");
      expect(redo.classes()).toContain("disabled");
      let revertAll = wrapper.find("[data-test=revertAll-btn]");
      expect(revertAll.exists()).toEqual(revertAllEnabled);
    },
  );
  it("redo enabled after undo", async () => {
    const eventBus: Emitter<Record<EventType, any>> = mitt();
    const wrapper = await mountUndoRedo({ eventBus, revertAllEnabled: false });
    eventBus.emit("change", { test: "event" });
    const undoHandler = jest.fn();
    eventBus.on("undo", undoHandler);

    await wrapper.vm.$nextTick();

    let undo = wrapper.get("[data-test=undo-btn]");
    expect(undo.classes()).not.toContain("disabled");
    wrapper.vm.doUndo();
    await wrapper.vm.$nextTick();
    expect(undoHandler).toHaveBeenCalledWith({ test: "event" });

    let redo = wrapper.get("[data-test=redo-btn]");
    expect(redo.classes()).not.toContain("disabled");

    expect(undo.classes()).toContain("disabled");
  });
  it("undo re-enabled after redo", async () => {
    const eventBus: Emitter<Record<EventType, any>> = mitt();
    const wrapper = await mountUndoRedo({ eventBus, revertAllEnabled: false });
    eventBus.emit("change", { test: "event" });
    const redoHandler = jest.fn();
    eventBus.on("redo", redoHandler);

    await wrapper.vm.$nextTick();

    let undo = wrapper.get("[data-test=undo-btn]");
    expect(undo.classes()).not.toContain("disabled");
    wrapper.vm.doUndo();
    await wrapper.vm.$nextTick();
    wrapper.vm.doRedo();
    await wrapper.vm.$nextTick();
    expect(redoHandler).toHaveBeenCalledWith({ test: "event" });

    let redo = wrapper.get("[data-test=redo-btn]");
    expect(redo.classes()).toContain("disabled");

    expect(undo.classes()).not.toContain("disabled");
  });
  it("revert all action", async () => {
    const eventBus: Emitter<Record<EventType, any>> = mitt();
    const wrapper = await mountUndoRedo({ eventBus, revertAllEnabled: true });
    eventBus.emit("change", { test: "event" });
    const handler = jest.fn();
    eventBus.on("revertAll", handler);
    await wrapper.vm.$nextTick();

    let revertAll = wrapper.get("[data-test=revertAll-btn]");
    expect(revertAll.classes()).not.toContain("disabled");
    wrapper.vm.doRevertAll();
    await wrapper.vm.$nextTick();
    expect(handler).toHaveBeenCalled();

    let undo = wrapper.get("[data-test=undo-btn]");
    let redo = wrapper.get("[data-test=redo-btn]");
    expect(undo.classes()).toContain("disabled");
    expect(redo.classes()).not.toContain("disabled");
  });
});
