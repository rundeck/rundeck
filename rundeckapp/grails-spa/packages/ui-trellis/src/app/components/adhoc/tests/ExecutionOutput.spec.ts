import { mount, VueWrapper } from "@vue/test-utils";
import ExecutionOutput from "../ExecutionOutput.vue";
import { AdhocCommandStore } from "../../../../library/stores/AdhocCommandStore";

describe("ExecutionOutput", () => {
  const mockEventBus = {
    on: jest.fn(),
    emit: jest.fn(),
    off: jest.fn(),
  };

  const mockAdhocCommandStore = {
    running: false,
    stopFollowing: jest.fn(),
  } as unknown as AdhocCommandStore;

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
        adhocCommandStore: mockAdhocCommandStore,
      },
    });

    // runcontent is conditionally rendered with v-if="showOutput", so it won't exist initially
    expect(wrapper.find(".col-sm-12").exists()).toBe(true);
  });

  it("should listen for ko-adhoc-running event", () => {
    mount(ExecutionOutput, {
      props: {
        eventBus: mockEventBus,
        pageParams: mockPageParams,
        adhocCommandStore: mockAdhocCommandStore,
      },
    });

    expect(mockEventBus.on).toHaveBeenCalledWith(
      "ko-adhoc-running",
      expect.any(Function)
    );
  });

  it("should show output when execution starts", () => {
    const wrapper = mount(ExecutionOutput, {
      props: {
        eventBus: mockEventBus,
        pageParams: mockPageParams,
        adhocCommandStore: mockAdhocCommandStore,
      },
    }) as VueWrapper<any>;

    wrapper.vm.handleExecutionStart({ id: "123" });

    expect(wrapper.vm.executionId).toBe("123");
    expect(wrapper.vm.showOutput).toBe(true);
  });
});

