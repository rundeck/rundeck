import { mount, VueWrapper } from "@vue/test-utils";
import AdhocCommandForm from "../AdhocCommandForm.vue";
import { NodeFilterStore } from "../../../../library/stores/NodeFilterLocalstore";

jest.mock("../../../../library", () => ({
  getAppLinks: jest.fn().mockReturnValue({
    adhocHistoryAjax: "/api/adhoc/history",
    scheduledExecutionRunAdhocInline: "/api/execution/runAdhoc",
  }),
}));

describe("AdhocCommandForm", () => {
  const mockEventBus = {
    on: jest.fn(),
    emit: jest.fn(),
    off: jest.fn(),
  };

  const mockNodeFilterStore = {
    selectedFilter: "name: test",
    setSelectedFilter: jest.fn(),
  } as unknown as NodeFilterStore;

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
    const wrapper = mount(AdhocCommandForm, {
      props: {
        nodeFilterStore: mockNodeFilterStore,
        project: "test-project",
        eventBus: mockEventBus,
        pageParams: mockPageParams,
        nodeTotal: 0,
        nodeError: null,
        initialCommandString: "",
      },
    });

    expect(wrapper.find("#runbox").exists()).toBe(true);
    expect(wrapper.find("#runFormExec").exists()).toBe(true);
  });

  it("should disable run button when no nodes", () => {
    const wrapper = mount(AdhocCommandForm, {
      props: {
        nodeFilterStore: mockNodeFilterStore,
        project: "test-project",
        eventBus: mockEventBus,
        pageParams: mockPageParams,
        nodeTotal: 0,
        nodeError: null,
        initialCommandString: "",
      },
    }) as VueWrapper<any>;

    expect(wrapper.vm.isRunDisabled).toBe(true);
  });

  it("should enable run button when nodes available", () => {
    const wrapper = mount(AdhocCommandForm, {
      props: {
        nodeFilterStore: mockNodeFilterStore,
        project: "test-project",
        eventBus: mockEventBus,
        pageParams: mockPageParams,
        nodeTotal: 5,
        nodeError: null,
        initialCommandString: "",
      },
    }) as VueWrapper<any>;

    expect(wrapper.vm.isRunDisabled).toBe(false);
  });
});

