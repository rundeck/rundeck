import { vi } from "vitest";
import "isomorphic-fetch";

console.log(global.fetch);

import { api, apiClient } from "../../src/library/services/api";
import { ExecutionOutputStore } from "../../src/library/stores/ExecutionOutput";

import { observe } from "mobx";
import { RundeckBrowser, RundeckClient } from "@rundeck/client";
import { RootStore } from "../../src/library/stores/RootStore";
import { RundeckToken } from "../../src/library/interfaces/rundeckWindow";
import { EventBus } from "../../src/library";

vi.setConfig({ testTimeout: 60000 });

vi.mock("../../src/library/services/api", () => {
  const api = {
    get: vi.fn(),
    put: vi.fn(),
    post: vi.fn(),
  };
  return {
    api,
    apiClient: vi.fn().mockImplementation((vers) => api),
  };
});

vi.mock("@/library/rundeckService", () => ({
  getRundeckContext: vi.fn().mockImplementation(() => ({
    client: {},
    eventBus: { on: vi.fn(), off: vi.fn(), emit: vi.fn() },
    rdBase: "http://localhost:4440",
    projectName: "testProject",
    apiVersion: "44",
    rootStore: {
      plugins: {
        load: vi.fn(),
        getServicePlugins: vi.fn(),
      },
    },
  })),
}));
vi.mock("../../src/library/services/projects");
vi.mock("../../src/library/stores/RootStore", () => {
  return {
    __esModule: true,
    RootStore: vi.fn().mockImplementation(() => {
      return {};
    }),
  };
});

describe("ExecutionOutput Store", () => {
  beforeEach(() => {
    window._rundeck = {
      activeTour: "",
      activeTourStep: "",
      apiVersion: "43",
      appMeta: {},
      data: {},
      eventBus: {} as typeof EventBus,
      feature: {},
      navbar: {
        items: [],
      },
      projectName: "test",
      rdBase: "/",
      rootStore: {} as unknown as RootStore,
      rundeckClient: {} as RundeckBrowser,
      token: {} as RundeckToken,
      tokens: {},
    };
    vi.resetAllMocks();
    (apiClient as vi.MockedFn<any>).mockImplementation((vers) => api);
  });

  it("Loads Output", async () => {
    // Mock api responses needed for the test
    const mockOutputResponse = {
      data: {
        entries: [],
        completed: true,
        execCompleted: true,
        offset: "0",
        totalSize: 100,
      },
      status: 200,
    };

    // Mock the execution status response
    const mockStatusResponse = {
      data: {
        id: "900",
        description: "Test execution",
        job: { id: "test-job-id" },
      },
      status: 200,
    };

    // Mock the job workflow response
    const mockWorkflowResponse = {
      data: {
        workflow: [{ exec: "echo test", type: "exec", nodeStep: "true" }],
      },
      status: 200,
    };

    // Set up the api mocks
    (api.get as vi.Mock).mockImplementation((url, params) => {
      if (url.includes("execution/900/output")) {
        return Promise.resolve(mockOutputResponse);
      } else if (url.includes("execution/900")) {
        return Promise.resolve(mockStatusResponse);
      } else if (url.includes("job/test-job-id/workflow")) {
        return Promise.resolve(mockWorkflowResponse);
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    const executionOutputStore = new ExecutionOutputStore(
      window._rundeck.rootStore,
      {} as RundeckClient,
    );

    const output = executionOutputStore.createOrGet("900");

    const intercepter = observe(output.entries, (change) => {
      console.log(change.type == "splice");
      if (change.type == "splice") {
        console.log(change.index);
        console.log(change.addedCount);
      }
    });

    let finished = false;
    while (!finished) {
      const output = await executionOutputStore.getOutput("900", 200);
      finished = output.completed;
    }

    // Verify api.get was called with expected parameters
    expect(api.get).toHaveBeenCalledWith(
      "execution/900/output",
      expect.objectContaining({
        params: expect.objectContaining({
          offset: "0",
          maxlines: "200",
        }),
      }),
    );

    expect(api.get).toHaveBeenCalledWith("execution/900");
  });
});
