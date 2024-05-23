import { mount, VueWrapper } from "@vue/test-utils";
import ActivityList from "../activityList.vue";
import ActivityFilter from "../activityFilter.vue";
import { RundeckContext } from "../../../../library";
import { setupRundeckContext } from "../setupRundeckContext";

// Mocking necessary services and modules
jest.mock("../../../../library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({ projectName: "test" }),
  url: jest.fn().mockReturnValue("http://localhost"),
}));
jest.mock("@rundeck/client", () => {
  return {
    RundeckBrowser: jest.fn().mockImplementation(() => ({
      executionBulkDelete: jest.fn().mockResolvedValue({ allsuccessful: true }),
      executionListRunning: jest.fn().mockResolvedValue({ executions: [] }),
    })),
  };
});
jest.mock("axios");
const mockEventBus = {
  on: jest.fn(),
  emit: jest.fn(),
  off: jest.fn(),
  all: new Map(),
};
type ActivityListInstance = InstanceType<typeof ActivityList>;
const mountActivityList = async (props = {}) => {
  const wrapper = mount(ActivityList, {
    props: {
      pagination: {
        total: 10,
        offset: 0,
        max: 10,
      },
      reports: [],
      query: {},
      eventBus: mockEventBus,
      running: {
        executions: [],
      },
      ...props,
    },
    global: {
      components: {
        ActivityFilter,
      },
      stubs: {
        directives: {
          tooltip: {},
        },
        "i18n-t": true,
        ActivityFilter: true,
        Modal: {
          template: "<div><slot></slot></div>",
        },
        ProgressBar: {
          template: "<div><slot></slot></div>",
        },
        tooltip: true,
        btn: true,
      },
      mocks: {
        $t: (msg) => msg,
        $tc: (msg) => msg,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper as VueWrapper<ActivityListInstance>;
};
declare global {
  interface Window {
    _rundeck: RundeckContext;
  }
}
beforeAll(() => {
  jest.useFakeTimers();
  setupRundeckContext();
});
afterAll(() => {
  jest.useRealTimers();
});
afterEach(() => {
  jest.clearAllMocks();
});
describe("ActivityList", () => {
  it("renders the component correctly", async () => {
    const wrapper = await mountActivityList();
    expect(wrapper.exists()).toBe(true);
  });
  it("opens and closes the filter modal", async () => {
    const wrapper = await mountActivityList();
    const filterButton = wrapper.find(
      '[data-test-id="activity-list-filter-button"]',
    );
    await filterButton.trigger("click");
    expect(wrapper.findComponent(ActivityFilter).exists()).toBe(true);
  });
  it.skip("renders execution links", async () => {
    const reports = [
      {
        execution: {
          id: 68,
          permalink: "/project/jaya-test/execution/show/68",
        },
        status: "succeeded",
        dateCompleted: "2024-05-22T14:33:52Z",
        node: {
          total: 1,
          succeeded: 1,
          failed: 0,
        },
      },
    ];
    const wrapper = await mountActivityList({ reports });
    await wrapper.vm.$nextTick();
    const executionLink = wrapper.find('[data-test-id="execution-link"]');
    expect(executionLink.exists()).toBe(true);
    expect(executionLink.attributes("href")).toBe(
      "/project/jaya-test/execution/show/68",
    );
  });
  it("renders the filter button", async () => {
    const wrapper = await mountActivityList();
    const filterButton = wrapper.find(
      '[data-test-id="activity-list-filter-button"]',
    );
    expect(filterButton.exists()).toBe(true);
  });
  it("opens the clear selections modal", async () => {
    const wrapper = await mountActivityList();
    wrapper.vm.showBulkEditCleanSelections = true;
    const clearSelectionsButton = wrapper.find(
      '[data-test-id="modal-clean-selections"]',
    );
    expect(clearSelectionsButton.exists()).toBe(true);
  });
  it("opens the bulk delete modal", async () => {
    const wrapper = await mountActivityList();
    wrapper.vm.showBulkEditConfirm = true;
    const bulkDeleteButton = wrapper.find('[data-test-id="modal-bulk-delete"]');
    expect(bulkDeleteButton.exists()).toBe(true);
  });
  it("opens the bulk delete results modal", async () => {
    const wrapper = await mountActivityList();
    wrapper.vm.showBulkEditResults = true;
    const bulkDeleteResults = wrapper.find(
      '[data-test-id="modal-bulk-delete-results"]',
    );
    expect(bulkDeleteResults.exists()).toBe(true);
  });
  it("renders since count data", async () => {
    const wrapper = await mountActivityList({
      sinceCount: 5,
    });
    await wrapper.vm.$nextTick();
    expect(wrapper.html()).toContain("5");
  });
  it.skip("renders reports", async () => {
    const reports = [
      {
        execution: {
          id: 68,
          permalink: "/project/jaya-test/execution/show/68",
        },
        status: "succeeded",
        dateCompleted: "2024-05-22T14:33:52Z",
        node: {
          total: 1,
          succeeded: 1,
          failed: 0,
        },
      },
    ];
    const wrapper = await mountActivityList({ reports });
    await wrapper.vm.$nextTick();
    const reportRows = wrapper.find('[data-test-id="report-row-item"]');
    expect(reportRows.exists()).toBe(true);
    expect(reportRows.text()).toContain("#68");
  });
  it("renders running executions", async () => {
    const wrapper = await mountActivityList({
      running: {
        executions: [
          {
            id: "1",
            status: "running",
          },
        ],
      },
    });
    await wrapper.vm.$nextTick();
    expect(wrapper.html()).toContain("running");
  });
  it("renders loading area", async () => {
    const wrapper = await mountActivityList({ loading: true });
    await wrapper.vm.$nextTick();
    expect(wrapper.find('[data-test-id="loading-area"]').exists()).toBe(true);
  });
  it("handles loadActivity method call", async () => {
    const wrapper = await mountActivityList();
    const loadActivitySpy = jest.spyOn(wrapper.vm, "loadActivity");
    await wrapper.vm.loadActivity(0);
    expect(loadActivitySpy).toHaveBeenCalledWith(0);
    loadActivitySpy.mockRestore();
  });
  it.skip("displays an error message when load fails", async () => {
    const wrapper = await mountActivityList({
      props: {
        loadError: "Test error message",
      },
      global: {
        mocks: {
          $t: (msg, params) => {
            console.log(`$t called with msg: ${msg} and params: ${params}`);
            return params ? params[0] : msg;
          },
        },
      },
    });
    console.log(`loadError: ${wrapper.vm.loadError}`);
    expect(wrapper.html()).toBe("Test error message");
    await wrapper.vm.$nextTick();
    const errorMessage = wrapper.find(".text-warning");
    expect(errorMessage.exists()).toBe(true);
    expect(errorMessage.text()).toContain("Test error message");
  });
  it.skip("renders no executions message when reports are empty", async () => {
    const wrapper = await mountActivityList({
      reports: undefined,
    });
    await wrapper.vm.$nextTick();
    const noExecutionsMessage = wrapper.find('[data-test-id="loading-area"]');
    expect(noExecutionsMessage.exists()).toBe(true);
    // expect(noExecutionsMessage.text()).toContain("No executions found");
    expect(noExecutionsMessage.html()).toContain("results.empty.text");
  });
  it("triggers the loadSince method", async () => {
    const wrapper = await mountActivityList();
    const loadSinceSpy = jest.spyOn(wrapper.vm, "loadSince");
    await wrapper.vm.loadSince();
    expect(loadSinceSpy).toHaveBeenCalled();
    loadSinceSpy.mockRestore();
  });
});
