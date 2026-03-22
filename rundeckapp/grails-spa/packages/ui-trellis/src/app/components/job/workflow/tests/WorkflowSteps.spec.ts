import { flushPromises, mount, VueWrapper } from "@vue/test-utils";
import ChoosePluginModal from "@/library/components/plugins/ChoosePluginModal.vue";
import EditPluginModal from "@/library/components/plugins/EditPluginModal.vue";
import type { PluginConfig } from "../../../../../library/interfaces/PluginConfig";
import type {
  EditStepData,
  ErrorHandlerDefinition,
  JobRefDefinition,
  StepData,
  StepsData,
} from "../types/workflowTypes";
import WorkflowSteps from "../WorkflowSteps.vue";
import { createTestingPinia } from "@pinia/testing";
import { getRundeckContext } from "../../../../../library";
import ErrorHandlerStep from "@/app/components/job/workflow/ErrorHandlerStep.vue";

jest.mock("@/library/modules/pluginService", () => ({
  getServiceProviderDescription: jest.fn(),
  getPluginProvidersForService: jest.fn().mockReturnValue({
    service: true,
    descriptions: ["descrip1", "descrip2"],
    labels: ["label1", "label2"],
  }),
  validatePluginConfig: jest.fn().mockResolvedValue({
    valid: true,
    errors: {},
  }),
}));
jest.mock("@/library/modules/rundeckClient", () => ({
  client: jest.fn(),
}));
jest.mock("@/library/rundeckService", () => {
  const mittLib = require("mitt");
  const mittFn = mittLib.default || mittLib;
  const _bus = mittFn();
  const eventBus = {
    on: jest.fn(_bus.on.bind(_bus)),
    off: jest.fn(_bus.off.bind(_bus)),
    emit: jest.fn(_bus.emit.bind(_bus)),
  };
  return {
    getRundeckContext: jest.fn().mockImplementation(() => ({
      client: {},
      eventBus,
      rdBase: "http://localhost:4440",
      projectName: "testProject",
      apiVersion: "44",
      rootStore: {
        plugins: {
          load: jest.fn(),
          getServicePlugins: jest.fn(),
        },
      },
    })),
  };
});
jest.mock("@/library/services/projects");
jest.mock("@/library/modules/pluginService");
jest.mock("@/library/stores/NodesStorePinia", () => ({
  useNodesStore: jest.fn().mockImplementation(() => ({})),
}));

/**
 * Persisted workflow JSON for simple exec steps: nested errorhandler uses the exec branch in
 * commandToEditConfig (see StepData.errorhandler vs ErrorHandlerDefinition in workflowTypes).
 */
interface PersistedWorkflowCommand {
  description?: string;
  exec?: string;
  jobref?: JobRefDefinition;
  nodeStep?: boolean;
  plugins?: { LogFilter?: PluginConfig[] };
  errorhandler?:
    | {
        description?: string;
        nodeStep: boolean;
        jobref?: JobRefDefinition;
        exec: string;
        keepgoingOnSuccess?: boolean;
      }
    | ErrorHandlerDefinition;
}

type WorkflowStepsTestProps = Partial<Omit<StepsData, "commands">> & {
  commands?: Array<PersistedWorkflowCommand | StepData>;
};

/** Fixture matching persisted workflow JSON shape used by these tests (see editCommandsToStepsData output). */
const baseCommand: PersistedWorkflowCommand = {
  description: "echo test",
  errorhandler: {
    description: undefined,
    nodeStep: true,
    jobref: undefined,
    exec: "echo error",
    keepgoingOnSuccess: false,
  },
  jobref: undefined,
  exec: "echo test",
};
const createWrapper = async (
  props: WorkflowStepsTestProps = {},
  stubs: Record<string, boolean | object> = {},
): Promise<VueWrapper<InstanceType<typeof WorkflowSteps>>> => {
  const wrapper = mount(WorkflowSteps, {
    props: {
      modelValue: {
        commands: [{ ...baseCommand }],
        ...props,
      } as StepsData,
    },
    global: {
      stubs: {
        popover: true,
        tabs: true,
        tab: true,
        dropdown: {
          template: `<div><slot/><slot name="dropdown" /></div>`,
        },
        JobRefForm: true,
        ...stubs,
      },
      plugins: [createTestingPinia()],
    },
  });
  await flushPromises();
  return wrapper;
};

describe("WorkflowSteps", () => {
  let wrapper: VueWrapper<InstanceType<typeof WorkflowSteps>>;
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("no commands initially available", () => {
    beforeEach(async () => {
      wrapper = await createWrapper({ commands: [] });
    });

    it("renders the component with no steps", async () => {
      expect(wrapper.find('[data-testid="no-steps"]').text()).toContain(
        "Workflow.noSteps",
      );
    });

    it("emits update:modelValue when a step is added", async () => {
      const mockEventBus = getRundeckContext().eventBus;
      const addButton = wrapper.find('[data-testid="add-button"]');
      await addButton.trigger("click");

      const chooseModal = wrapper.findComponent(ChoosePluginModal);
      chooseModal.vm.$emit("selected", { service: "a", provider: "b" });
      await wrapper.vm.$nextTick();
      const editModal = wrapper.findComponent(EditPluginModal);
      editModal.vm.$emit("update:modelValue", {
        type: "c",
        config: { test: true },
      });
      editModal.vm.$emit("save");

      await flushPromises();
      expect(wrapper.emitted("update:modelValue")![1][0]).toEqual({
        commands: [
          {
            configuration: {
              test: true,
            },
            description: undefined,
            nodeStep: false,
            type: "c",
          },
        ],
      });
      expect(mockEventBus.emit).toHaveBeenCalledWith(
        "workflow-editor-workflowsteps-updated",
        wrapper.vm.model,
      );
    });
  });

  describe("1 command initially available", () => {
    beforeEach(async () => {
      wrapper = await createWrapper();
    });

    it("emits update:modelValue when a step is removed", async () => {
      const mockEventBus = getRundeckContext().eventBus;
      const removeButton = wrapper.find('[data-test="remove-step"]');
      await removeButton.trigger("click");
      expect(wrapper.emitted("update:modelValue")![1][0]).toEqual({
        commands: [],
      });
      expect(mockEventBus.emit).toBeCalledWith(
        "workflow-editor-workflowsteps-updated",
        {
          commands: [],
        },
      );
    });

    it("emits update:modelValue when a step is updated", async () => {
      const mockEventBus = getRundeckContext().eventBus;
      const editStep = wrapper.find('[data-test="edit-step-item"]');
      await editStep.trigger("click");
      await wrapper.vm.$nextTick();
      const editModal = wrapper.findAllComponents(EditPluginModal)[1];
      editModal.vm.$emit("update:modelValue", {
        type: "exec-command",
        config: {
          adhocRemoteString: "abcdef123",
        },
      });
      editModal.vm.$emit("save");
      await flushPromises();

      expect(wrapper.emitted("update:modelValue")![1][0]).toEqual({
        commands: [
          {
            description: "echo test",
            errorhandler: {
              description: undefined,
              exec: "echo error",
              jobref: undefined,
              keepgoingOnSuccess: false,
              nodeStep: true,
            },
            exec: "abcdef123",
            nodeStep: true,
          },
        ],
      });
      expect(mockEventBus.emit).toHaveBeenCalledWith(
        "workflow-editor-workflowsteps-updated",
        wrapper.vm.model,
      );
    });

    it("duplicates a step with all its properties", async () => {
      const duplicateBtn = wrapper.find("[title='Workflow.duplicateStep']");
      await duplicateBtn.trigger("click");

      const emitted = wrapper.emitted("update:modelValue")![1][0];
      expect(emitted["commands"]).toHaveLength(2);
      expect(emitted["commands"]).toEqual([
        { ...baseCommand, nodeStep: true },
        { ...baseCommand, nodeStep: true },
      ]);
    });
  });

  describe("log filters preserved on edit save (saveEditStep)", () => {
    it("keeps plugins.LogFilter on the step after opening edit and saving plugin config changes", async () => {
      const logFilterEntry: PluginConfig = {
        type: "key-value-data",
        config: { regex: "preserved" },
      };
      const commandWithFilters: StepData = {
        description: "echo test",
        exec: "echo original",
        nodeStep: true,
        plugins: {
          LogFilter: [logFilterEntry],
        },
      };
      const localWrapper = await createWrapper({ commands: [commandWithFilters] });

      const editStep = localWrapper.find('[data-test="edit-step-item"]');
      await editStep.trigger("click");
      await localWrapper.vm.$nextTick();

      // Same modal index pattern as "emits update:modelValue when a step is updated"
      const editModal = localWrapper.findAllComponents(EditPluginModal)[1];
      const updatedEditStep: EditStepData = {
        id: "edit-modal-step",
        type: "exec-command",
        nodeStep: true,
        config: {
          adhocRemoteString: "echo updated",
        },
      };
      editModal.vm.$emit("update:modelValue", updatedEditStep);
      editModal.vm.$emit("save");
      await flushPromises();

      const emissions = localWrapper.emitted("update:modelValue");
      expect(emissions).toBeDefined();
      const lastPayload = emissions![emissions!.length - 1][0] as StepsData;

      // saveEditStep must not clear filters (previously saveData.filters = []); they map
      // from edit model via commandToEditConfig(plugins.LogFilter) and back in editToCommandConfig.
      expect(lastPayload.commands[0]).toMatchObject({
        description: "echo test",
        exec: "echo updated",
        nodeStep: true,
        plugins: {
          LogFilter: [logFilterEntry],
        },
      });
    });
  });

  describe("2 commands initially available", () => {
    beforeEach(async () => {
      wrapper = await createWrapper(
        {
          commands: [
            { ...baseCommand },
            { ...baseCommand, errorhandler: undefined },
          ],
        },
        {
          popover: {
            template: `<div><slot/></div>`,
          },
          tabs: {
            template: `<div><slot/></div>`,
          },
          tab: {
            template: `<div><slot/></div>`,
          },
        },
      );
    });

    describe("error handlers", () => {
      it("renders the correct information when adding an error handler", async () => {
        const addErrorHandlerButton = wrapper.find(
          "[data-test='add-error-handler']",
        );
        await addErrorHandlerButton.trigger("click");
        await wrapper.vm.$nextTick();
        const chooseModal = wrapper.findAllComponents(ChoosePluginModal)[0];
        expect(chooseModal.find(".modal-title").text()).toBe(
          "Workflow.addErrorHandler",
        );
        expect(
          chooseModal.find("[data-testid='error-handler-title']").text(),
        ).toBe("framework.service.WorkflowNodeStep.description");
        expect(
          chooseModal.find("[data-testid='selection-description']").text(),
        ).toBe("Workflow.errorHandlerDescription");
      });

      it("edits existing error handler", async () => {
        const errorHandlerStep = wrapper.findComponent(ErrorHandlerStep);
        errorHandlerStep.vm.$emit("edit");
        await wrapper.vm.$nextTick();

        const editModal = wrapper.findAllComponents(EditPluginModal)[2];
        editModal.vm.$emit("update:modelValue", {
          type: "exec-command",
          config: { adhocRemoteString: "new-error" },
        });

        editModal.vm.$emit("save");
        await flushPromises();

        expect(
          wrapper.emitted("update:modelValue")![1][0]!["commands"][0],
        ).toMatchObject({
          ...baseCommand,
          errorhandler: {
            exec: "new-error",
            keepgoingOnSuccess: false,
            nodeStep: true,
          },
        });
      });
    });

    it("adds log filter information in the correct step", async () => {
      const mockEventBus = getRundeckContext().eventBus;
      const addLogFilterSecondStep = wrapper.findAll(
        "a[data-test='add-log-filter']",
      )[1];
      await addLogFilterSecondStep.trigger("click");
      await wrapper.vm.$nextTick();
      const chooseModal = wrapper.findAllComponents(ChoosePluginModal)[0];
      chooseModal.vm.$emit("selected", { service: "a", provider: "b" });
      await wrapper.vm.$nextTick();
      const editModal = wrapper.findAllComponents(EditPluginModal)[1];
      editModal.vm.$emit("update:modelValue", {
        type: "key-value-data",
        config: { regex: "" },
      });
      editModal.vm.$emit("save");
      await flushPromises();

      expect(wrapper.emitted("update:modelValue")![1][0]!["commands"]).toEqual([
        { ...baseCommand, nodeStep: true },
        {
          description: "echo test",
          exec: "echo test",
          nodeStep: true,
          plugins: {
            LogFilter: [
              {
                config: {
                  regex: "",
                },
                type: "key-value-data",
              },
            ],
          },
        },
      ]);

      expect(mockEventBus.emit).toHaveBeenCalledWith(
        "workflow-editor-workflowsteps-updated",
        wrapper.vm.model,
      );
    });

    it("on mount registers handler to respond to updated data request", async () => {
      const mockEventBus = getRundeckContext().eventBus;
      expect(mockEventBus.on).toHaveBeenCalledWith(
        "workflow-editor-workflowsteps-request",
        expect.any(Function),
      );
    });
  });
});
