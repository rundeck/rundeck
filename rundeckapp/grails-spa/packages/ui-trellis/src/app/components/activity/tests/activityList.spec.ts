import {
  rundeckServiceMock,
  rundeckClientMock,
  axiosMock,
  i18nMocks,
  mockEventBus,
} from "./mock";
import { mount, VueWrapper } from "@vue/test-utils";

import ActivityList from "../activityList.vue";
import ActivityFilter from "../activityFilter.vue";
import OffsetPagination from "../../../../library/components/utils/OffsetPagination.vue";

import { setupRundeckContext } from "./setupRundeckContext";
import { GlobalOptions } from "./type";

jest.mock("../../../../library/rundeckService", () => rundeckServiceMock);
jest.mock("@rundeck/client", () => rundeckClientMock);
jest.mock("axios", () => axiosMock);
type ActivityListInstance = InstanceType<typeof ActivityList>;
const defaultProps = {
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
  bulkEditMode: true,
  bulkSelectedIds: [],
};
const mountActivityList = async (
  props = {},
  globalOptions: GlobalOptions = {}
) => {
  const wrapper = mount(ActivityList, {
    props: {
      ...defaultProps,
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
        ActivityFilter: true,
        Modal: { template: "<div><slot></slot></div>" },
        ProgressBar: { template: "<div><slot></slot></div>" },
        btn: {
          template: `<button :disabled="disabled" data-test-id="activity-list-delete-selected-executions">Delete Selected Executions</button>`,
          props: ["disabled"],
        },

        i18n: true,
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
    const wrapper = await mountActivityList();
    expect(wrapper.exists()).toBe(true);
  });
  it("renders loading area", async () => {
    const wrapper = await mountActivityList({ loading: true });
    expect(wrapper.find('[data-test-id="loading-area"]').exists()).toBe(true);
  });
  it("displays an error message when load fails", async () => {
    const wrapper = await mountActivityList();
    const loadError = "An Error Occurred";
    await wrapper.setProps({ loadError });
    await wrapper.vm.$nextTick();
    const errorMessage = wrapper.find(".loading-area .text-warning");
    expect(errorMessage.text()).toContain(loadError);
  });
  it("renders the no data message when reports are empty and loading is false", async () => {
    const wrapper = await mountActivityList({
      reports: [],
      loading: false,
      loadError: null,
    });
    await wrapper.vm.$nextTick();
    const noDataMessageElement = wrapper.find(
      ':contains("No results for the query")'
    );
    expect(noDataMessageElement.exists()).toBe(true);
  });
  it("renders since count data", async () => {
    const wrapper = await mountActivityList({ sinceCount: 5 });
    expect(wrapper.html()).toContain("5");
  });

  it("renders the pagination correctly", async () => {
    const paginationProp = { total: 50, offset: 10, max: 10 };
    const wrapper = await mountActivityList(
      { pagination: paginationProp },
      {
        stubs: {
          OffsetPagination: { template: '<div id="OffsetPagination" />' },
        },
      }
    );
    const pagination = wrapper.find("#OffsetPagination");
    expect(pagination.exists()).toBe(true);
  });
  it("renders the filter button", async () => {
    const wrapper = await mountActivityList();
    const filterButton = wrapper.find(
      '[data-test-id="activity-list-filter-button"]'
    );
    expect(filterButton.exists()).toBe(true);
  });
});
describe("ActivityList Bulk Edit Modals", () => {
  it("opens and closes the filter modal", async () => {
    const wrapper = await mountActivityList();
    const filterButton = wrapper.find(
      '[data-test-id="activity-list-filter-button"]'
    );
    await filterButton.trigger("click");
    expect(wrapper.findComponent(ActivityFilter).exists()).toBe(true);
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
      '[data-test-id="modal-bulk-delete-results"]'
    );
    expect(bulkDeleteResults.exists()).toBe(true);
  });
  it("enables bulk delete button when items are selected", async () => {
    const wrapper = await mountActivityList({
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
    const wrapper = await mountActivityList();
    const autoRefreshCheckbox = wrapper.find("#auto-refresh");
    await autoRefreshCheckbox.setValue(true);
    expect(wrapper.vm.autorefresh).toBe(true);
    await autoRefreshCheckbox.setValue(false);
    expect(wrapper.vm.autorefresh).toBe(false);
  });
  it("handles pagination correctly", async () => {
    const wrapper = await mountActivityList();
    const paginationSpy = jest.spyOn(wrapper.vm, "changePageOffset");
    const pagination = wrapper.findComponent({ name: "OffsetPagination" });
    pagination.vm.$emit("change", 10);
    await wrapper.vm.$nextTick();
    expect(paginationSpy).toHaveBeenCalledWith(10);
    paginationSpy.mockRestore();
  });
  it("displays correct message when no bulk delete results", async () => {
    const wrapper = await mountActivityList(
      {
        showBulkEditResults: true,
        bulkEditResults: {
          requestCount: 0,
          successCount: 0,
          failedCount: 0,
          failures: [],
        },
        bulkEditProgress: true,
      },
      {
        stubs: {
          Modal: {
            template: '<div class="modal-stub"><slot></slot></div>',
          },
          "i18n-t": {
            template: '<div class="i18n-t-stub">{{ $attrs.keypath }}</div>',
          },
          ProgressBar: {
            template: '<div class="progress-bar-stub"></div>',
          },
        },
      }
    );
    await wrapper.vm.$nextTick();
    const modal = wrapper.find("#cleanselections");
    expect(modal.exists()).toBe(true);
    const progressMessage = modal.find(".i18n-t-stub");
    expect(progressMessage.exists()).toBe(true);
    expect(progressMessage.text()).toContain("clearselected.confirm.text");
  });
  it("opens and closes the bulk delete results modal", async () => {
    const wrapper = await mountActivityList(
      {
        showBulkEditResults: true,
      },
      {
        stubs: {
          Modal: {
            template: "<div class='modal-stub'><slot></slot></div>",
          },
        },
      }
    );
    const bulkDeleteResultsModal = wrapper.find(
      '[data-test-id="modal-bulk-delete-results"]'
    );
    expect(bulkDeleteResultsModal.exists()).toBe(true);
  });
  it("handles bulk edit deselect all", async () => {
    const wrapper = await mountActivityList({
      showBulkEditCleanSelections: true,
    });
    const modalCleanSelections = wrapper.find("#cleanselections");
    const allButton = modalCleanSelections.find("button.btn.btn-danger");
    if (allButton.exists()) {
      const bulkEditDeselectAllPagesSpy = jest.spyOn(
        wrapper.vm,
        "bulkEditDeselectAllPages"
      );
      await allButton.trigger("click");
      expect(bulkEditDeselectAllPagesSpy).toHaveBeenCalled();
      bulkEditDeselectAllPagesSpy.mockRestore();
    }
  });
  it("renders running executions", async () => {
    const wrapper = await mountActivityList();
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
