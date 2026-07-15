import { mount } from "@vue/test-utils";
import WorkflowEditWarning from "../WorkflowEditWarning.vue";
import { getRundeckContext } from "../../../../../library";

jest.mock("@/library", () => {
  const mittLib = require("mitt");
  const mittFn = mittLib.default || mittLib;
  const bus = mittFn();
  const eventBus = {
    on: jest.fn(bus.on.bind(bus)),
    off: jest.fn(bus.off.bind(bus)),
    emit: jest.fn(bus.emit.bind(bus)),
  };
  return {
    getRundeckContext: jest.fn(() => ({ eventBus })),
  };
});

const createWrapper = async () => {
  const wrapper = mount(WorkflowEditWarning);
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("WorkflowEditWarning", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("initial state", () => {
    it("does not render the warning when mounted", async () => {
      const wrapper = await createWrapper();

      expect(wrapper.find('[data-testid="workflow-edit-warning"]').exists()).toBe(false);
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

      getRundeckContext().eventBus.emit("workflow-editing-state-changed", { isEditing: true });
      await wrapper.vm.$nextTick();

      expect(wrapper.find('[data-testid="workflow-edit-warning"]').exists()).toBe(true);
    });

    it("hides the warning after isEditing transitions back to false", async () => {
      const wrapper = await createWrapper();

      getRundeckContext().eventBus.emit("workflow-editing-state-changed", { isEditing: true });
      await wrapper.vm.$nextTick();

      getRundeckContext().eventBus.emit("workflow-editing-state-changed", { isEditing: false });
      await wrapper.vm.$nextTick();

      expect(wrapper.find('[data-testid="workflow-edit-warning"]').exists()).toBe(false);
    });

    it("renders the i18n warning message key when visible", async () => {
      const wrapper = await createWrapper();

      getRundeckContext().eventBus.emit("workflow-editing-state-changed", { isEditing: true });
      await wrapper.vm.$nextTick();

      expect(wrapper.find('[data-testid="workflow-edit-warning"]').text()).toBe(
        "job.editor.workflow.unsavedchanges.warning",
      );
    });
  });
});
