import { mount, VueWrapper } from "@vue/test-utils";
import App from "../App.vue";
import { NodeFilterStore } from "../../../../library/stores/NodeFilterLocalstore";
import { AdhocCommandStore } from "../../../../library/stores/AdhocCommandStore";

jest.mock("../../../utilities/loadJsonData", () => ({
  loadJsonData: jest.fn((id: string) => {
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

jest.mock("../../../../library/services/api", () => ({
  apiClient: jest.fn(() => ({
    get: jest.fn(),
    post: jest.fn(),
  })),
}));

jest.mock("../../../../library", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    projectName: "test-project",
    apiVersion: "44",
    rdBase: "http://localhost:4440",
    data: {
      jobslistDateFormatMoment: "YYYY-MM-DD",
      runningDateFormatMoment: "YYYY-MM-DD HH:mm:ss",
    },
  }),
  getAppLinks: jest.fn().mockReturnValue({
    frameworkNodesQueryAjax: "/api/nodes/query",
  }),
}));

describe("AdhocApp", () => {
  const mockEventBus = {
    on: jest.fn(),
    emit: jest.fn(),
    off: jest.fn(),
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
    expect(wrapper.vm.adhocCommandStore).toBeInstanceOf(AdhocCommandStore);
  });
});

