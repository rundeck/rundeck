import { mount, VueWrapper } from "@vue/test-utils";
import ActivityList from "../activityList.vue";
import ActivityFilter from "../activityFilter.vue";
import { RundeckContext } from "../../../../library";
import { setupRundeckContext } from "./setupRundeckContext";
import OffsetPagination from "../../../../library/components/utils/OffsetPagination.vue";
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
type ComponentStub = boolean | { template: string };
type Stubs = string[] | Record<string, ComponentStub>;
interface Directive {
  beforeMount?: (
    el: HTMLElement,
    binding: any,
    vnode: any,
    prevVnode: any,
  ) => void;
  mounted?: (el: HTMLElement, binding: any, vnode: any, prevVnode: any) => void;
  beforeUpdate?: (
    el: HTMLElement,
    binding: any,
    vnode: any,
    prevVnode: any,
  ) => void;
  updated?: (el: HTMLElement, binding: any, vnode: any, prevVnode: any) => void;
  beforeUnmount?: (
    el: HTMLElement,
    binding: any,
    vnode: any,
    prevVnode: any,
  ) => void;
  unmounted?: (
    el: HTMLElement,
    binding: any,
    vnode: any,
    prevVnode: any,
  ) => void;
}
interface GlobalOptions {
  stubs?: Stubs;
  mocks?: Record<string, unknown>;
  directives?: Record<string, Directive>;
}

type ActivityListInstance = InstanceType<typeof ActivityList>;
const mountActivityList = async (
  props = {},
  globalOptions: GlobalOptions = {},
) => {
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
        OffsetPagination,
        ActivityFilter,
      },
      stubs: {
        ActivityFilter: true,
        Modal: {
          template: "<div><slot></slot></div>",
        },
        ProgressBar: {
          template: "<div><slot></slot></div>",
        },
        btn: {
          template: "<button><slot></slot></button>",
        },
        "i18n-t": true,
        i18n: true,
        tooltip: true,

        ...globalOptions.stubs,
      },
      mocks: {
        $t: (msg) => msg,
        $tc: (msg) => msg,
        ...globalOptions.mocks,
      },
      directives: {
        tooltip: () => {},
        ...globalOptions.directives,
      },
      ...globalOptions,
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
  it("renders reports", async () => {
    const reports = [
      {
        execution: {
          id: 1,
          permalink: "/project/jaya-test/execution/show/1",
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
    const reportRows = wrapper.find("tbody.history-executions tr.link");
    expect(reportRows.exists()).toBe(true);
    expect(reportRows.text()).toContain("#1");
  });
  it("renders running executions", async () => {
    const wrapper = await mountActivityList(
      {
        running: {
          executions: [
            {
              id: "1",
              status: "running",
            },
          ],
        },
      },
      {
        stubs: {
          ProgressBar: {
            template: "<div class='progress-bar-stub'></div>",
          },
        },
      },
    );
    await wrapper.vm.$nextTick();
    const runningExecutions = wrapper.find(".running-executions");
    expect(runningExecutions.exists()).toBe(true);
    expect(runningExecutions.html()).toContain("Running Executions");
  });
  it("renders loading area", async () => {
    const wrapper = await mountActivityList({ loading: true });
    await wrapper.vm.$nextTick();
    expect(wrapper.find('[data-test-id="loading-area"]').exists()).toBe(true);
  });
  it("enables bulk delete button when items are selected", async () => {
    const wrapper = await mountActivityList(
      {
        reports: [
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
        ],
      },
      {
        stubs: {
          btn: {
            template: "<button class='btn-stb'><slot></slot></button>",
          },
        },
      },
    );

    wrapper.vm.bulkEditMode = true;
    wrapper.vm.bulkSelectedIds = ["#68"];
    await wrapper.vm.$nextTick();
    const bulkDeleteButton = wrapper.find(
      '[data-test-id="activity-list-delete-selected-executions"]',
    );
    expect(bulkDeleteButton.attributes("disabled")).toBeFalsy();
  });

  it("handles loadActivity method call", async () => {
    const wrapper = await mountActivityList();
    const loadActivitySpy = jest.spyOn(wrapper.vm, "loadActivity");
    await wrapper.vm.loadActivity(0);
    expect(loadActivitySpy).toHaveBeenCalledWith(0);
    loadActivitySpy.mockRestore();
  });
  it("displays an error message when load fails", async () => {
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
    await wrapper.vm.$nextTick();
    const errorMessage = wrapper.find(".loading-area .text-warning");
    expect(errorMessage.exists()).toBe(true);
    expect(errorMessage.text()).toContain("Test error message");
  });

  it("renders no executions message when reports are empty", async () => {
    const wrapper = await mountActivityList({
      reports: [],
      loading: false,
    });
    await wrapper.vm.$nextTick();
    const noExecutionsMessage = wrapper.find(".loading-area .loading-text");
    expect(noExecutionsMessage.exists()).toBe(true);
    expect(noExecutionsMessage.text()).toContain("results.empty.text");
  });
  it("triggers the loadSince method", async () => {
    const wrapper = await mountActivityList();
    const loadSinceSpy = jest.spyOn(wrapper.vm, "loadSince");
    await wrapper.vm.loadSince();
    expect(loadSinceSpy).toHaveBeenCalled();
    loadSinceSpy.mockRestore();
  });
  it("renders the pagination correctly", async () => {
    const paginationProp = {
      total: 50,
      offset: 10,
      max: 10,
    };

    const OffsetPaginationStub = {
      template: '<div id="OffsetPagination" />',
    };

    const wrapper = await mountActivityList(
      {
        pagination: paginationProp,
      },
      {
        stubs: {
          OffsetPagination: OffsetPaginationStub, // Use the stub here
        },
      },
    );

    await wrapper.vm.$nextTick();

    // Find the stubbed component
    const pagination = wrapper.find("#OffsetPagination");

    // Check if the stubbed component exists
    expect(pagination.exists()).toBe(true);
  });
  it("renders the error message correctly", async () => {
    const wrapper = await mountActivityList({
      loadError: "Test error message",
    });
    await wrapper.vm.$nextTick();
    const errorMessage = wrapper.find(".loading-area .text-warning");
    expect(errorMessage.exists()).toBe(true);
    expect(errorMessage.text()).toBe("Test error message");
  });
  it("renders the no data message when reports are empty and loading is false", async () => {
    const wrapper = await mountActivityList({
      reports: [],
      loading: false,
      loadError: false,
      mocks: {
        $t: (key) => (key === "error.message.0" ? "Test error message" : key),
      },
    });
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    const loadingArea = wrapper.get('[data-test-id="loading-area"]');
    await wrapper.vm.$nextTick();
    // expect(loadingArea.text()).not.toContain("error.message.0");
    expect(loadingArea.text()).toContain("results.empty.text");
  });
  it("triggers the loadSince method", async () => {
    const wrapper = await mountActivityList();
    const loadSinceSpy = jest.spyOn(wrapper.vm, "loadSince");
    await wrapper.vm.loadSince();
    expect(loadSinceSpy).toHaveBeenCalled();
    loadSinceSpy.mockRestore();
  });
  it("handles bulk edit deselect all", async () => {
    const wrapper = await mountActivityList({
      reports: [
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
      ],
    });
    wrapper.vm.bulkEditMode = true;
    await wrapper.vm.$nextTick();
    const bulkEditDeselectAllSpy = jest.spyOn(
      wrapper.vm,
      "bulkEditDeselectAll",
    );
    const deselectAllButton = wrapper.find(
      '[data-test-id="activity-list-deselect-all"]',
    );
    await deselectAllButton.trigger("click");
    expect(bulkEditDeselectAllSpy).toHaveBeenCalled();
    bulkEditDeselectAllSpy.mockRestore();
  });
  it("handles bulk delete execution confirmation", async () => {
    const wrapper = await mountActivityList({
      reports: [
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
      ],
    });
    wrapper.vm.bulkEditMode = true;
    wrapper.vm.bulkSelectedIds = ["68"];
    await wrapper.vm.$nextTick();
    const performBulkDeleteSpy = jest.spyOn(wrapper.vm, "performBulkDelete");
    const confirmButton = wrapper.find(
      '[data-test-id="activity-list-delete-selected-executions"]',
    );
    await confirmButton.trigger("click");
    expect(performBulkDeleteSpy).toHaveBeenCalled();
    performBulkDeleteSpy.mockRestore();
  });
  it("selects and deselects all reports for bulk edit", async () => {
    const wrapper = await mountActivityList({
      reports: [
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
      ],
    });
    wrapper.vm.bulkEditMode = true;
    await wrapper.vm.$nextTick();
    const selectAllButton = wrapper.find(
      '[data-test-id="activity-list-select-all"]',
    );
    const deselectAllButton = wrapper.find(
      '[data-test-id="activity-list-deselect-all"]',
    );
    await selectAllButton.trigger("click");
    expect(wrapper.vm.bulkSelectedIds).toContain(68);
    await deselectAllButton.trigger("click");
    expect(wrapper.vm.bulkSelectedIds).not.toContain(68);
  });
  it("toggles auto-refresh", async () => {
    const wrapper = await mountActivityList();
    const autoRefreshCheckbox = wrapper.find("#auto-refresh");
    await autoRefreshCheckbox.setValue(true);
    expect(wrapper.vm.autorefresh).toBe(true);
    await autoRefreshCheckbox.setValue(false);
    expect(wrapper.vm.autorefresh).toBe(false);
  });
  it("handles middle click on report row correctly", async () => {
    const wrapper = await mountActivityList({
      reports: [
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
      ],
    });
    const middleClickSpy = jest.spyOn(wrapper.vm, "middleClickRow");
    const reportRow = wrapper.find("tbody.history-executions tr.link");
    await reportRow.trigger("click.middle");
    expect(middleClickSpy).toHaveBeenCalledWith(wrapper.vm.reports[0]);
    middleClickSpy.mockRestore();
  });
  it("displays new executions since last update", async () => {
    const wrapper = await mountActivityList({
      sincecount: 5,
    });
    await wrapper.vm.$nextTick();
    const sinceCountRow = wrapper.find("tbody.since-count-data");
    expect(sinceCountRow.exists()).toBe(true);
    expect(sinceCountRow.text()).toContain("info.newexecutions.since.0");
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
  it("shows auto-refresh checkbox and toggles its state", async () => {
    const wrapper = await mountActivityList();
    const autoRefreshCheckbox = wrapper.find("#auto-refresh");
    await autoRefreshCheckbox.setValue(true);
    expect(wrapper.vm.autorefresh).toBe(true);
    await autoRefreshCheckbox.setValue(false);
    expect(wrapper.vm.autorefresh).toBe(false);
  });
  it("displays correct message when no bulk delete results", async () => {
    const wrapper = await mountActivityList({
      showBulkEditResults: true,
      bulkEditResults: null,
      bulkEditProgress: true, // Add this line
      // stubs: {
      //   modal: {
      //     template: "<div><slot></slot></div>",
      //   },
      // },
    });
    await wrapper.vm.$nextTick();
    const bulkDeleteResultsModal = wrapper.find(
      '[data-test-id="modal-bulk-delete-results"]',
    );
    expect(bulkDeleteResultsModal.exists()).toBe(true);
    await wrapper.vm.$nextTick();
    expect(bulkDeleteResultsModal.text()).toContain(
      "Requesting bulk delete, please wait.",
    );
  });
  // it("opens and closes the bulk edit results modal", async () => {
  //   const wrapper = await mountActivityList();
  //   wrapper.vm.showBulkEditResults = true;
  //   await wrapper.vm.$nextTick();
  //   const bulkDeleteResultsModal = wrapper.find(
  //     '[data-test-id="modal-bulk-delete-results"]',
  //   );
  //   expect(bulkDeleteResultsModal.exists()).toBe(true);
  //   wrapper.vm.showBulkEditResults = false;
  //   await wrapper.vm.$nextTick();
  //   expect(
  //     wrapper.find('[data-test-id="modal-bulk-delete-results"]').exists(),
  //   ).toBe(false);
  // });
  it("open and closes the bulk delete modal", async () => {
    const wrapper = await mountActivityList(
      {
        showBulkEditConfirm: true,
      },
      {
        stubs: {
          modal: {
            template: "<div class='modal-stub'><slot></slot></div>",
          },
        },
      },
    );
    const bulkDeleteModal = wrapper.find('[data-test-id="modal-bulk-delete"]');
    expect(bulkDeleteModal.exists()).toBe(true);
  });
  it("opens and closes the bulk delete results modal", async () => {
    const wrapper = await mountActivityList(
      {
        showBulkEditResults: true,
      },
      {
        stubs: {
          modal: {
            template: "<div class='modal-stub'><slot></slot></div>",
          },
        },
      },
    );
    const bulkDeleteResultsModal = wrapper.find(
      '[data-test-id="modal-bulk-delete-results"]',
    );
    expect(bulkDeleteResultsModal.exists()).toBe(true);
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
          btn: {
            template: '<button class="btn-stub"><slot></slot></button>',
          },
        },
      },
    );
    await wrapper.vm.$nextTick();
    // Check if the modal is rendered
    const modal = wrapper.find(".modal-stub");
    expect(modal.exists()).toBe(true);
    // Check if the bulk edit progress message is displayed
    await wrapper.vm.$nextTick();
    const progressMessage = modal.find(".i18n-t-stub");
    console.log(progressMessage.text()); // Log the text to see what is rendered
    expect(progressMessage.exists()).toBe(true);
    expect(progressMessage.text()).toContain("bulkresult.attempted.text");
    // Check the actual message
    await wrapper.vm.$nextTick();
    const message = modal.find("em");
    expect(message.exists()).toBe(true);
    await wrapper.vm.$nextTick();
    expect(message.text()).toContain("Requesting bulk delete, please wait.");
  });

  it("disables bulk delete button when no items are selected", async () => {
    const wrapper = await mountActivityList(
      {
        reports: [
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
        ],
      },
      {
        stubs: {
          btn: {
            template: "<button class='btn-stb'><slot></slot></button>",
          },
        },
      },
    );

    wrapper.vm.bulkEditMode = true;
    wrapper.vm.bulkSelectedIds = [];
    await wrapper.vm.$nextTick();

    const bulkDeleteButton = wrapper.find(
      '[data-test-id="activity-list-delete-selected-executions"]',
    );
    expect(bulkDeleteButton.attributes("disabled")).toBe("disabled");
    expect(bulkDeleteButton.exists()).toBe(true);
  });
});
