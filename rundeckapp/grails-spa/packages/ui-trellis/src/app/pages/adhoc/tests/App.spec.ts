import { vi } from "vitest";
import { mount, VueWrapper } from "@vue/test-utils";
import App from "../App.vue";
import { NodeFilterStore } from "../../../../library/stores/NodeFilterLocalstore";
// AdhocCommandStore removed - state is now local to AdhocCommandForm

// Mock CancelToken before importing components that use logViewer
vi.mock("@esfx/canceltoken", () => ({
  CancelToken: {
    source: vi.fn(() => ({
      token: { signaled: false },
      cancel: vi.fn(),
    })),
  },
}));

vi.mock("../../../utilities/loadJsonData", () => ({
  loadJsonData: vi.fn((id: string) => {
    if (id === "filterParamsJSON") {
      return { matchedNodesMaxCount: 50, filter: "", filterAll: false };
    }
    if (id === "pageParams") {
      return {
        disableMarkdown: "",
        smallIconUrl: "",
        iconUrl: "",
        lastlines: 20,
        maxLastLines: 20,
        emptyQuery: null,
        ukey: "",
        project: "test-project",
        runCommand: "",
        adhocKillAllowed: false,
      };
    }
    return null;
  }),
}));

vi.mock("../../../../library/services/api", () => ({
  apiClient: vi.fn(() => ({
    get: vi.fn(),
    post: vi.fn(),
  })),
}));

vi.mock("../../../../library/rundeckService", () => ({
  getRundeckContext: vi.fn().mockReturnValue({
    projectName: "test-project",
    apiVersion: "44",
    rdBase: "http://localhost:4440",
    rootStore: {
      theme: { theme: "dark" },
      executionOutputStore: {
        createOrGet: vi.fn(),
      },
      ui: {
        itemsForLocation: vi.fn(() => []),
        addWatcher: vi.fn(),
      },
    },
  }),
}));

vi.mock("../../../../library", () => ({
  getRundeckContext: vi.fn().mockReturnValue({
    projectName: "test-project",
    apiVersion: "44",
    rdBase: "http://localhost:4440",
    rootStore: {
      theme: { theme: "dark" },
      executionOutputStore: {
        createOrGet: vi.fn(),
      },
      ui: {
        itemsForLocation: vi.fn(() => []),
        addWatcher: vi.fn(),
      },
    },
    data: {
      jobslistDateFormatMoment: "YYYY-MM-DD",
      runningDateFormatMoment: "YYYY-MM-DD HH:mm:ss",
    },
  }),
  getAppLinks: vi.fn().mockReturnValue({
    frameworkNodesQueryAjax: "/api/nodes/query",
  }),
}));

describe("AdhocApp", () => {
  const mockEventBus = {
    on: vi.fn(),
    emit: vi.fn(),
    off: vi.fn(),
  };

  beforeEach(() => {
    (window as any)._rundeck = {
      data: {
        eventReadAuth: true,
        executionModeActive: true,
      },
    };
  });

  it("should render correctly", async () => {
    const wrapper = mount(App, {
      props: {
        eventBus: mockEventBus,
      },
    }) as VueWrapper<any>;

    // Wait for mounted() to complete and stores to be initialized
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick(); // Extra tick for child components to render

    expect(wrapper.find("#adhoc-app").exists()).toBe(true);
  });

  it("should initialize stores", async () => {
    const wrapper = mount(App, {
      props: {
        eventBus: mockEventBus,
      },
    }) as VueWrapper<any>;

    // Wait for mounted() to complete and stores to be initialized
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick(); // Extra tick for initialization

    expect(wrapper.vm.nodeFilterStore).toBeInstanceOf(NodeFilterStore);
    // AdhocCommandStore removed - state is now local to AdhocCommandForm
  });
});
