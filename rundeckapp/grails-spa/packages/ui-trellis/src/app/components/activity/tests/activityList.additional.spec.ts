import {
  rundeckServiceMock,
  rundeckClientMock,
  axiosMock,
  i18nMocks,
  mockEventBus,
} from "./mock";
import { shallowMount, VueWrapper } from "@vue/test-utils";
import ActivityList from "../activityList.vue";
import ActivityFilter from "../activityFilter.vue";
import OffsetPagination from "../../../../library/components/utils/OffsetPagination.vue";
import { setupRundeckContext } from "./setupRundeckContext";
import { GlobalOptions } from "./type";
jest.mock("../../../../library/rundeckService", () => rundeckServiceMock);
jest.mock("@rundeck/client", () => rundeckClientMock);
jest.mock("axios", () => axiosMock);
type ActivityListInstance = InstanceType<typeof ActivityList>;
const shallowMountActivityList = async (
  props = {},
  globalOptions: GlobalOptions = {},
) => {
  const wrapper = shallowMount(ActivityList, {
    props: {
      eventBus: mockEventBus,
      displayMode: "mode",
      ...props,
    },
    global: {
      components: {
        OffsetPagination,
        ActivityFilter,
      },
      mocks: {
        ...i18nMocks,
        ...globalOptions.mocks,
      },
      directives: {
        tooltip: () => {},
      },
      ...globalOptions,
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper as VueWrapper<ActivityListInstance>;
};
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
describe("ActivityList Component", () => {
  it("renders loading area when query changes", async () => {
    const wrapper = await shallowMountActivityList();
    wrapper.vm.query.jobIdFilter = "test";
    await wrapper.vm.$nextTick();
    expect(wrapper.find('[data-test-id="loading-area"]').exists()).toBe(true);
  });
  it("displays an error message when load fails", async () => {
    const wrapper = await shallowMountActivityList();
    wrapper.vm.loadError = "An Error Occurred";
    await wrapper.vm.$nextTick();
    const errorMessage = wrapper
      .find('[data-test-id="loading-area"]')
      .find(".text-warning");
    expect(errorMessage.exists()).toBe(true);
    expect(errorMessage.text()).toContain("An Error Occurred");
  });
  it("displays a no data message", async () => {
    const wrapper = await shallowMountActivityList();
    wrapper.vm.reports = [];
    wrapper.vm.loading = false;
    wrapper.vm.loadError = null;
    await wrapper.vm.$nextTick();
    const noDataMessage = wrapper.find('[data-test="no-data-message"]');
    expect(noDataMessage.exists()).toBe(true);
    expect(noDataMessage.text()).toBe("No results for the query");
  });
  it("renders since count data", async () => {
    const wrapper = await shallowMountActivityList();
    wrapper.vm.sincecount = 5;
    await wrapper.vm.$nextTick();
    const sinceCountTd = wrapper.find(".since-count-data");
    expect(sinceCountTd.exists()).toBe(true);
    expect(sinceCountTd.text()).toContain("info.newexecutions.since.0");
  });
  it("renders the pagination correctly", async () => {
    const wrapper = await shallowMountActivityList();
    wrapper.vm.pagination = { total: 50, offset: 10, max: 10 };
    await wrapper.vm.$nextTick();
    const pagination = wrapper.findComponent(OffsetPagination);
    expect(pagination.exists()).toBe(true);
  });
  it("renders the filter button", async () => {
    const wrapper = await shallowMountActivityList();
    const filterButton = wrapper.find(
      '[data-test-id="activity-list-filter-button"]',
    );
    expect(filterButton.exists()).toBe(true);
  });
  it("renders running executions", async () => {
    const wrapper = await shallowMountActivityList();
    wrapper.vm.running = {
      executions: [
        {
          id: "1",
          status: "running",
          dateStarted: { date: new Date() },
        },
      ],
      paging: {},
    };
    await wrapper.vm.$nextTick();
    const execution = wrapper.vm.running.executions[0];
    expect(execution).toHaveProperty("id", "1");
    expect(execution).toHaveProperty("status", "running");
    expect(execution.dateStarted).toHaveProperty("date");
  });
});

describe("ActivityList Bulk Edit Modals", () => {
  it("opens the bulk edit modal", async () => {
    const wrapper = await shallowMountActivityList();
    wrapper.vm.auth = { deleteExec: true };
    wrapper.vm.pagination = { total: 1 };
    wrapper.vm.bulkEditMode = true;
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.bulkEditMode).toBe(true);
  });
  it("open and closes the filter modals", async () => {
    const wrapper = await shallowMountActivityList();
    wrapper.vm.showFilters = true;
    await wrapper.vm.$nextTick();
    expect(wrapper.findComponent(ActivityFilter).exists()).toBe(true);
    wrapper.vm.showFilters = false;
    await wrapper.vm.$nextTick();
    expect(wrapper.findComponent(ActivityFilter).exists()).toBe(false);
  });
  it("opens the bulk delete modal when delete selected executions button is clicked", async () => {
    const wrapper = await shallowMountActivityList();

    // Set the data properties
    wrapper.vm.auth = { deleteExec: true };
    wrapper.vm.pagination = { total: 1 };
    wrapper.vm.showBulkDelete = true;
    wrapper.vm.bulkEditMode = true;
    wrapper.vm.bulkSelectedIds = ["1"];
    await wrapper.vm.$nextTick();

    const deleteSelectedExecutionsButton = wrapper.find(
      '[data-test-id="activity-list-delete-selected-executions"]',
    );
    await deleteSelectedExecutionsButton.trigger("click");

    // Wait for the next DOM update
    await wrapper.vm.$nextTick();

    expect(wrapper.vm.showBulkDelete).toBe(true);
  });

  it("closes the bulk delete modal when the cancel button is clicked", async () => {
    const wrapper = await shallowMountActivityList();

    // Set the data properties
    wrapper.vm.auth = { deleteExec: true };
    wrapper.vm.pagination = { total: 1 };
    wrapper.vm.showBulkDelete = true;
    wrapper.vm.bulkEditMode = true;
    wrapper.vm.bulkSelectedIds = ["1"];
    await wrapper.vm.$nextTick();

    const cancelButton = wrapper.find('[data-test-id="bulk-delete-cancel"]');
    await cancelButton.trigger("click");

    // Wait for the next DOM update
    await wrapper.vm.$nextTick();

    expect(wrapper.vm.showBulkDelete).toBe(false);
  });
});
it("enables bulk delete button when items are selected", async () => {
  const wrapper = await shallowMountActivityList();
  wrapper.vm.reports = [
    {
      execution: {
        id: "2",
        permalink: "/project/test/execution/show/2",
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
  wrapper.vm.bulkEditMode = true;
  wrapper.vm.bulkSelectedIds = ["2"];
  await wrapper.vm.$nextTick();
  expect(wrapper.vm.bulkSelectedIds.length).toBeGreaterThan(0);
});
describe("ActivityList Miscellaneous", () => {
  it("toggles auto-refresh", async () => {
    const wrapper = await shallowMountActivityList();
    const autoRefreshCheckbox = wrapper.find("#auto-refresh");
    await autoRefreshCheckbox.setValue(true);
    expect(wrapper.vm.autorefresh).toBe(true);
    await autoRefreshCheckbox.setValue(false);
    expect(wrapper.vm.autorefresh).toBe(false);
  });
  it("handles pagination correctly", async () => {
    const wrapper = await shallowMountActivityList();
    const paginationSpy = jest.spyOn(wrapper.vm, "changePageOffset");
    const pagination = wrapper.findComponent({ name: "OffsetPagination" });
    pagination.vm.$emit("change", 10);
    await wrapper.vm.$nextTick();
    expect(paginationSpy).toHaveBeenCalledWith(10);
    paginationSpy.mockRestore();
  });
  it("navigates to execution link", async () => {
    const wrapper = await shallowMountActivityList();
    // Mock window.location
    const originalLocation = window.location;
    const mockLocation = { assign: jest.fn(), href: "" };
    // @ts-ignore
    delete window.location;
    // @ts-ignore
    window.location = mockLocation;
    // Simulate the data loading
    wrapper.vm.loadActivity = jest.fn().mockImplementation(() => {
      wrapper.vm.reports = [
        {
          execution: {
            id: "1",
            permalink: "project/test/execution/show/1",
          },
          status: "succeeded",
          dateCompleted: "2024-05-22T14:33:52Z",
          node: { total: 1, succeeded: 1, failed: 0 },
        },
      ];
    });

    await wrapper.vm.$nextTick();
    console.log("Reports data in wrapper:", wrapper.vm.reports);
    // Simulate the click on the report row item
    const reportRowItem = wrapper.find('[data-test-id="report-row-item"]');
    await reportRowItem.trigger("click");
    console.log("After clicking report row item:", wrapper.html());
    // Assert that navigation function was called with expected URL
    expect(mockLocation.assign).toHaveBeenCalledWith(
      "http://localhost:4440/project/test/execution/show/1",
    );
    // Restore original window.location
    window.location = originalLocation;
  });
});
