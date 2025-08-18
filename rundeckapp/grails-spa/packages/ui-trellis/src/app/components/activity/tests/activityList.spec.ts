import {
  axiosMock,
  i18nMocks,
  mockEventBus,
  mockReports,
  rundeckClientMock,
  rundeckServiceMock,
  mockQueryRunning,
} from "../mocks/mock";
import { flushPromises, shallowMount, VueWrapper } from "@vue/test-utils";
import ActivityList from "../activityList.vue";
import ActivityFilter from "../activityFilter.vue";
import OffsetPagination from "../../../../library/components/utils/OffsetPagination.vue";
import { cloneDeep } from "lodash";
import { Btn, Modal } from "uiv";
jest.mock("../../../../library/services/executions", () => {
  return {
    queryRunning: mockQueryRunning,
  };
});
jest.mock("../../../../library/rundeckService", () => rundeckServiceMock);
jest.mock("@rundeck/client", () => rundeckClientMock);
jest.mock("axios", () => axiosMock);

// Add this mock for the api service
jest.mock("../../../../library/services/api", () => ({
  api: {
    post: jest.fn(),
    get: jest.fn(),
    put: jest.fn(),
  },
}));

// Import the API mock
import { api } from "../../../../library/services/api";

const mockRunningExecutions = mockReports.map((report) => report.execution);
type ActivityListInstance = InstanceType<typeof ActivityList>;
const shallowMountActivityList = async (
  props = {},
  options: Record<string, any> = {}, // Use a more generic type for options
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
        Modal,
        Btn,
      },
      stubs: {
        modal: false,
        btn: false,
        "i18n-t": true,
        ProgressBar: true,
      },
      mocks: {
        ...i18nMocks,
      },
      directives: {
        tooltip: () => {},
      },
      ...options, // Spread the generic options object
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper as VueWrapper<ActivityListInstance>;
};
let reports;
describe("ActivityList", () => {
  beforeAll(() => {
    jest.useFakeTimers();
  });
  afterAll(() => {
    jest.restoreAllMocks();
    jest.useRealTimers();
  });
  afterEach(() => {
    jest.clearAllMocks();
  });
  beforeEach(() => {
    reports = cloneDeep(mockReports);
    mockQueryRunning.mockResolvedValue({
      results: mockRunningExecutions,
      paging: { max: 20, offset: 0 },
    });
    axiosMock.get.mockImplementation((url) => {
      if (url.includes("eventsAjax")) {
        return {
          data: {
            total: 20,
            offset: 0,
            reports: reports,
          },
        };
      }
    });
  });

  describe("renders", () => {
    it("information about the page being shown and total number of results", async () => {
      const wrapper = await shallowMountActivityList();
      const pageInfoSpan = wrapper.find('[data-testid="page-info"]');
      const summaryCount = wrapper.find('[data-testid="summary-count"]');
      expect(pageInfoSpan.text()).toBe("1 - 2 of");
      expect(summaryCount.text()).toBe("20 executions");
    });

    it("information about the current executions and finished jobs", async () => {
      const wrapper = await shallowMountActivityList();
      await wrapper.vm.$nextTick();
      const executionItems = wrapper.findAll('[data-testid="execution-link"]');
      expect(executionItems.length).toBe(2); // Directly using the expected length value
      const reportItems = wrapper.findAll('[data-testid="report-row-item"]');
      expect(reportItems.length).toBe(2);
      expect(mockEventBus.emit).toHaveBeenCalledWith(
        "activity-nowrunning-count",
        2,
      );
    });

    it("error message when loading(apis) fails", async () => {
      axiosMock.get.mockRejectedValue(new Error("An error occurred"));
      const wrapper = await shallowMountActivityList();
      await wrapper.vm.$nextTick();
      const errorMessage = wrapper.find('[data-testid="error-message"]');
      expect(errorMessage.text()).toContain("An Error Occurred");
    });

    it("message when there is no info available", async () => {
      axiosMock.get.mockImplementation((url) => {
        if (url.includes("eventsAjax")) {
          return Promise.resolve({
            data: {
              reports: [],
              total: 0,
              offset: 0,
              lastDate: -1,
            },
          });
        }
        if (url.includes("running")) {
          return Promise.resolve({
            data: {
              executions: [],
            },
          });
        }
        return Promise.resolve({ data: {} });
      });
      const wrapper = await shallowMountActivityList();
      await wrapper.vm.$nextTick();
      const noInfoMessage = wrapper.find('[data-testid="no-data-message"]');
      expect(noInfoMessage.text()).toBe("No results for the query");
    });
  });

  describe("trigger bulk actions", () => {
    it("trigger bulk actions - delete", async () => {
      const wrapper = await shallowMountActivityList();
      await wrapper.vm.$nextTick();
      const bulkDeleteButton = wrapper.find(
        '[data-testid="activity-list-bulk-delete"]',
      );
      await bulkDeleteButton.trigger("click");
      await wrapper.vm.$nextTick();
      const checkboxes = wrapper.findAll(
        '[data-testid="bulk-delete-checkbox"]',
      );
      await checkboxes[0]!.setValue(true);
      await checkboxes[1]!.setValue(true);
      axiosMock.get.mockImplementationOnce((url) => {
        if (url.includes("eventsAjax")) {
          // Assuming 2 reports were deleted
          return Promise.resolve({
            data: {
              total: reports.length - 2,
              offset: 0,
              reports: reports.slice(2),
            },
          });
        }
        return Promise.resolve({ data: {} });
      });

      // Mock the api post for bulk deletion
      (api.post as jest.Mock).mockResolvedValue({
        data: { allsuccessful: true },
      });

      const confirmDeleteButton = wrapper.find(
        '[data-testid="delete-selected-executions"]',
      );
      await confirmDeleteButton.trigger("click");
      await flushPromises();
      const modal = wrapper.findComponent({ ref: "bulkexecdeleteresult" });
      await wrapper.vm.$nextTick();
      const modalConfirmDeleteButton = modal.find(
        '[data-testid="confirm-delete"]',
      );
      await modalConfirmDeleteButton.trigger("click");

      await wrapper.vm.$nextTick();
      expect(api.post).toHaveBeenCalledWith("executions/delete", { ids: [42] });
      const reportRows = wrapper.findAll('[data-testid="report-row-item"]');
      expect(reportRows.length).toBe(reports.length - 2); // Update expected length based on deletion
    });

    it("trigger bulk actions - search", async () => {
      axiosMock.get.mockImplementation((url, data) => {
        if (url.includes("eventsAjax")) {
          if (data?.params.recentFilter.length > 0) {
            return Promise.resolve({
              data: {
                total: 0,
                offset: 0,
                reports: [{ ...reports[0], node: "1/0/1" }],
              },
            });
          }
          return Promise.resolve({
            data: {
              total: reports.length,
              offset: 0,
              reports: reports,
            },
          });
        }
        return Promise.resolve({ data: {} });
      });
      const wrapper = await shallowMountActivityList();
      await wrapper.vm.$nextTick();
      const filterButton = wrapper.find(
        '[data-testid="activity-list-filter-button"]',
      );
      await filterButton.trigger("click");
      await wrapper.vm.$nextTick();
      const activityFilter = wrapper.findComponent(ActivityFilter);
      await activityFilter.vm.$emit("update:modelValue", {
        recentFilter: "testJobId",
      });
      await flushPromises();
      // Verify the results
      const reportItems = wrapper.findAll('[data-testid="report-row-item"]');
      expect(reportItems.length).toBe(1);
    });

    it("shows error when bulk delete fails", async () => {
      const wrapper = await shallowMountActivityList();
      await wrapper.vm.$nextTick();

      // Enter bulk edit mode
      const bulkDeleteButton = wrapper.find(
        '[data-testid="activity-list-bulk-delete"]',
      );
      await bulkDeleteButton.trigger("click");
      await wrapper.vm.$nextTick();

      // Select some executions
      const checkboxes = wrapper.findAll(
        '[data-testid="bulk-delete-checkbox"]',
      );
      await checkboxes[0]!.setValue(true);

      // Mock the api post to return an error response
      (api.post as jest.Mock).mockResolvedValue({
        status: 400,
        data: { message: "Failed to delete executions" },
        statusText: "Bad Request",
      });

      // Trigger the delete action
      const confirmDeleteButton = wrapper.find(
        '[data-testid="delete-selected-executions"]',
      );
      await confirmDeleteButton.trigger("click");
      await flushPromises();

      // Confirm the delete
      const modal = wrapper.findComponent({ ref: "bulkexecdeleteresult" });
      await wrapper.vm.$nextTick();
      const modalConfirmDeleteButton = modal.find(
        '[data-testid="confirm-delete"]',
      );
      await modalConfirmDeleteButton.trigger("click");

      // Wait for the API call to complete
      await flushPromises();

      // Check that the error message is displayed in the results modal
      expect(wrapper.vm.bulkEditError).toBe("Failed to delete executions");
      expect(wrapper.vm.showBulkEditResults).toBe(true);
      expect(wrapper.vm.bulkEditProgress).toBe(false);
    });

    it("handles exception during bulk delete", async () => {
      const wrapper = await shallowMountActivityList();
      await wrapper.vm.$nextTick();

      // Enter bulk edit mode
      const bulkDeleteButton = wrapper.find(
        '[data-testid="activity-list-bulk-delete"]',
      );
      await bulkDeleteButton.trigger("click");
      await wrapper.vm.$nextTick();

      // Select some executions
      const checkboxes = wrapper.findAll(
        '[data-testid="bulk-delete-checkbox"]',
      );
      await checkboxes[0]!.setValue(true);

      // Mock the api post to throw an exception
      (api.post as jest.Mock).mockRejectedValue(new Error("Network error"));

      // Trigger the delete action
      const confirmDeleteButton = wrapper.find(
        '[data-testid="delete-selected-executions"]',
      );
      await confirmDeleteButton.trigger("click");
      await flushPromises();

      // Confirm the delete
      const modal = wrapper.findComponent({ ref: "bulkexecdeleteresult" });
      await wrapper.vm.$nextTick();
      const modalConfirmDeleteButton = modal.find(
        '[data-testid="confirm-delete"]',
      );
      await modalConfirmDeleteButton.trigger("click");

      // Wait for the API call to complete
      await flushPromises();

      // Check that the error message is displayed in the results modal
      expect(wrapper.vm.bulkEditError).toBe("Network error");
      expect(wrapper.vm.showBulkEditResults).toBe(true);
      expect(wrapper.vm.bulkEditProgress).toBe(false);
    });
  });

  it("navigates to the execution page", async () => {
    const wrapper = await shallowMountActivityList();
    await wrapper.vm.$nextTick();
    const reportRowItem = wrapper.find('[data-testid="report-row-item"]');
    expect(reportRowItem.exists()).toBe(true);
    await reportRowItem.trigger("click");
    expect(window.location.href).toEqual(
      "http://localhost/project/aaa/execution/show/42",
    );
  });

  it("automatically fetches data and displays a message when there are new executions since the last timestamp", async () => {
    axiosMock.get.mockImplementation((url) => {
      if (url.includes("eventsAjax")) {
        return Promise.resolve({
          data: {
            offset: 0,
            total: 1,
            reports: [],
            lastDate: 1,
          },
        });
      }
      if (url.includes("running")) {
        return Promise.resolve({
          data: {
            executions: [],
          },
        });
      }
      if (url.includes("since.json")) {
        return Promise.resolve({
          data: {
            since: {
              count: 5,
            },
          },
        });
      }
      return Promise.resolve({ data: {} });
    });
    const wrapper = await shallowMountActivityList();
    await wrapper.vm.$nextTick();
    // Enable auto-refresh
    const autoRefreshCheckbox = wrapper.find(
      '[data-testid="auto-refresh-checkbox"]',
    );
    await autoRefreshCheckbox.setValue(true);
    await wrapper.vm.$nextTick();
    jest.advanceTimersByTime(5000);
    await flushPromises();
    const sinceCountData = wrapper.find('[data-testid="since-count-data"]');
    expect(sinceCountData.text()).toContain("5 New Results. Click to load.");
    jest.clearAllTimers();
  });
});
