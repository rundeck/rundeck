import "isomorphic-fetch";
import { api, apiClient } from "../../services/api";
import {
  ExecutionOutput,
  ExecutionOutputEntry,
  ExecutionOutputStore
} from "../ExecutionOutput";
import { RootStore } from "../RootStore";
import { JobWorkflow } from "../../utilities/JobWorkflow";

// Mock the API and other dependencies
const mockApiGet = jest.fn();
const mockApiClientGet = jest.fn();
jest.mock("../../services/api", () => {
  const api = {
    get: jest.fn(),
    put: jest.fn(),
    post: jest.fn(),
  };
  return {
    api,
    apiClient: jest.fn().mockImplementation(() => ({
      get: mockApiClientGet,
      put: jest.fn(),
      post: jest.fn(),
    })),
  };
});

describe("ExecutionOutput", () => {
  let rootStore: RootStore;

  beforeEach(() => {
    jest.clearAllMocks();

    rootStore = {} as RootStore;

    // Reset API mocks
    (apiClient as jest.MockedFn<any>).mockImplementation(() => ({
      get: mockApiClientGet,
      put: jest.fn(),
      post: jest.fn(),
    }));
    (api.get as jest.Mock).mockReset();
    mockApiClientGet.mockReset();
  });

  describe("ExecutionOutputStore", () => {
    it("should create or get an execution output instance", () => {
      const store = new ExecutionOutputStore(rootStore);

      // First call should create a new instance
      const output1 = store.createOrGet("123");
      expect(output1).toBeInstanceOf(ExecutionOutput);
      expect(output1.id).toBe("123");

      // Second call with same ID should return the same instance
      const output2 = store.createOrGet("123");
      expect(output2).toBe(output1);

      // Different ID should create a new instance
      const output3 = store.createOrGet("456");
      expect(output3).not.toBe(output1);
      expect(output3.id).toBe("456");
    });

    it("should get output with a specified max lines", async () => {
      const store = new ExecutionOutputStore(rootStore);
      const getOutputSpy = jest.spyOn(ExecutionOutput.prototype, 'getOutput').mockResolvedValue([]);

      await store.getOutput("123", 100);

      expect(getOutputSpy).toHaveBeenCalledWith(100);
    });
  });

  describe("ExecutionOutput", () => {
    let executionOutput: ExecutionOutput;

    beforeEach(() => {
      executionOutput = new ExecutionOutput("123");
    });

    it("should initialize with correct default values", () => {
      expect(executionOutput.id).toBe("123");
      expect(executionOutput.offset).toBe(0);
      expect(executionOutput.entries.length).toBe(0);
      expect(executionOutput.size).toBe(0);
      expect(executionOutput.backoff).toBe(0);
      expect(executionOutput.lineNumber).toBe(0);
    });

    it("should get filtered entries by node", () => {
      // Setup mock entries
      const entry1 = new ExecutionOutputEntry(executionOutput);
      Object.assign(entry1, { node: "node1" });

      const entry2 = new ExecutionOutputEntry(executionOutput);
      Object.assign(entry2, { node: "node2" });

      const entry3 = new ExecutionOutputEntry(executionOutput);
      Object.assign(entry3, { node: "node1" });

      executionOutput.entries.push(entry1, entry2, entry3);

      // Filter by node
      const node1Entries = executionOutput.getEntriesFiltered("node1");
      expect(node1Entries.length).toBe(2);
      expect(node1Entries[0]).toBe(entry1);
      expect(node1Entries[1]).toBe(entry3);
    });

    it("should get filtered entries by node and step context", () => {
      // Setup mock entries with node and stepctx
      const entry1 = new ExecutionOutputEntry(executionOutput);
      Object.assign(entry1, { node: "node1", stepctx: "1" });

      const entry2 = new ExecutionOutputEntry(executionOutput);
      Object.assign(entry2, { node: "node1", stepctx: "2" });

      const entry3 = new ExecutionOutputEntry(executionOutput);
      Object.assign(entry3, { node: "node1", stepctx: "1" });

      executionOutput.entries.push(entry1, entry2, entry3);

      // Setup entries by node:step
      executionOutput.entriesbyNodeCtx.get = jest.fn().mockImplementation((key) => {
        if (key === "node1:1") return [entry1, entry3];
        if (key === "node1:2") return [entry2];
        return [];
      });

      // Filter by node and step
      const nodeStepEntries = executionOutput.getEntriesFiltered("node1", "1");
      expect(executionOutput.entriesbyNodeCtx.get).toHaveBeenCalledWith("node1:1");
      expect(nodeStepEntries).toEqual([entry1, entry3]);
    });

    it("should initialize with execution output info", async () => {
      // Mock API response for init() which calls getExecutionOutput() that uses apiClient(43)
      mockApiClientGet.mockResolvedValueOnce({
        data: {
          id: "123",
          offset: "0",
          completed: true,
          execCompleted: true,
          hasFailedNodes: false,
          execState: "succeeded",
          lastModified: "2023-01-01T12:00:00Z",
          execDuration: 1000,
          totalSize: 500,
          entries: [],
        },
        status: 200,
      });

      await executionOutput.init();

      expect(executionOutput.execCompleted).toBe(true);
      expect(executionOutput.size).toBe(500);
      expect(mockApiClientGet).toHaveBeenCalledWith("execution/123/output", {
        params: {
          offset: "0",
          maxlines: "1",
        },
      });
    });

    it("should get job workflow from execution status", async () => {
      // Mock API responses
      (api.get as jest.Mock).mockImplementation((url) => {
        if (url === "execution/123") {
          return Promise.resolve({
            data: {
              id: "123",
              job: { id: "job-123" },
            }
          });
        } else if (url === "job/job-123/workflow") {
          return Promise.resolve({
            data: {
              workflow: [
                { exec: "echo test", type: "exec", nodeStep: "true" }
              ]
            }
          });
        }
        return Promise.reject(new Error(`Unexpected URL: ${url}`));
      });

      const workflow = await executionOutput.getJobWorkflow();

      expect(workflow).toBeInstanceOf(JobWorkflow);
      expect(workflow.workflow).toEqual([
        { exec: "echo test", type: "exec", nodeStep: "true" }
      ]);

      // Should cache the workflow
      const workflow2 = await executionOutput.getJobWorkflow();
      expect(workflow2).toBe(workflow);
      expect(api.get).toHaveBeenCalledTimes(2); // Only called API once for each endpoint
    });

    it("should handle workflow for non-job executions", async () => {
      // Mock API response for a command execution
      (api.get as jest.Mock).mockResolvedValueOnce({
        data: {
          id: "123",
          description: "Ad-hoc command",
          // No job property means it's an ad-hoc execution
        }
      });

      const workflow = await executionOutput.getJobWorkflow();

      expect(workflow).toBeInstanceOf(JobWorkflow);
      expect(workflow.workflow).toEqual([
        { exec: "Ad-hoc command", type: "exec", nodeStep: "true" }
      ]);
    });

    it("should get execution status", async () => {
      const mockStatus = {
        id: "123",
        status: "succeeded",
        executionType: "scheduled",
        project: "test",
      };

      (api.get as jest.Mock).mockResolvedValueOnce({
        data: mockStatus
      });

      const status = await executionOutput.getExecutionStatus();
      expect(status).toEqual(mockStatus);

      // Should cache the status
      const status2 = await executionOutput.getExecutionStatus();
      expect(status2).toBe(status);
      expect(api.get).toHaveBeenCalledTimes(1);
    });

    it("should fetch execution output through API", async () => {
      const mockOutput = {
        id: "123",
        offset: "100",
        completed: false,
        execCompleted: false,
        entries: [
          { time: "12:00:00", log: "Test log entry", level: "INFO", node: "node1" }
        ],
        totalSize: 200,
      };

      mockApiClientGet.mockResolvedValueOnce({
        data: mockOutput,
        status: 200,
      });

      const result = await executionOutput.getExecutionOutput("123", 0, 100);

      expect(result).toEqual(mockOutput);
    });

    it("should throw error for non-200 API responses", async () => {
      mockApiClientGet.mockResolvedValueOnce({
        data: { error: "Not found" },
        status: 404,
      });

      await expect(executionOutput.getExecutionOutput("123", 0, 100))
        .rejects
        .toThrow("Error calling execution log api:");
    });

    it("should observe entries with callback", () => {
      const callback = jest.fn();
      executionOutput.observeEntries(callback);

      // Add an entry to trigger the observer
      const entry = new ExecutionOutputEntry(executionOutput);
      Object.assign(entry, { log: "Test log" });
      executionOutput.entries.push(entry);

      expect(callback).toHaveBeenCalled();
    });
  });

  describe("ExecutionOutputEntry", () => {
    it("should create an entry from API response", () => {
      const executionOutput = new ExecutionOutput("123");
      const mockWorkflow = new JobWorkflow([
        { exec: "echo test", type: "exec", nodeStep: "true" }
      ]);

      const apiResponse = {
        time: "12:00:00",
        absolute_time: "2023-01-01T12:00:00Z",
        log: "Test log entry",
        loghtml: "<span>Test log entry</span>",
        level: "INFO",
        stepctx: "1",
        node: "node1",
        metadata: { key: "value" }
      };

      const entry = ExecutionOutputEntry.FromApiResponse(
        executionOutput,
        apiResponse,
        5,
        mockWorkflow
      );

      expect(entry.executionOutput).toBe(executionOutput);
      expect(entry.time).toBe("12:00:00");
      expect(entry.absoluteTime).toBe("2023-01-01T12:00:00Z");
      expect(entry.log).toBe("Test log entry");
      expect(entry.logHtml).toBe("<span>Test log entry</span>");
      expect(entry.level).toBe("INFO");
      expect(entry.stepctx).toBe("1");
      expect(entry.node).toBe("node1");
      expect(entry.lineNumber).toBe(5);
      expect(entry.renderedStep).toBeTruthy();
      expect(entry.meta).toEqual({ key: "value" });
    });
  });
});