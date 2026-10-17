import { vi } from "vitest";
import { mount } from "@vue/test-utils";
import WorkflowEditWarning from "../WorkflowEditWarning.vue";
import { getRundeckContext } from "../../../../../library";

vi.mock("@/library", async () => {
  const mittLib = await vi.importActual<typeof import("mitt")>("mitt");
  const mittFn = mittLib.default || mittLib;
  const bus = mittFn();
  const eventBus = {
    on: vi.fn(bus.on.bind(bus)),
    off: vi.fn(bus.off.bind(bus)),
    emit: vi.fn(bus.emit.bind(bus)),
  };
  return {
    getRundeckContext: vi.fn(() => ({ eventBus })),
  };
});

const createWrapper = async () => {
  const wrapper = mount(WorkflowEditWarning);
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("WorkflowEditWarning", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("initial state", () => {
    it("does not render the warning when mounted", async () => {
      const wrapper = await createWrapper();

      expect(
        wrapper.find('[data-testid="workflow-edit-warning"]').exists(),
      ).toBe(false);
    });
  });

  describe("event bus integration", () => {
    it("registers workflow-editing-state-changed listener on mount", async () => {
      await createWrapper();

      expect(getRundeckContext().eventBus.on).toHaveBeenCalledWith(
        "workflow-editing-state-changed",
        expect.any(Function),
      );
    });

    it("unregisters the listener on unmount", async () => {
      const wrapper = await createWrapper();
      wrapper.unmount();

      expect(getRundeckContext().eventBus.off).toHaveBeenCalledWith(
        "workflow-editing-state-changed",
        expect.any(Function),
      );
    });
  });

  describe("warning visibility", () => {
    it("shows the warning when workflow-editing-state-changed fires with isEditing true", async () => {
      const wrapper = await createWrapper();

      getRundeckContext().eventBus.emit("workflow-editing-state-changed", {
        isEditing: true,
      });
      await wrapper.vm.$nextTick();

      expect(
        wrapper.find('[data-testid="workflow-edit-warning"]').exists(),
      ).toBe(true);
    });

    it("hides the warning after isEditing transitions back to false", async () => {
      const wrapper = await createWrapper();

      getRundeckContext().eventBus.emit("workflow-editing-state-changed", {
        isEditing: true,
      });
      await wrapper.vm.$nextTick();

      getRundeckContext().eventBus.emit("workflow-editing-state-changed", {
        isEditing: false,
      });
      await wrapper.vm.$nextTick();

      expect(
        wrapper.find('[data-testid="workflow-edit-warning"]').exists(),
      ).toBe(false);
    });

    it("renders the i18n warning message key when visible", async () => {
      const wrapper = await createWrapper();

      getRundeckContext().eventBus.emit("workflow-editing-state-changed", {
        isEditing: true,
      });
      await wrapper.vm.$nextTick();

      expect(wrapper.find('[data-testid="workflow-edit-warning"]').text()).toBe(
        "job.editor.workflow.unsavedchanges.warning",
      );
    });
  });
});
