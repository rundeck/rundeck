import { mount, VueWrapper } from "@vue/test-utils";
import AdhocCommandForm from "../AdhocCommandForm.vue";
import { AdhocCommandStore } from "../../../../library/stores/AdhocCommandStore";
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

  const mockAdhocCommandStore = {
    commandString: "",
    recentCommands: [],
    recentCommandsLoaded: false,
    recentCommandsNoneFound: false,
    running: false,
    canRun: false,
    allowInput: false,
    loadRecentCommands: jest.fn(),
  } as unknown as AdhocCommandStore;

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
        adhocCommandStore: mockAdhocCommandStore,
        nodeFilterStore: mockNodeFilterStore,
        project: "test-project",
        eventBus: mockEventBus,
        pageParams: mockPageParams,
        nodeTotal: 0,
        nodeError: null,
      },
    });

    expect(wrapper.find("#runbox").exists()).toBe(true);
    expect(wrapper.find("#runFormExec").exists()).toBe(true);
  });

  it("should disable run button when no nodes", () => {
    const wrapper = mount(AdhocCommandForm, {
      props: {
        adhocCommandStore: mockAdhocCommandStore,
        nodeFilterStore: mockNodeFilterStore,
        project: "test-project",
        eventBus: mockEventBus,
        pageParams: mockPageParams,
        nodeTotal: 0,
        nodeError: null,
      },
    }) as VueWrapper<any>;

    expect(wrapper.vm.isRunDisabled).toBe(true);
  });

  it("should enable run button when nodes available", () => {
    const storeWithCanRun = {
      ...mockAdhocCommandStore,
      canRun: true,
      allowInput: true,
    } as unknown as AdhocCommandStore;

    const wrapper = mount(AdhocCommandForm, {
      props: {
        adhocCommandStore: storeWithCanRun,
        nodeFilterStore: mockNodeFilterStore,
        project: "test-project",
        eventBus: mockEventBus,
        pageParams: mockPageParams,
        nodeTotal: 5,
        nodeError: null,
      },
    }) as VueWrapper<any>;

    expect(wrapper.vm.isRunDisabled).toBe(false);
  });
});

