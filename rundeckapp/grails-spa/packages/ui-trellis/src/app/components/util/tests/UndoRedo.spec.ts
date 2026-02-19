import { mount, VueWrapper } from "@vue/test-utils";
import mitt, { Emitter, EventType } from "mitt";
import Btn from "uiv";
import UndoRedo from "../UndoRedo.vue";
import { EventBus } from "../../../../library";

const createWrapper = async (options: {
  eventBus: typeof EventBus;
  revertAllEnabled?: boolean;
}): Promise<VueWrapper<any>> => {
  const wrapper = mount(UndoRedo, {
    props: {
      eventBus: options.eventBus,
      revertAllEnabled: options.revertAllEnabled ?? false,
    },
    global: {
      stubs: {
        Btn: {
          name: "Btn",
          props: ["size", "title", "disabled"],
          template: "<button :disabled='disabled' ><slot></slot></button>",
        },
      },
    },
    components: {
      Btn,
    },
  });

  await wrapper.vm.$nextTick();

  return wrapper;
};

describe("UndoRedo", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("undo and redo are disabled without changes", async () => {
    const eventBus: Emitter<Record<EventType, any>> = mitt();
    const wrapper = await createWrapper({ eventBus });

    expect(wrapper.find('[data-testid="undo-btn"]').classes()).toContain(
      "disabled",
    );
    expect(wrapper.find('[data-testid="redo-btn"]').classes()).toContain(
      "disabled",
    );
  });

  it.each([true, false])(
    "undo enabled with changes, has revertall %p",
    async (revertAllEnabled) => {
      const eventBus: Emitter<Record<EventType, any>> = mitt();
      const wrapper = await createWrapper({ eventBus, revertAllEnabled });
      eventBus.emit("change", { test: "event" });

      await wrapper.vm.$nextTick();

      expect(
        wrapper.find('[data-testid="undo-btn"]').classes(),
      ).not.toContain("disabled");
      expect(wrapper.find('[data-testid="redo-btn"]').classes()).toContain(
        "disabled",
      );
      const revertAll = wrapper.find('[data-testid="revertAll-btn"]');
      expect(revertAll.exists()).toEqual(revertAllEnabled);
    },
  );

  it("redo enabled after undo", async () => {
    const eventBus: Emitter<Record<EventType, any>> = mitt();
    const wrapper = await createWrapper({ eventBus });
    eventBus.emit("change", { test: "event" });
    const undoHandler = jest.fn();
    eventBus.on("undo", undoHandler);

    await wrapper.vm.$nextTick();

    const undo = wrapper.find('[data-testid="undo-btn"]');
    expect(undo.classes()).not.toContain("disabled");

    await undo.trigger("click");
    await wrapper.vm.$nextTick();

    expect(undoHandler).toHaveBeenCalledWith({ test: "event" });

    expect(wrapper.find('[data-testid="redo-btn"]').classes()).not.toContain(
      "disabled",
    );
    expect(wrapper.find('[data-testid="undo-btn"]').classes()).toContain(
      "disabled",
    );
  });

  it("undo re-enabled after redo", async () => {
    const eventBus: Emitter<Record<EventType, any>> = mitt();
    const wrapper = await createWrapper({ eventBus });
    eventBus.emit("change", { test: "event" });
    const redoHandler = jest.fn();
    eventBus.on("redo", redoHandler);

    await wrapper.vm.$nextTick();

    await wrapper.find('[data-testid="undo-btn"]').trigger("click");
    await wrapper.vm.$nextTick();

    await wrapper.find('[data-testid="redo-btn"]').trigger("click");
    await wrapper.vm.$nextTick();

    expect(redoHandler).toHaveBeenCalledWith({ test: "event" });
    expect(wrapper.find('[data-testid="redo-btn"]').classes()).toContain(
      "disabled",
    );
    expect(wrapper.find('[data-testid="undo-btn"]').classes()).not.toContain(
      "disabled",
    );
  });

  it("revert all resets index and emits revertAll", async () => {
    const eventBus: Emitter<Record<EventType, any>> = mitt();
    const wrapper = await createWrapper({ eventBus, revertAllEnabled: true });
    eventBus.emit("change", { test: "event" });
    const handler = jest.fn();
    eventBus.on("revertAll", handler);
    await wrapper.vm.$nextTick();

    const revertAll = wrapper.find('[data-testid="revertAll-btn"]');
    expect(revertAll.exists()).toBe(true);

    await revertAll.trigger("click");
    await wrapper.vm.$nextTick();

    expect(handler).toHaveBeenCalled();
    expect(wrapper.find('[data-testid="undo-btn"]').classes()).toContain(
      "disabled",
    );
    expect(wrapper.find('[data-testid="redo-btn"]').classes()).not.toContain(
      "disabled",
    );
  });
});
