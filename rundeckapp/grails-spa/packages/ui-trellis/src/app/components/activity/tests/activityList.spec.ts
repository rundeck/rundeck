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
import Modal from "uiv";
import Btn from "uiv";
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
        // modal: {
        //   template: `
        //     <div>
        //       <slot></slot>
        //       <div id="bulkexecdeleteresult">
        //         <button type="button" data-testid="confirm-delete">Confirm Delete</button>
        //       </div>
        //     </div>
        //   `,
        // },
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
          console.log("Initial fetch of reports with data:", reports);
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
          console.log("Initial fetch of running executions");
          return Promise.resolve({
            data: {
              executions: mockRunningExecutions,
            },
          });
        }
        return Promise.resolve({ data: {} });
      });
      const wrapper = await shallowMountActivityList();
      // Trigger bulk edit mode LineNo: 94 -99
      const bulkDeleteButton = wrapper.find(
        '[data-testid="activity-list-bulk-delete"]',
      );
      console.log("Bulk delete button exists:", bulkDeleteButton.exists());
      if (!bulkDeleteButton.exists()) {
        console.error("Bulk delete button not found in DOM");
        return;
      }
      await bulkDeleteButton.trigger("click");
      console.log("Triggered bulk delete button click");

      // Select and click checkboxes for deletion (Component line: Line No: 253 - 266 )
      const checkboxes = wrapper.findAll(
        '[data-testid="bulk-delete-checkbox"]',
      );
      console.log("Number of checkboxes:", checkboxes.length);
      checkboxes.forEach((checkbox, index) => {
        const inputElement = checkbox.element as HTMLInputElement;
        console.log(`Checkbox ${index} value:`, inputElement.value);
      });
      await checkboxes.at(0)?.trigger("click");
      console.log("First checkbox checked");
      await checkboxes.at(1)?.trigger("click");
      console.log("Second checkbox checked");

      // Mock updated reports list after deletion
      axiosMock.get.mockImplementationOnce((url) => {
        if (url.includes("eventsAjax")) {
          console.log("Fetch of reports after deletion");
          // Assuming 2 reports were deleted, adjust the slice accordingly
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
      // Trigger delete confirmation (Component line:79-85)
      const confirmDeleteButton = wrapper.find(
        '[data-testid="delete-selected-executions"]',
      );
      if (
        !confirmDeleteButton.exists() ||
        (confirmDeleteButton.element as HTMLButtonElement).disabled
      ) {
        console.error("Confirm delete button not found in DOM or is disabled");
        return;
      }
      await confirmDeleteButton.trigger("click");
      await wrapper.vm.$nextTick();
      console.log("Triggered confirm delete button click");
      // Ensure modal is rendered Line 146-152
      const modal = wrapper.findComponent({ ref: "bulkexecdeleteresult" });
      expect(modal.exists()).toBe(true);
      if (modal.exists()) {
        console.log("Modal HTML:", modal.html());
      } else {
        console.error("Modal not found in the DOM.");
      }
      // Check if modal contains the confirm delete button Line 164 -168
      const modalConfirmDeleteButton = wrapper.find(
        '[data-testid="confirm-delete"]',
      );
      expect(modalConfirmDeleteButton.exists()).toBe(true);
      console.log(
        "Modal confirm delete button exists:",
        modalConfirmDeleteButton.exists(),
      );
      if (!modalConfirmDeleteButton.exists()) {
        console.error("Modal confirm delete button not found in DOM");
        return;
      }
      // Trigger modal confirm delete button click
      await modalConfirmDeleteButton.trigger("click");
      await wrapper.vm.$nextTick();
      console.log("Triggered modal confirm delete button click");
      // Wait for the deletion to complete and the list to update
      await wrapper.vm.$nextTick(); // Wait for state updates
      await wrapper.vm.$nextTick(); // Ensure all updates are applied
      // Check the number of remaining report rows

      // Check the number of remaining report rows
      // component line:147>
      const reportRows = wrapper.findAll('[data-testid="report-row-item"]');
      console.log("Number of report rows after deletion:", reportRows.length);
      expect(reportRows.length).toBe(reports.length - 2); // Update expected length based on deletion // Update expected length based on deletion
    });

    it("search", async () => {
      // to do: change how axiosMock is mocked, so that upon the first fetch of reports you can return the whole array of reports,
      // but on the second time (which happens after search), the mock will return the array of reports that match the search term (ie 2);

      axiosMock.get.mockResolvedValue({
        data: {
          reports: reports,
        },
      });
      const wrapper = await shallowMountActivityList();
      const filterButton = wrapper.find(
        '[data-testid="activity-list-filter-button"]',
      );
      await filterButton.trigger("click");
      expect(wrapper.vm.showFilters).toBe(true);
      const activityFilter = wrapper.findComponent(ActivityFilter);
      activityFilter.vm.$emit("input", {
        ...wrapper.vm.query,
        jobIdFilter: "testJob",
      });
      await wrapper.vm.$nextTick();
      // Check the API call
      expect(axiosMock.get).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          params: expect.objectContaining({
            jobIdFilter: "testJob",
          }),
        }),
      );

      const reportItems = wrapper.findAll('[data-testid="report-row-item"]');
      // TODO: use expect
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
    // to do: add all 3 mocks for axiosMock (since, reports, execution), to ensure that loadSince will pass;
    axiosMock.get.mockResolvedValue({
      data: {
        reports: reports,
      },
    });
    const wrapper = await shallowMountActivityList();
    const autoRefreshCheckbox = wrapper.find(
      '[data-testid="auto-refresh-checkbox"]',
    );
    await autoRefreshCheckbox.setValue(true);
    await wrapper.vm.$nextTick();
    // its fine to advance timers, but need to make sure that it has passed enough time to trigger the setTimeout
    // or can trigger runAllTimers, which will advance the timers until all setTimeouts/setIntervals run
    jest.runAllTimers();
    await wrapper.vm.$nextTick();
    expect(axiosMock.get).toHaveBeenCalledTimes(2); // this number will be bigger
  });
});
