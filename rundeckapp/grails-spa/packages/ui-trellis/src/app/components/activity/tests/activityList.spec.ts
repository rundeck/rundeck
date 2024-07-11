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
      },
      stubs: {
        modal: true,
        "i18n-t": true,
        btn: true,
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
          reports: mockReports,
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
              reports: mockReports,
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
      // expect(errorMessage.text()).toContain("An error occurred");
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
              reports: mockReports,
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
      axiosMock.get.mockResolvedValueOnce({
        data: {
          total: 2,
          offset: 0,
          auth: { projectAdmin: true, deleteExec: true },
          bulkEditMode: false,
          showBulkDelete: true,
          bulkSelectedIds: ["42"],
          reports: mockReports,
        },
      });

      const wrapper = await shallowMountActivityList();

      await wrapper.vm.$nextTick(); // Wait for DOM updates

      const bulkDeleteButton = wrapper.find(
        '[data-testid="activity-list-bulk-delete"]',
      );

      expect(bulkDeleteButton.exists()).toBe(true);

      await wrapper.vm.$nextTick(); // Wait for any state changes

      await bulkDeleteButton.trigger("click");

      expect(wrapper.vm.bulkEditMode).toBe(true);
    });
    it("Search", async () => {
      axiosMock.get.mockResolvedValue({
        data: {
          reports: mockReports, // Your mock data here
        },
      });
      const wrapper = await shallowMountActivityList();
      const filterButton = wrapper.find(
        '[data-testid="activity-list-filter-button"]',
      );
      await filterButton.trigger("click");
      expect(wrapper.vm.showFilters).toBe(true);
      // Simulate search action by updating the query directly
      wrapper.vm.query.jobFilter = "testJob";
      await wrapper.vm.$nextTick();
      // Trigger the reload method or any action that should follow the search input change
      await wrapper.vm.reload();
      expect(axiosMock.get).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          params: expect.objectContaining({
            jobFilter: "testJob",
          }),
        }),
      );
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
            reports: mockReports,
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
});
