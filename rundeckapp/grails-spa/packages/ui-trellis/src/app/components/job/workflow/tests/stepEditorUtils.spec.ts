import type { EditStepData } from "../types/workflowTypes";

const mockValidatePluginConfig = jest.fn();
const mockGetServicePlugins = jest.fn();

jest.mock("@/library/modules/pluginService", () => ({
  validatePluginConfig: (...args: any[]) => mockValidatePluginConfig(...args),
  getServiceProviderDescription: jest.fn(),
  getPluginProvidersForService: jest.fn(),
}));

jest.mock("@/library/modules/rundeckClient", () => ({
  client: jest.fn(),
}));

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    rootStore: {
      plugins: {
        getServicePlugins: (...args: any[]) => mockGetServicePlugins(...args),
        load: jest.fn(),
      },
    },
    projectName: "testProject",
    eventBus: { on: jest.fn(), off: jest.fn(), emit: jest.fn() },
  })),
}));

jest.mock("@/library/stores/Plugins", () => ({
  ServiceType: {
    WorkflowNodeStep: "WorkflowNodeStep",
    WorkflowStep: "WorkflowStep",
  },
  PluginStore: jest.fn(),
}));

jest.mock("../../../../../library/services/projects");

// Import after mocks are set up
import {
  createStepFromProvider,
  getPluginDetailsForStep,
  resetValidation,
  validateStepForSave,
} from "../stepEditorUtils";

// Use raw string values matching the mocked ServiceType enum
const ServiceType = {
  WorkflowNodeStep: "WorkflowNodeStep",
  WorkflowStep: "WorkflowStep",
};

describe("stepEditorUtils", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("resetValidation", () => {
    it("returns a clean validation state with valid true and empty errors", () => {
      const result = resetValidation();

      expect(result.valid).toBe(true);
      expect(result.errors).toEqual({});
    });

    it("returns a new object on each call", () => {
      const result1 = resetValidation();
      const result2 = resetValidation();

      expect(result1).not.toBe(result2);
    });
  });

  describe("createStepFromProvider", () => {
    it("creates a regular plugin step for WorkflowNodeStep service", () => {
      const step = createStepFromProvider(
        ServiceType.WorkflowNodeStep,
        "script-inline",
      );

      expect(step.type).toBe("script-inline");
      expect(step.config).toEqual({});
      expect(step.nodeStep).toBe(true);
      expect(step.id).toBeDefined();
      expect(step.jobref).toBeUndefined();
    });

    it("creates a regular plugin step for WorkflowStep service", () => {
      const step = createStepFromProvider(
        ServiceType.WorkflowStep,
        "some-workflow-step",
      );

      expect(step.type).toBe("some-workflow-step");
      expect(step.config).toEqual({});
      expect(step.nodeStep).toBe(false);
      expect(step.id).toBeDefined();
    });

    it("creates a job reference step", () => {
      const step = createStepFromProvider(
        ServiceType.WorkflowNodeStep,
        "job.reference",
      );

      expect(step.type).toBe("job.reference");
      expect(step.nodeStep).toBe(true);
      expect(step.description).toBe("");
      expect(step.jobref).toBeDefined();
      expect(step.jobref!.nodeStep).toBe(true);
      expect(step.id).toBeDefined();
    });

    it("creates a job reference step with nodeStep=false for WorkflowStep", () => {
      const step = createStepFromProvider(
        ServiceType.WorkflowStep,
        "job.reference",
      );

      expect(step.nodeStep).toBe(false);
      expect(step.jobref!.nodeStep).toBe(false);
    });

    it("creates a conditional logic step", () => {
      const step = createStepFromProvider(
        ServiceType.WorkflowNodeStep,
        "conditional.logic",
      );

      expect(step.type).toBe("conditional.logic");
      expect(step.config).toEqual({});
      expect(step.nodeStep).toBe(true);
      expect(step.description).toBe("");
      expect(step.id).toBeDefined();
      expect(step.jobref).toBeUndefined();
    });

    it("generates unique ids for each call", () => {
      const step1 = createStepFromProvider(
        ServiceType.WorkflowNodeStep,
        "script-inline",
      );
      const step2 = createStepFromProvider(
        ServiceType.WorkflowNodeStep,
        "script-inline",
      );

      expect(step1.id).not.toBe(step2.id);
    });
  });

  describe("getPluginDetailsForStep", () => {
    it("returns plugin details when plugin is found in rootStore", () => {
      mockGetServicePlugins.mockReturnValue([
        {
          name: "script-inline",
          title: "Inline Script",
          description: "Execute an inline script",
          iconUrl: "/icon/script.png",
        },
      ]);

      const element = {
        type: "script-inline",
        nodeStep: true,
        id: "test-id",
      } as EditStepData;

      const result = getPluginDetailsForStep(
        element,
        ServiceType.WorkflowNodeStep,
      );

      expect(mockGetServicePlugins).toHaveBeenCalledWith(
        ServiceType.WorkflowNodeStep,
      );
      expect(result).toEqual({
        title: "Inline Script",
        description: "Execute an inline script",
        iconUrl: "/icon/script.png",
        tooltip: "Execute an inline script",
      });
    });

    it("uses service passed by caller (e.g. InnerStepList targetService)", () => {
      mockGetServicePlugins.mockReturnValue([
        {
          name: "exec-command",
          title: "Command",
          description: "Execute a remote command",
          iconUrl: "/icon/shell.png",
          providerMetadata: { faicon: "terminal" },
        },
      ]);

      const element = {
        type: "exec-command",
        id: "test-id",
      } as EditStepData;

      const result = getPluginDetailsForStep(
        element,
        ServiceType.WorkflowNodeStep,
      );

      expect(result.title).toBe("Command");
    });

    it("falls back to element description when plugin not found", () => {
      mockGetServicePlugins.mockReturnValue([]);

      const element = {
        type: "unknown-plugin",
        description: "My custom step",
        id: "test-id",
      } as EditStepData;

      const result = getPluginDetailsForStep(
        element,
        ServiceType.WorkflowNodeStep,
      );

      expect(result).toEqual({
        title: "My custom step",
        description: "",
        iconUrl: "",
        tooltip: "",
      });
    });

    it("falls back to element type when neither plugin nor description exists", () => {
      mockGetServicePlugins.mockReturnValue([]);

      const element = {
        type: "unknown-plugin",
        nodeStep: true,
        id: "test-id",
      } as EditStepData;

      const result = getPluginDetailsForStep(
        element,
        ServiceType.WorkflowNodeStep,
      );

      expect(result).toEqual({
        title: "unknown-plugin",
        description: "",
        iconUrl: "",
        tooltip: "",
      });
    });

    it("uses plugin title over element description when both exist", () => {
      mockGetServicePlugins.mockReturnValue([
        {
          name: "script-inline",
          title: "Inline Script",
          description: "Execute an inline script",
          iconUrl: "",
        },
      ]);

      const element = {
        type: "script-inline",
        nodeStep: true,
        description: "My custom description",
        id: "test-id",
      } as EditStepData;

      const result = getPluginDetailsForStep(
        element,
        ServiceType.WorkflowNodeStep,
      );

      expect(result.title).toBe("Inline Script");
    });

    it("uses element description as title fallback when plugin has no title", () => {
      mockGetServicePlugins.mockReturnValue([
        {
          name: "script-inline",
          title: "",
          description: "",
          iconUrl: "",
        },
      ]);

      const element = {
        type: "script-inline",
        nodeStep: true,
        description: "My custom step",
        id: "test-id",
      } as EditStepData;

      const result = getPluginDetailsForStep(
        element,
        ServiceType.WorkflowNodeStep,
      );

      expect(result.title).toBe("My custom step");
    });

    it("returns jobref plugin details when element has jobref but no type (loaded from saved workflow)", () => {
      mockGetServicePlugins.mockReturnValue([
        {
          name: "job.reference",
          title: "Job reference",
          description: "Execute another job",
          iconUrl: "",
          providerMetadata: { glyphicon: "book" },
        },
      ]);

      const element = {
        jobref: { name: "My Job", uuid: "", nodeStep: false },
        description: "My job ref step",
        id: "test-id",
      } as EditStepData;

      const result = getPluginDetailsForStep(
        element,
        ServiceType.WorkflowStep,
      );

      expect(mockGetServicePlugins).toHaveBeenCalledWith(
        ServiceType.WorkflowStep,
      );
      expect(result).toEqual({
        title: "Job reference",
        description: "Execute another job",
        iconUrl: "",
        tooltip: "Execute another job",
        providerMetadata: { glyphicon: "book" },
      });
    });

    it("returns jobref plugin details when element has type job.reference", () => {
      mockGetServicePlugins.mockReturnValue([
        {
          name: "job.reference",
          title: "Job reference",
          description: "Run a job on the remote node",
          iconUrl: "",
          providerMetadata: { glyphicon: "book" },
        },
      ]);

      const element = {
        type: "job.reference",
        jobref: { nodeStep: true },
        nodeStep: true,
        id: "test-id",
      } as EditStepData;

      const result = getPluginDetailsForStep(
        element,
        ServiceType.WorkflowNodeStep,
      );

      expect(result.providerMetadata).toEqual({ glyphicon: "book" });
      expect(result.title).toBe("Job reference");
    });

    it("includes providerMetadata when plugin has it", () => {
      mockGetServicePlugins.mockReturnValue([
        {
          name: "job.reference",
          title: "Job reference",
          description: "Execute another job",
          iconUrl: "",
          providerMetadata: { glyphicon: "book" },
        },
      ]);

      const element = {
        type: "job.reference",
        jobref: { nodeStep: false },
        nodeStep: false,
        id: "test-id",
      } as EditStepData;

      const result = getPluginDetailsForStep(
        element,
        ServiceType.WorkflowStep,
      );

      expect(result.providerMetadata).toEqual({ glyphicon: "book" });
    });

    it("returns fallback with providerMetadata for jobref when plugin not found", () => {
      mockGetServicePlugins.mockReturnValue([]);

      const element = {
        jobref: { name: "", uuid: "" },
        id: "test-id",
      } as EditStepData;

      const result = getPluginDetailsForStep(
        element,
        ServiceType.WorkflowStep,
      );

      expect(result).toEqual({
        title: "Job reference",
        description: "Execute another job",
        iconUrl: "",
        tooltip: "Execute another job",
        providerMetadata: { glyphicon: "book" },
      });
    });

    it("falls back to element type when plugin not found", () => {
      mockGetServicePlugins.mockReturnValue([]);

      const element = {
        type: "exec-command",
        nodeStep: true,
        id: "test-id",
      } as EditStepData;

      const result = getPluginDetailsForStep(
        element,
        ServiceType.WorkflowNodeStep,
      );

      expect(result.title).toBe("exec-command");
      expect(result.description).toBe("");
    });
  });

  describe("validateStepForSave", () => {
    it("returns valid for job reference with name", async () => {
      const step = {
        type: "job.reference",
        jobref: { name: "My Job", uuid: "" },
        id: "test-id",
        nodeStep: true,
      } as EditStepData;

      const result = await validateStepForSave(
        step,
        ServiceType.WorkflowNodeStep,
      );

      expect(result.valid).toBe(true);
      expect(result.errors).toEqual({});
      expect(mockValidatePluginConfig).not.toHaveBeenCalled();
    });

    it("returns valid for job reference with uuid", async () => {
      const step = {
        type: "job.reference",
        jobref: { name: "", uuid: "abc-123" },
        id: "test-id",
        nodeStep: true,
      } as EditStepData;

      const result = await validateStepForSave(
        step,
        ServiceType.WorkflowNodeStep,
      );

      expect(result.valid).toBe(true);
      expect(result.errors).toEqual({});
    });

    it("returns invalid for job reference without name or uuid", async () => {
      const step = {
        type: "job.reference",
        jobref: { name: "", uuid: "" },
        id: "test-id",
        nodeStep: true,
      } as EditStepData;

      const result = await validateStepForSave(
        step,
        ServiceType.WorkflowNodeStep,
      );

      expect(result.valid).toBe(false);
      expect(result.errors.jobref).toBeDefined();
    });

    it("returns valid for conditional.logic when conditionSet is empty", async () => {
      const step = {
        type: "conditional.logic",
        config: {},
        id: "test-id",
        nodeStep: true,
      } as EditStepData;

      const result = await validateStepForSave(
        step,
        ServiceType.WorkflowNodeStep,
      );

      expect(result.valid).toBe(true);
      expect(result.errors).toEqual({});
      expect(mockValidatePluginConfig).not.toHaveBeenCalled();
    });

    it("returns valid for conditional.logic when all conditions have field and value", async () => {
      const step = {
        type: "conditional.logic",
        config: {
          conditionSet: [
            {
              id: "set-1",
              conditions: [
                { id: "cond-1", field: "myField", operator: "equals", value: "myValue" },
              ],
            },
          ],
        },
        id: "test-id",
        nodeStep: true,
      } as EditStepData;

      const result = await validateStepForSave(
        step,
        ServiceType.WorkflowNodeStep,
      );

      expect(result.valid).toBe(true);
      expect(result.errors).toEqual({});
      expect(mockValidatePluginConfig).not.toHaveBeenCalled();
    });

    it("returns invalid for conditional.logic when a condition has no field", async () => {
      const step = {
        type: "conditional.logic",
        config: {
          conditionSet: [
            {
              id: "set-1",
              conditions: [
                { id: "cond-1", field: null, operator: "equals", value: "myValue" },
              ],
            },
          ],
        },
        id: "test-id",
        nodeStep: true,
      } as EditStepData;

      const result = await validateStepForSave(
        step,
        ServiceType.WorkflowNodeStep,
      );

      expect(result.valid).toBe(false);
      expect((result.errors as any).conditions["cond-1"].field).toBe("editConditionalStep.fieldRequired");
      expect(mockValidatePluginConfig).not.toHaveBeenCalled();
    });

    it("returns invalid for conditional.logic when a condition has no value", async () => {
      const step = {
        type: "conditional.logic",
        config: {
          conditionSet: [
            {
              id: "set-1",
              conditions: [
                { id: "cond-2", field: "someField", operator: "equals", value: "" },
              ],
            },
          ],
        },
        id: "test-id",
        nodeStep: true,
      } as EditStepData;

      const result = await validateStepForSave(
        step,
        ServiceType.WorkflowNodeStep,
      );

      expect(result.valid).toBe(false);
      expect((result.errors as any).conditions["cond-2"].value).toBe("editConditionalStep.valueRequired");
      expect(mockValidatePluginConfig).not.toHaveBeenCalled();
    });

    it("returns invalid for conditional.logic when a condition has whitespace-only field", async () => {
      const step = {
        type: "conditional.logic",
        config: {
          conditionSet: [
            {
              id: "set-1",
              conditions: [
                { id: "cond-3", field: "   ", operator: "equals", value: "someValue" },
              ],
            },
          ],
        },
        id: "test-id",
        nodeStep: true,
      } as EditStepData;

      const result = await validateStepForSave(
        step,
        ServiceType.WorkflowNodeStep,
      );

      expect(result.valid).toBe(false);
      expect((result.errors as any).conditions["cond-3"].field).toBe("editConditionalStep.fieldRequired");
    });

    it("reports both field and value errors for a condition missing both", async () => {
      const step = {
        type: "conditional.logic",
        config: {
          conditionSet: [
            {
              id: "set-1",
              conditions: [
                { id: "cond-4", field: null, operator: "equals", value: "" },
              ],
            },
          ],
        },
        id: "test-id",
        nodeStep: true,
      } as EditStepData;

      const result = await validateStepForSave(
        step,
        ServiceType.WorkflowNodeStep,
      );

      expect(result.valid).toBe(false);
      expect((result.errors as any).conditions["cond-4"].field).toBe("editConditionalStep.fieldRequired");
      expect((result.errors as any).conditions["cond-4"].value).toBe("editConditionalStep.valueRequired");
    });

    it("only records errors for invalid conditions, not valid ones", async () => {
      const step = {
        type: "conditional.logic",
        config: {
          conditionSet: [
            {
              id: "set-1",
              conditions: [
                { id: "cond-bad", field: null, operator: "equals", value: "" },
                { id: "cond-good", field: "validField", operator: "equals", value: "validValue" },
              ],
            },
          ],
        },
        id: "test-id",
        nodeStep: true,
      } as EditStepData;

      const result = await validateStepForSave(
        step,
        ServiceType.WorkflowNodeStep,
      );

      expect(result.valid).toBe(false);
      expect((result.errors as any).conditions["cond-bad"]).toBeDefined();
      expect((result.errors as any).conditions["cond-good"]).toBeUndefined();
    });

    it("calls validatePluginConfig for regular plugins and returns valid", async () => {
      mockValidatePluginConfig.mockResolvedValue({
        valid: true,
        errors: {},
      });

      const step = {
        type: "script-inline",
        config: { adhocLocalString: "echo hello" },
        id: "test-id",
        nodeStep: true,
      } as EditStepData;

      const result = await validateStepForSave(
        step,
        ServiceType.WorkflowNodeStep,
      );

      expect(mockValidatePluginConfig).toHaveBeenCalledWith(
        ServiceType.WorkflowNodeStep,
        "script-inline",
        { adhocLocalString: "echo hello" },
      );
      expect(result.valid).toBe(true);
      expect(result.errors).toEqual({});
    });

    it("returns validation errors from API for regular plugins", async () => {
      mockValidatePluginConfig.mockResolvedValue({
        valid: false,
        errors: { adhocLocalString: "Script is required" },
      });

      const step = {
        type: "script-inline",
        config: {},
        id: "test-id",
        nodeStep: true,
      } as EditStepData;

      const result = await validateStepForSave(
        step,
        ServiceType.WorkflowNodeStep,
      );

      expect(result.valid).toBe(false);
      expect(result.errors).toEqual({
        adhocLocalString: "Script is required",
      });
    });

    it("handles API errors gracefully", async () => {
      mockValidatePluginConfig.mockRejectedValue(new Error("Network error"));

      const step = {
        type: "script-inline",
        config: {},
        id: "test-id",
        nodeStep: true,
      } as EditStepData;

      const result = await validateStepForSave(
        step,
        ServiceType.WorkflowNodeStep,
      );

      expect(result.valid).toBe(false);
      expect(result.errors._general).toBeDefined();
    });

    it("passes empty config when step config is undefined", async () => {
      mockValidatePluginConfig.mockResolvedValue({
        valid: true,
        errors: {},
      });

      const step = {
        type: "script-inline",
        id: "test-id",
        nodeStep: true,
      } as EditStepData;

      await validateStepForSave(step, ServiceType.WorkflowNodeStep);

      expect(mockValidatePluginConfig).toHaveBeenCalledWith(
        ServiceType.WorkflowNodeStep,
        "script-inline",
        {},
      );
    });
  });
});
