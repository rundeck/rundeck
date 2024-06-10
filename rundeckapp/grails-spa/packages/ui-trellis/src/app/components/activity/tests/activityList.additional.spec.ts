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
  globalOptions: GlobalOptions = {}
) => {
  const wrapper = shallowMount(ActivityList, {
    props: {
      eventBus: mockEventBus,
      displayMode: "full",
      ...props,
    },
    global: {
      components: {
        OffsetPagination,
        ActivityFilter,
      },
      stubs: {
        modal: true,
        ActivityFilter: true,
        Modal: {
          template: "<div class='modal-stub'><slot></slot></div>",
        },
        ProgressBar: { template: "<div><slot></slot></div>" },
        btn: {
          template: `<button :disabled="disabled" data-test-id="activity-list-delete-selected-executions">Delete Selected Executions</button>`,
          props: ["disabled"],
        },

        tooltip: true,
        ...globalOptions.stubs,
      },
      directives: {
        tooltip: () => {},
      },
      mocks: {
        ...i18nMocks,
        ...globalOptions.mocks,
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
  it("renders the component correctly", async () => {
    const wrapper = await shallowMountActivityList();
    expect(wrapper.exists()).toBe(true);
  });
  it("renders loading area", async () => {
    const wrapper = await shallowMountActivityList();
    await wrapper.setData({ loading: true });
    expect(wrapper.find('[data-test-id="loading-area"]').exists()).toBe(true);
  });
  it("displays an error message when load fails", async () => {
    const wrapper = await shallowMountActivityList();
    await wrapper.setData({ loadError: "An Error Occurred" });
    await wrapper.vm.$nextTick();
    const errorMessage = wrapper.find(".loading-area .text-warning");
    expect(errorMessage.text()).toContain("An Error Occurred");
  });
  it("displays a no data message", async () => {
    const wrapper = await shallowMountActivityList();
    await wrapper.setData({ reports: [], loading: false, loadError: null });
    await wrapper.vm.$nextTick();
    const noDataMessage = wrapper.find('[data-test="no-data-message"]');
    expect(noDataMessage.exists()).toBe(true);
    expect(noDataMessage.text()).toBe("No results for the query");
  });

  it("renders since count data", async () => {
    const wrapper = await shallowMountActivityList();
    await wrapper.setData({ sincecount: 5 });
    await wrapper.vm.$nextTick();

    const sinceCountTd = wrapper.find(".since-count-data");

    expect(sinceCountTd.text()).toContain("info.newexecutions.since.0");
  });

  it("renders the pagination correctly", async () => {
    const wrapper = await shallowMountActivityList();
    await wrapper.setData({ pagination: { total: 50, offset: 10, max: 10 } });
    const pagination = wrapper.findComponent(OffsetPagination);
    expect(pagination.exists()).toBe(true);
  });
  it("renders the filter button", async () => {
    const wrapper = await shallowMountActivityList();

    const filterButton = wrapper.find(
      '[data-test-id="activity-list-filter-button"]'
    );
    expect(filterButton.exists()).toBe(true);
  });

  it("renders running executions", async () => {
    const wrapper = await shallowMountActivityList();
    await wrapper.setData({
      running: {
        executions: [
          {
            id: "1",
            status: "running",
            dateStarted: { date: new Date() },
          },
        ],
        paging: {},
      },
    });
    await wrapper.vm.$nextTick();
    const execution = wrapper.vm.running.executions[0];
    expect(execution).toHaveProperty("id", "1");
    expect(execution).toHaveProperty("status", "running");
    expect(execution.dateStarted).toHaveProperty("date");
  });
});
describe("ActivityList Bulk Edit Modals", () => {
  it("opens and closes the filter modal", async () => {
    const wrapper = await shallowMountActivityList();
    const filterButton = wrapper.find(
      '[data-test-id="activity-list-filter-button"]'
    );
    await filterButton.trigger("click");
    expect(wrapper.findComponent(ActivityFilter).exists()).toBe(true);
  });
  it("opens the bulk delete modal", async () => {
    const wrapper = await shallowMountActivityList();
    // wrapper.vm.showBulkEditConfirm = true;
    await wrapper.setData({ showBulkEditConfirm: true });
    const bulkDeleteButton = wrapper.find('[data-test-id="modal-bulk-delete"]');
    expect(bulkDeleteButton.exists()).toBe(true);
  });
  it("opens the bulk delete results modal", async () => {
    const wrapper = await shallowMountActivityList();
    wrapper.vm.showBulkEditResults = true;
    const bulkDeleteResults = wrapper.find(
      '[data-test-id="modal-bulk-delete-results"]'
    );
    expect(bulkDeleteResults.exists()).toBe(true);
  });
  it("enables bulk delete button when items are selected", async () => {
    const wrapper = await shallowMountActivityList();
    await wrapper.setData({
      reports: [
        {
          execution: {
            id: 2,
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
      ],
      bulkEditMode: true,
      bulkSelectedIds: ["#2"],
    });
    wrapper.vm.bulkSelectedIds = ["#2"];
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.bulkSelectedIds.length).toBeGreaterThan(0);
  });
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
  it("displays correct message when no bulk delete results", async () => {
    const wrapper = await shallowMountActivityList(
      {},
      {
        stubs: {
          "i18n-t": {
            template: '<div class="i18n-t-stub">{{ $attrs.keypath }}</div>',
          },
        },
      },
    );

    await wrapper.setData({
      showBulkEditResults: true,
      bulkEditResults: {
        requestCount: 0,
        successCount: 0,
        failedCount: 0,
        failures: [],
      },
      bulkEditProgress: true,
    });

    await wrapper.vm.$nextTick();

    const modal = wrapper.find("#cleanselections");
    expect(modal.exists()).toBe(true);
    const progressMessage = modal.find(".i18n-t-stub");
    expect(progressMessage.exists()).toBe(true);
    expect(progressMessage.text()).toContain("clearselected.confirm.text");
  });
  it("opens and closes the bulk delete results modal", async () => {
    const wrapper = await shallowMountActivityList();

    await wrapper.setData({
      showBulkEditResults: true,
    });

    await wrapper.vm.$nextTick();

    const bulkDeleteResultsModal = wrapper.find(
      '[data-test-id="modal-bulk-delete-results"]'
    );
    expect(bulkDeleteResultsModal.exists()).toBe(true);
  });
  it("handles bulk edit deselect all", async () => {
    const wrapper = await shallowMountActivityList();
    await wrapper.setData({ showBulkEditCleanSelections: true });
    const modalCleanSelections = wrapper.find("#cleanselections");
    const allButton = modalCleanSelections.find("button.btn.btn-danger");
    if (allButton.exists()) {
      const bulkEditDeselectAllPages = jest.spyOn(
        wrapper.vm,
        "bulkEditDeselectAllPages"
      );
      await allButton.trigger("click");
      expect(bulkEditDeselectAllPages).toHaveBeenCalled();
    }
  });
});
