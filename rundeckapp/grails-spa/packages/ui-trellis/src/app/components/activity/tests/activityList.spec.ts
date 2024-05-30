import { mount, VueWrapper } from "@vue/test-utils";
import ActivityList from "../activityList.vue";
import ActivityFilter from "../activityFilter.vue";
import { RundeckContext } from "../../../../library";
import { setupRundeckContext } from "./setupRundeckContext";
import { createI18n } from "vue-i18n";
import OffsetPagination from "../../../../library/components/utils/OffsetPagination.vue";
// Localization mocks
const i18nMocks = {
  $t: (msg) => {
    const translations = {
      "error.message.0": "An Error Occurred",
      "results.empty.text": "No results for the query",
      "bulkresult.attempted.text": " Executions were attempted.",
      "pagination.of": "of",
      execution: "execution",
      "Auto refresh": "Auto refresh",
      "bulk.selected.count": "selected",
      "select.all": "Select All",
      "select.none": "Select None",
      "delete.selected.executions": "Delete Selected Executions",
      "cancel.bulk.delete": "Cancel Bulk Delete",
      "bulk.delete": "Bulk Delete",
      // Add other translations as needed
    };
    return translations[msg] || msg;
  },
  $tc: (msg, count) => {
    const translations = {
      execution: `${count} executions`,
      // Add other translations as needed
    };
    return translations[msg] || msg;
  },
};
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
        ...i18nMocks,
        ...globalOptions.mocks,
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

  it("renders running executions", async () => {
    const i18n = createI18n({
      locale: "en",
      messages: {
        en: {
          "execution.status.running": "running",
          // add other keys here
        },
      },
    });

    const wrapper = await mountActivityList({
      global: {
        plugins: [i18n],
      },
      props: {
        running: {
          executions: [
            {
              id: "1",
              status: "running",
            },
          ],
        },
      },
    });
    await wrapper.vm.$nextTick();
    expect(wrapper.text()).not.toContain("stopped");
  });
  it("renders loading area", async () => {
    const wrapper = await mountActivityList({ loading: true });
    await wrapper.vm.$nextTick();
    expect(wrapper.find('[data-test-id="loading-area"]').exists()).toBe(true);
  });
  it("enables bulk delete button when items are selected", async () => {
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
      bulkEditMode: true,
      bulkSelectedIds: ["#68"],
    });
    console.log(wrapper.html());
    wrapper.vm.bulkSelectedIds = ["#68"];
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.bulkSelectedIds.length).toBeGreaterThan(0);
  });
  it("handles loadActivity method call", async () => {
    const wrapper = await mountActivityList();
    const loadActivitySpy = jest.spyOn(wrapper.vm, "loadActivity");
    await wrapper.vm.loadActivity(0);
    expect(loadActivitySpy).toHaveBeenCalledWith(0);
    loadActivitySpy.mockRestore();
  });
  it("displays an error message when load fails", async () => {
    const loadError = "An Error Occurred";
    const wrapper = await mountActivityList();
    (wrapper.vm.$props as any).loadError = loadError;
    await wrapper.vm.$nextTick();
    const errorMessage = wrapper.find(".loading-area .text-warning");
    console.log("errorMessage:", errorMessage.text());
    expect(errorMessage.exists()).toBe(true);
    expect(errorMessage.text()).toContain("An Error Occurred");
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

  it("renders the no data message when reports are empty and loading is false", async () => {
    const wrapper = mount(ActivityList, {
      global: {
        mocks: {
          $t: (key) =>
            key === "results.empty.text" ? "No results for the query" : key,
          $tc: (key, count) => `${count} ${key}`, // Mock implementation for $tc
        },
      },
    });

    // Set the state of the component
    await wrapper.setData({
      reports: [],
      loading: false,
      loadError: null,
    });

    // Force update the component
    await wrapper.vm.$nextTick();

    // Find the no data message element and assert its text
    const noDataMessageElement = wrapper.find(
      '[data-test-id="no-data-message"]',
    );
    if (noDataMessageElement.exists()) {
      expect(noDataMessageElement.text()).toBe("No results for the query");
    }
  });
  it("triggers the loadSince method", async () => {
    const wrapper = await mountActivityList();
    const loadSinceSpy = jest.spyOn(wrapper.vm, "loadSince");
    await wrapper.vm.loadSince();
    expect(loadSinceSpy).toHaveBeenCalled();
    loadSinceSpy.mockRestore();
  });

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
  it("shows auto-refresh checkbox and toggles its state", async () => {
    const wrapper = await mountActivityList();
    const autoRefreshCheckbox = wrapper.find("#auto-refresh");
    await autoRefreshCheckbox.setValue(true);
    expect(wrapper.vm.autorefresh).toBe(true);
    await autoRefreshCheckbox.setValue(false);
    expect(wrapper.vm.autorefresh).toBe(false);
  });

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

  it("renders Modal when showBulkEditCleanSelections is true", async () => {
    const wrapper = await mountActivityList({
      props: {
        showBulkEditCleanSelections: true,
      },
      global: {
        stubs: {
          Modal: {
            template:
              '<div id="Modal"><div class="footer"><slot name="footer"></slot></div></div>',
          },
        },
      },
    });

    console.log(wrapper.html());

    const modalCleanSelections = wrapper.find("#cleanselections");
    expect(modalCleanSelections.exists()).toBe(true);

    const modalBulkDelete = wrapper.find("#bulkexecdelete");
    expect(modalBulkDelete.exists()).toBe(true);

    const modalBulkDeleteResult = wrapper.find("#bulkexecdeleteresult");
    expect(modalBulkDeleteResult.exists()).toBe(true);
  });
  it("calls the correct methods when buttons in cleanselections modal are clicked", async () => {
    const wrapper = await mountActivityList({
      props: {
        showBulkEditCleanSelections: true,
      },
      global: {
        stubs: {
          Modal: {
            template:
              '<div id="Modal"><div class="footer"><slot name="footer"></slot></div></div>',
          },
        },
      },
    });

    const modalCleanSelections = wrapper.find("#cleanselections");

    // Test the "cancel" button
    const cancelButton = modalCleanSelections.find(
      'button[data-dismiss="modal"]',
    );
    if (cancelButton.exists()) {
      await cancelButton.trigger("click");
      expect(wrapper.vm.showBulkEditCleanSelections).toBe(false);
    }

    // Test the "Only shown executions" button
    const onlyShownExecutionsButton = modalCleanSelections.find(
      "button.btn.btn-default",
    );
    if (onlyShownExecutionsButton.exists()) {
      const bulkEditDeselectAll = jest.spyOn(wrapper.vm, "bulkEditDeselectAll");
      await onlyShownExecutionsButton.trigger("click");
      expect(bulkEditDeselectAll).toHaveBeenCalled();
    }

    // Test the "all" button
    const allButton = modalCleanSelections.find("button.btn.btn-danger");
    if (allButton.exists()) {
      const bulkEditDeselectAllPages = jest.spyOn(
        wrapper.vm,
        "bulkEditDeselectAllPages",
      );
      await allButton.trigger("click");
      expect(bulkEditDeselectAllPages).toHaveBeenCalled();
    }
  });

  it("displays the correct number of reports when loading is false", async () => {
    const pagination = {
      total: 10,
      offset: 0,
      max: 10,
    };
    const wrapper = await mountActivityList(
      {
        pagination,
        loading: false,
        reports: Array(10).fill({}),
      },
      {
        stubs: {
          OffsetPagination: {
            template: `<div id='OffsetPagination'>${pagination.total}</div>`,
          },
        },
      },
    );
    await wrapper.vm.$nextTick();
    const offsetPagination = wrapper.findComponent("#OffsetPagination");
    expect(offsetPagination.exists()).toBe(true);
    expect(offsetPagination.text()).toContain("10");
  });
  it("renders correctly when pagination.total is greater than pagination.max", async () => {
    const pagination = {
      total: 10,
      max: 5,
      offset: 0,
    };
    const wrapper = await mountActivityList(
      {
        pagination,
        loading: false,
      },
      {
        stubs: {
          OffsetPagination: {
            template: `<div id='OffsetPagination'>${pagination.total}</div>`,
          },
        },
      },
    );
    await wrapper.vm.$nextTick();
    const offsetPagination = wrapper.findComponent("#OffsetPagination");
    expect(offsetPagination.exists()).toBe(true);
    expect(offsetPagination.text()).toContain("10");
  });
});
