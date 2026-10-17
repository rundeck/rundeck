import { vi } from "vitest";
import { mount, VueWrapper } from "@vue/test-utils";
import ExecutionOutput from "../ExecutionOutput.vue";

// Mock CancelToken before importing components that use logViewer
vi.mock("@esfx/canceltoken", () => ({
  CancelToken: {
    source: vi.fn(() => ({
      token: { signaled: false },
      cancel: vi.fn(),
    })),
  },
}));

vi.mock("../../../../library/services/api", () => ({
  apiClient: vi.fn(() => ({
    get: vi.fn(),
    post: vi.fn(),
  })),
  api: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

vi.mock("../../../../library/rundeckService", () => ({
  getRundeckContext: vi.fn().mockReturnValue({
    apiVersion: "44",
    rdBase: "http://localhost:4440",
    projectName: "test-project",
    rootStore: {
      theme: { theme: "dark" },
      executionOutputStore: {
        createOrGet: vi.fn().mockReturnValue({ init: vi.fn() }),
      },
    },
  }),
}));

vi.mock("../../../../library", () => ({
  getRundeckContext: vi.fn().mockReturnValue({
    apiVersion: "44",
    rdBase: "http://localhost:4440",
    projectName: "test-project",
    rootStore: {
      theme: { theme: "dark" },
      executionOutputStore: {
        createOrGet: vi.fn().mockReturnValue({ init: vi.fn() }),
      },
    },
  }),
  getAppLinks: vi.fn().mockReturnValue({
    executionOutputAjax: "/api/execution/output",
  }),
}));

describe("ExecutionOutput", () => {
  const mockEventBus = {
    on: vi.fn(),
    emit: vi.fn(),
    off: vi.fn(),
  };

  const mockPageParams = {
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

  it("should render correctly", () => {
    const wrapper = mount(ExecutionOutput, {
      props: {
        eventBus: mockEventBus,
        pageParams: mockPageParams,
        showHeader: false,
      },
    });

    // runcontent is conditionally rendered with v-if="showOutput", so it won't exist initially
    expect(wrapper.find(".col-sm-12").exists()).toBe(true);
  });

  it("should show output when executionId prop is set", async () => {
    const wrapper = mount(ExecutionOutput, {
      props: {
        executionId: "123",
        eventBus: mockEventBus,
        pageParams: mockPageParams,
        showHeader: false,
      },
      global: {
        stubs: { LogViewer: true },
      },
    }) as VueWrapper<any>;

    // Wait for mounted hook to process the executionId
    await wrapper.vm.$nextTick();

    expect(wrapper.vm.executionId).toBe("123");
    expect(wrapper.vm.showOutput).toBe(true);
  });

  it("should show output when execution starts via handleExecutionStart", async () => {
    const wrapper = mount(ExecutionOutput, {
      props: {
        executionId: null,
        eventBus: mockEventBus,
        pageParams: mockPageParams,
        showHeader: false,
      },
      global: {
        stubs: { LogViewer: true },
      },
    }) as VueWrapper<any>;

    // Set executionId prop and call handleExecutionStart
    await wrapper.setProps({ executionId: "123" });
    await wrapper.vm.$nextTick();

    expect(wrapper.vm.executionId).toBe("123");
    expect(wrapper.vm.showOutput).toBe(true);
  });
});
