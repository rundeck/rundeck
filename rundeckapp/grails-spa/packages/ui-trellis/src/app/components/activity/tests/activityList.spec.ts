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

describe("ActivityList", () => {
  beforeAll(() => {
    jest.useFakeTimers();
  });
  afterAll(() => {
    jest.useRealTimers();
  });
  afterEach(() => {
    jest.clearAllMocks();
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
      axiosMock.get
        .mockResolvedValueOnce({
          data: {
            reports: mockReports,
          },
        })
        .mockResolvedValueOnce({
          data: {
            executions: mockRunningExecutions,
          },
        });
      const wrapper = await shallowMountActivityList();
      await wrapper.vm.$nextTick();
      await wrapper.vm.$nextTick(); // Ensure the DOM is updated after data fetch
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
      axiosMock.get.mockResolvedValue({
        data: {
          reports: [],
          running: {
            executions: [],
          },
        },
      });
      const wrapper = await shallowMountActivityList();

      wrapper.setData({ loading: false, loadError: null });
      await wrapper.vm.$nextTick();
      const noInfoMessage = wrapper.find('[data-testid="no-data-message"]');
      expect(noInfoMessage.exists()).toBe(true);
      expect(noInfoMessage.text()).toBe("No results for the query");
    });
    it("renders message when there are no executions since timestamp", async () => {
      axiosMock.get
        .mockResolvedValueOnce({
          data: {
            reports: mockReports,
          },
        })
        .mockResolvedValueOnce({
          data: {
            executions: mockRunningExecutions,
          },
        });

      const wrapper = await shallowMountActivityList();
      await wrapper.setData({ sincecount: 5 });
      await wrapper.vm.$nextTick(); // Ensure the DOM is updated after data fetch
      const sinceCountData = wrapper.find('[data-testid="since-count-data"]');
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
          reports: mockReports,
        },
      });

      const wrapper = await shallowMountActivityList();
      await wrapper.setData({
        auth: { projectAdmin: true, deleteExec: true },
        pagination: { offset: 0, max: 10, total: 2 },
        bulkEditMode: false,
        showBulkDelete: false,
        bulkSelectedIds: ["0"],
      });
      await wrapper.vm.$nextTick(); // Wait for DOM updates
      console.log("auth.deleteExec: ", wrapper.vm.auth.deleteExec);
      console.log("bulkEditMode: ", wrapper.vm.bulkEditMode);
      console.log("showBulkDelete: ", wrapper.vm.showBulkDelete);
      console.log("bulkSelectedIds: ", wrapper.vm.bulkSelectedIds);
      console.log("Rendered HTML: ", wrapper.html());

      const bulkDeleteButton = wrapper.find(
        '[data-testid="activity-list-bulk-delete"]',
      );
      console.log("bulkDeleteButton: ", bulkDeleteButton.exists());
      expect(bulkDeleteButton.exists()).toBe(true);

      await wrapper.vm.$nextTick(); // Wait for any state changes

      await bulkDeleteButton.trigger("click");
      console.log("bulkDeleteButton clicked:, ", wrapper.vm.bulkEditMode);
      expect(wrapper.vm.bulkEditMode).toBe(true);
    });
    it("search", async () => {
      axiosMock.get.mockResolvedValue({
        data: {
          reports: mockReports,
        },
      });
      const wrapper = await shallowMountActivityList();
      const filterButton = wrapper.find(
        '[data-testid="activity-list-filter-button"]',
      );
      await filterButton.trigger("click");
      expect(wrapper.vm.showFilters).toBe(true);
      // Simulate search action
      await wrapper.setData({ query: { jobFilter: "testJob" } });
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
    axiosMock.get.mockResolvedValueOnce({
      data: {
        executions: mockRunningExecutions,
      },
    });
    const wrapper = await shallowMountActivityList();
    await wrapper.vm.$nextTick();
    // Mock window.location
    delete window.location;
    window.location = { href: "" } as any;
    // Set the reports data explicitly
    await wrapper.setData({
      reports: [
        {
          execution: { id: "1" },
          permalink: "http://localhost:4440/project/test/execution/show/1",
          status: "succeed",
          dateCompleted: "2024-05-22T14:33:52Z",
          node: { total: 1, succeeded: 1, failed: 0 },
        },
      ],
    });
    await wrapper.vm.$nextTick();
    // Find the report row item and simulate click
    const reportRowItem = wrapper.find('[data-testid="report-row-item"]');
    await reportRowItem.trigger("click");

    expect(window.location).toBe(
      "http://localhost:4440/project/test/execution/show/1",
    );
  });

  it("fetches data automatically when auto-refresh is true", async () => {
    axiosMock.get.mockResolvedValue({
      data: {
        reports: mockReports,
      },
    });
    const wrapper = await shallowMountActivityList();
    await wrapper.setData({ autorefresh: true });
    jest.advanceTimersByTime(5000); // Assuming autorefreshms is 5000
    expect(axiosMock.get).toHaveBeenCalledTimes(2); // Initial load + 1 auto-refresh
  });
});
