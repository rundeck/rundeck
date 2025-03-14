import "isomorphic-fetch";

console.log(global.fetch);

import { ExecutionOutputStore } from "../../src/library/stores/ExecutionOutput";

import { observe } from "mobx";
import { RundeckBrowser } from "@rundeck/client";
import { RootStore } from "../../src/library/stores/RootStore";
import { RundeckToken } from "../../src/library/interfaces/rundeckWindow";
import { EventBus } from "../../src/library";
import { api } from "../../src/library/services/api";

jest.setTimeout(60000);

jest.mock("@/library/modules/rundeckClient", () => ({
  client: jest.fn(),
}));

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    client: {},
    eventBus: { on: jest.fn(), off: jest.fn(), emit: jest.fn() },
    rdBase: "http://localhost:4440/",
    projectName: "testProject",
    apiVersion: "44",
    rootStore: {
      plugins: {
        load: jest.fn(),
        getServicePlugins: jest.fn(),
      },
    },
  })),
}));
jest.mock("../../src/library/services/projects");
jest.mock("../../src/library/stores/RootStore", () => {
  return {
    __esModule: true,
    RootStore: jest.fn().mockImplementation(() => {
      return {};
    }),
  };
});

jest.mock("../../src/library/services/api", () => ({
  api: {
    get: jest.fn(),
    put: jest.fn(),
    post: jest.fn(),
  },
}));

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
    jest.resetAllMocks();
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
    (api.get as jest.Mock).mockImplementation((url, params) => {
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
      null,
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
