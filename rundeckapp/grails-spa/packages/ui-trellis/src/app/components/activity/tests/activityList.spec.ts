import {
  rundeckServiceMock,
  rundeckClientMock,
  axiosMock,
  i18nMocks,
  mockEventBus,
  mockReports,
} from "../mocks/mock";
import { shallowMount, VueWrapper } from "@vue/test-utils";
import ActivityList from "../activityList.vue";
import ActivityFilter from "../activityFilter.vue";
import OffsetPagination from "../../../../library/components/utils/OffsetPagination.vue";
import { cloneDeep } from "lodash";
import { Modal, Btn } from "uiv";
// import { setupRundeckContext } from "../mocks/setupRundeckContext";
jest.mock("../../../../library/rundeckService", () => rundeckServiceMock);
jest.mock("@rundeck/client", () => rundeckClientMock);
jest.mock("axios", () => axiosMock);
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

        "i18n-t": true,
        btn: false,
        ProgressBar: true,
      },
      mocks: {
        ...i18nMocks,
        ...options.mocks, // Use the generic options object
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
    axiosMock.get.mockImplementation((url) => {
      if (url.includes("running")) {
        return {
          data: {
            executions: [...mockRunningExecutions],
          },
        };
      }
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
    afterEach(() => {
      jest.clearAllMocks();
    });

    it("information about the page being shown and total number of results", async () => {
      axiosMock.get.mockResolvedValue({
        data: {
          total: 20,
          offset: 0,
          reports: reports,
        },
      });
      const wrapper = await shallowMountActivityList();
      const pageInfoSpan = wrapper.find('[data-testid="page-info"]');
      const summaryCount = wrapper.find('[data-testid="summary-count"]');
      expect(pageInfoSpan.text()).toBe("1 - 2 of");
      expect(summaryCount.text()).toBe("20 executions");
    });

    it("information about the current executions and finished jobs", async () => {
      axiosMock.get.mockImplementation((url) => {
        if (url.includes("execution")) {
          return Promise.resolve({
            data: {
              executions: mockRunningExecutions,
            },
          });
        }
        if (url.includes("eventsAjax")) {
          return Promise.resolve({
            data: {
              total: 20,
              offset: 0,
              reports: reports,
            },
          });
        }
        return Promise.resolve({ data: {} });
      });
      const wrapper = await shallowMountActivityList();
      await wrapper.vm.$nextTick();
      const executionItems = wrapper.findAll('[data-testid="execution-link"]');
      const reportItems = wrapper.findAll('[data-testid="report-row-item"]');
      expect(executionItems.length).toBe(mockRunningExecutions.length);
      expect(reportItems.length).toBe(mockReports.length);
    });

    it("error message when loading(apis) fails", async () => {
      axiosMock.get.mockRejectedValue(new Error("An error occurred"));
      const wrapper = await shallowMountActivityList();
      await wrapper.vm.$nextTick();
      const errorMessage = wrapper.find('[data-testid="error-message"]');

      expect(errorMessage.exists()).toBe(true);

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
      await wrapper.vm.$nextTick(); // Wait for all pending updates to finish
      const noInfoMessage = wrapper.find('[data-testid="no-data-message"]');
      expect(noInfoMessage.exists()).toBe(true);
      expect(noInfoMessage.text()).toBe("No results for the query");
    });

    it("renders message when there are no executions since timestamp", async () => {
      axiosMock.get.mockImplementation((url) => {
        if (url.includes("eventsAjax")) {
          return Promise.resolve({
            data: {
              reports: reports,
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
      // Simulate the component's periodic check for new executions
      wrapper.vm.lastDate = 1; // Set a valid lastDate so loadSince will execute
      await wrapper.vm.loadSince();
      await wrapper.vm.$nextTick(); // Wait for all pending updates to finish
      const sinceCountData = wrapper.find('[data-testid="since-count-data"]');
      expect(sinceCountData.exists()).toBe(true);
      expect(sinceCountData.text()).toContain("5 New Results. Click to load.");
    });
  });

  describe("trigger bulk actions", () => {
    afterEach(() => {
      jest.clearAllMocks();
    });
    it("trigger bulk actions - delete", async () => {
      axiosMock.get.mockImplementation((url) => {
        if (url.includes("eventsAjax")) {
          return Promise.resolve({
            data: {
              total: reports.length,
              offset: 0,
              auth: { projectAdmin: true, deleteExec: true },
              bulkEditMode: false,
              showBulkDelete: true,
              bulkSelectedIds: ["42"],
              reports: reports,
            },
          });
        }
        if (url.includes("running")) {
          return Promise.resolve({
            data: {
              executions: mockRunningExecutions,
            },
          });
        }
        return Promise.resolve({ data: {} });
      });
      axiosMock.post.mockResolvedValueOnce({
        data: { allsuccessful: true },
      });
      const wrapper = await shallowMountActivityList();
      await wrapper.vm.$nextTick();
      const bulkDeleteButton = wrapper.find(
        '[data-testid="activity-list-bulk-delete"]',
      );
      await bulkDeleteButton.trigger("click");

      const checkboxes = wrapper.findAll(
        '[data-testid="bulk-delete-checkbox"]',
      );
      await checkboxes.at(0)?.setValue(true);
      await checkboxes.at(1)?.setValue(true);
      axiosMock.get.mockImplementationOnce((url) => {
        if (url.includes("eventsAjax")) {
          // Assuming 2 reports were deleted
          return Promise.resolve({
            data: {
              total: reports.length - 2,
              offset: 0,
              auth: { projectAdmin: true, deleteExec: true },
              bulkEditMode: false,
              showBulkDelete: true,
              reports: reports.slice(2),
            },
          });
        }
        return Promise.resolve({ data: {} });
      });
      const confirmDeleteButton = wrapper.find(
        '[data-testid="delete-selected-executions"]',
      );
      await confirmDeleteButton.trigger("click");
      await wrapper.vm.$nextTick();
      const modal = wrapper.findComponent({ ref: "bulkexecdeleteresult" });
      expect(modal.exists()).toBe(true);
      const modalConfirmDeleteButton = modal.find(
        '[data-testid="confirm-delete"]',
      );
      expect(modalConfirmDeleteButton.exists()).toBe(true);
      await modalConfirmDeleteButton.trigger("click");
      const executionBulkDeleteSpy = jest
        .spyOn(wrapper.vm.rundeckContext.rundeckClient, "executionBulkDelete")
        .mockImplementation(() => Promise.resolve({ allsuccessful: true }));
      await wrapper.vm.$nextTick();
      const reportRows = wrapper.findAll('[data-testid="report-row-item"]');
      expect(reportRows.length).toBe(0); // Update expected length based on deletion
      expect(executionBulkDeleteSpy).toHaveBeenCalledWith({ ids: [42] });
    });

    it("trigger bulk actions - search", async () => {
      // Mock initial API call to return the whole array of reports
      axiosMock.get.mockImplementation((url) => {
        if (url.includes("eventsAjax")) {
          return Promise.resolve({
            data: {
              total: reports.length,
              offset: 0,
              reports: reports,
            },
          });
        }
        if (url.includes("running")) {
          return Promise.resolve({
            data: {
              executions: mockRunningExecutions,
            },
          });
        }
        return Promise.resolve({ data: {} });
      });
      const wrapper = await shallowMountActivityList();
      // Simulate clicking the filter button to show filters
      const filterButton = wrapper.find(
        '[data-testid="activity-list-filter-button"]',
      );
      await filterButton.trigger("click");
      expect(wrapper.vm.showFilters).toBe(true);
      // Simulate entering a search term
      const activityFilter = wrapper.findComponent(ActivityFilter);
      activityFilter.vm.$emit("input", {
        ...wrapper.vm.query,
        jobIdFilter: "testJob",
      });

      await wrapper.vm.$nextTick();
      // Mock API call to return filtered reports
      axiosMock.get.mockImplementationOnce((url) => {
        if (url.includes("eventsAjax")) {
          return Promise.resolve({
            data: {
              total: 2, // Assuming the search results in 2 reports
              offset: 0,
              reports: reports.slice(0, 2), // Return only 2 matching reports
            },
          });
        }
        if (url.includes("running")) {
          return Promise.resolve({
            data: {
              executions: mockRunningExecutions,
            },
          });
        }
        return Promise.resolve({ data: {} });
      });

      expect(axiosMock.get).toHaveBeenCalledTimes(2); // Initial call + search call
      // Verify the results
      const reportItems = wrapper.findAll('[data-testid="report-row-item"]');
      expect(reportItems.length).toBe(2); // Ensure the filtered result is shown
    });
  });

  it("navigates to the execution page", async () => {
    axiosMock.get.mockImplementation((url, data) => {
      if (url.includes("execution")) {
        return Promise.resolve({
          data: {
            executions: mockRunningExecutions,
          },
        });
      }
      if (url.includes("eventsAjax")) {
        return Promise.resolve({
          data: {
            total: 20,
            offset: 0,
            reports: reports,
          },
        });
      }
      return Promise.resolve({ data: {} });
    });
    const wrapper = await shallowMountActivityList();
    await wrapper.vm.$nextTick();
    const reportRowItem = wrapper.find('[data-testid="report-row-item"]');
    expect(reportRowItem.exists()).toBe(true);
    // Simulate click on the report row item
    await reportRowItem.trigger("click");
    // Check if the navigation occurred correctly
    const report = mockReports[0]; // Use the first report for simplicity
    expect(window.location).toBe(report.executionHref);
  });

  it("fetches data automatically when auto-refresh is true", async () => {
    // Use fake timers
    jest.useFakeTimers();
    // Mock axios.get for different API calls
    axiosMock.get.mockImplementation((url) => {
      if (url.includes("eventsAjax")) {
        return Promise.resolve({
          data: {
            total: reports.length,
            reports: reports,
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
              count: 0,
            },
          },
        });
      }
      return Promise.resolve({ data: {} });
    });
    const wrapper = await shallowMountActivityList();
    // Enable auto-refresh
    const autoRefreshCheckbox = wrapper.find(
      '[data-testid="auto-refresh-checkbox"]',
    );
    await autoRefreshCheckbox.setValue(true);
    await wrapper.vm.$nextTick();
    expect(axiosMock.get).toHaveBeenCalledTimes(2); // Initial call

    console.log(
      "Before advancing timers, number of calls:",
      axiosMock.get.mock.calls.length,
    );

    // Advance timers to trigger auto-refresh
    jest.advanceTimersByTime(5000);
    await wrapper.vm.$nextTick();

    console.log(
      "After advancing timers, number of calls:",
      axiosMock.get.mock.calls.length,
    );

    // Verify the results
    expect(axiosMock.get).toHaveBeenCalledTimes(4);
    // Clean up timers
    jest.clearAllTimers();
  });
});
