import {
  rundeckServiceMock,
  rundeckClientMock,
  axiosMock,
  i18nMocks,
  mockEventBus,
  mockReports,
} from "../mocks/mock";
import { flushPromises, shallowMount, VueWrapper } from "@vue/test-utils";
import ActivityList from "../activityList.vue";
import ActivityFilter from "../activityFilter.vue";
import OffsetPagination from "../../../../library/components/utils/OffsetPagination.vue";
import { cloneDeep } from "lodash";
import { Modal, Btn } from "uiv";
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
        // modal: false,
        btn: false,
        "i18n-t": true,
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
    it("information about the page being shown and total number of results", async () => {
      const wrapper = await shallowMountActivityList();
      const pageInfoSpan = wrapper.find('[data-testid="page-info"]');
      const summaryCount = wrapper.find('[data-testid="summary-count"]');
      expect(pageInfoSpan.text()).toBe("1 - 2 of");
      expect(summaryCount.text()).toBe("20 executions");
    });

    it("information about the current executions and finished jobs", async () => {
      axiosMock.get.mockImplementation((url) => {
        if (url.includes("running")) {
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
      await wrapper.vm.$nextTick(); // Wait for all pending updates to finish
      // Verify the current executions
      const executionItems = wrapper.findAll('[data-testid="execution-link"]');
      expect(executionItems.length).toBe(2); // Directly using the expected length value
      // Verify the finished job reports
      const reportItems = wrapper.findAll('[data-testid="report-row-item"]');
      expect(reportItems.length).toBe(2);
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
      const confirmDeleteButton = wrapper.find(
        '[data-testid="delete-selected-executions"]',
      );

      await confirmDeleteButton.trigger("click");
      await flushPromises();
      const modal = wrapper.find('[data-testid="modal-bulk-delete"]');
      expect(modal.exists()).toBe(true);
      console.log("Modal HTML output:", modal.html());
      await flushPromises();
      await wrapper.vm.$nextTick();
      // console.log(wrapper.html());
      // const modalConfirmDeleteButton = modal.find(".confirm-delete");
      const modalConfirmDeleteButton = modal.find(
        '[data-testid="confirm-delete"]',
      );
      console.log(
        "Confirm Delete Button Exists:",
        modalConfirmDeleteButton.exists(),
      );

      expect(modalConfirmDeleteButton.exists()).toBe(true);

      await modalConfirmDeleteButton.trigger("click");
      const executionBulkDeleteSpy = jest
        .spyOn(wrapper.vm.rundeckContext.rundeckClient, "executionBulkDelete")
        .mockImplementation(() => Promise.resolve({ allsuccessful: true }));
      await wrapper.vm.$nextTick();
      expect(executionBulkDeleteSpy).toHaveBeenCalledWith({ ids: [42] });
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
  });

  it("navigates to the execution page", async () => {
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
    const reportRowItem = wrapper.find('[data-testid="report-row-item"]');
    expect(reportRowItem.exists()).toBe(true);
    await reportRowItem.trigger("click");
    expect(window.location).toBe("/project/aaa/execution/show/42");
  });

  it("fetches data automatically and renders message when there are  executions since timestamp", async () => {
    jest.useFakeTimers();
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
    jest.useRealTimers();
  });
});
