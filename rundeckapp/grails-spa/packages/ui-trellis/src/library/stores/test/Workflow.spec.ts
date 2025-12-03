import { describe, it, expect, beforeEach, jest } from "@jest/globals";
import { WorkflowStore } from "../Workflow";
import { RootStore } from "../RootStore";

const mockApiGet = jest.fn();
jest.mock("../../services/api", () => {
  return {
    api: {
      get: jest.fn(),
    },
  };
});

jest.mock("../../rundeckService", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    rdBase: "http://localhost:4440/",
    projectName: "testProject",
    apiVersion: "44",
  })),
}));

import { api } from "../../services/api";

describe("WorkflowStore", () => {
  let workflowStore: WorkflowStore;
  let mockRootStore: RootStore;

  beforeEach(() => {
    jest.clearAllMocks();
    mockRootStore = {} as RootStore;
    workflowStore = new WorkflowStore(mockRootStore);
    (api.get as any).mockImplementation(mockApiGet);
  });

  describe("get", () => {
    it("should fetch and cache workflow for a job", async () => {
      const jobId = "test-job-123";
      const mockWorkflow = {
        workflow: [
          {
            type: "command",
            exec: "echo test",
          },
        ],
      };

      (api.get as any).mockResolvedValueOnce({
        data: mockWorkflow,
      } as any);

      const workflow = await workflowStore.get(jobId);

      expect(api.get).toHaveBeenCalledWith(`job/${jobId}/workflow`);
      expect(workflow).toBeDefined();
      expect(workflow.workflow).toEqual(mockWorkflow.workflow);
    });

    it("should return cached workflow on subsequent calls", async () => {
      const jobId = "test-job-123";
      const mockWorkflow = {
        workflow: [
          {
            type: "command",
            exec: "echo test",
          },
        ],
      };

      (api.get as any).mockResolvedValueOnce({
        data: mockWorkflow,
      } as any);

      const workflow1 = await workflowStore.get(jobId);
      const workflow2 = await workflowStore.get(jobId);

      expect(api.get).toHaveBeenCalledTimes(1); // Should only call once
      expect(workflow1).toBe(workflow2); // Should return same instance
    });

    it("should fetch different workflows for different jobs", async () => {
      const jobId1 = "test-job-1";
      const jobId2 = "test-job-2";
      const mockWorkflow1 = {
        workflow: [{ type: "command", exec: "echo test1" }],
      };
      const mockWorkflow2 = {
        workflow: [{ type: "command", exec: "echo test2" }],
      };

      (api.get as any)
        .mockResolvedValueOnce({ data: mockWorkflow1 } as any)
        .mockResolvedValueOnce({ data: mockWorkflow2 } as any);

      const workflow1 = await workflowStore.get(jobId1);
      const workflow2 = await workflowStore.get(jobId2);

      expect(api.get).toHaveBeenCalledTimes(2);
      expect(workflow1.workflow).toEqual(mockWorkflow1.workflow);
      expect(workflow2.workflow).toEqual(mockWorkflow2.workflow);
    });
  });
});

