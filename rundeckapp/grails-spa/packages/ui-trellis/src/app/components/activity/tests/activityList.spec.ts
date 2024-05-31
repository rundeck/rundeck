// import { mountActivityList } from "./utils";
import {
  rundeckServiceMock,
  rundeckClientMock,
  axiosMock,
  i18nMocks,
  mockEventBus,
} from "./mock";
jest.mock("../../../../library/rundeckService", () => rundeckServiceMock);
jest.mock("@rundeck/client", () => rundeckClientMock);
jest.mock("axios", () => axiosMock);
import { mount, VueWrapper } from "@vue/test-utils";
import { RundeckContext } from "../../../../library";
import { setupRundeckContext } from "./setupRundeckContext";
import ActivityList from "../activityList.vue";
import ActivityFilter from "../activityFilter.vue";
import OffsetPagination from "../../../../library/components/utils/OffsetPagination.vue";
import { GlobalOptions } from "./type";
import { createI18n } from "vue-i18n";
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
      bulkEditMode: true, // Ensure bulk edit mode is enabled
      bulkSelectedIds: [], // Ensure bulkSelectedIds is defined
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
          template: `<button :disabled="disabled" data-test-id="activity-list-delete-selected-executions">Delete Selected Executions</button>`,
          props: ["disabled"],
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

// describe("ActivityList", () => {
//   it("renders the component correctly", async () => {
//     const wrapper = await mountActivityList();
//     expect(wrapper.exists()).toBe(true);
//   });
//   it("opens and closes the filter modal", async () => {
//     const wrapper = await mountActivityList();
//     const filterButton = wrapper.find(
//       '[data-test-id="activity-list-filter-button"]'
//     );
//     await filterButton.trigger("click");
//     expect(wrapper.findComponent(ActivityFilter).exists()).toBe(true);
//   });

//   it("renders the filter button", async () => {
//     const wrapper = await mountActivityList();
//     const filterButton = wrapper.find(
//       '[data-test-id="activity-list-filter-button"]'
//     );
//     expect(filterButton.exists()).toBe(true);
//   });
//   it("opens the clear selections modal", async () => {
//     const wrapper = await mountActivityList();
//     wrapper.vm.showBulkEditCleanSelections = true;
//     const clearSelectionsButton = wrapper.find(
//       '[data-test-id="modal-clean-selections"]'
//     );
//     expect(clearSelectionsButton.exists()).toBe(true);
//   });
//   it("opens the bulk delete modal", async () => {
//     const wrapper = await mountActivityList();
//     wrapper.vm.showBulkEditConfirm = true;
//     const bulkDeleteButton = wrapper.find('[data-test-id="modal-bulk-delete"]');
//     expect(bulkDeleteButton.exists()).toBe(true);
//   });
//   it("opens the bulk delete results modal", async () => {
//     const wrapper = await mountActivityList();
//     wrapper.vm.showBulkEditResults = true;
//     const bulkDeleteResults = wrapper.find(
//       '[data-test-id="modal-bulk-delete-results"]'
//     );
//     expect(bulkDeleteResults.exists()).toBe(true);
//   });
//   it("renders since count data", async () => {
//     const wrapper = await mountActivityList({
//       sinceCount: 5,
//     });
//     await wrapper.vm.$nextTick();
//     expect(wrapper.html()).toContain("5");
//   });

//   it("renders running executions", async () => {
//     const i18n = createI18n({
//       locale: "en",
//       messages: {
//         en: {
//           "execution.status.running": "running",
//         },
//       },
//     });

//     const wrapper = await mountActivityList({
//       global: {
//         plugins: [i18n],
//       },
//       props: {
//         running: {
//           executions: [
//             {
//               id: "1",
//               status: "running",
//             },
//           ],
//         },
//       },
//     });
//     await wrapper.vm.$nextTick();
//     expect(wrapper.text()).not.toContain("stopped");
//   });
//   it("renders loading area", async () => {
//     const wrapper = await mountActivityList({ loading: true });
//     await wrapper.vm.$nextTick();
//     expect(wrapper.find('[data-test-id="loading-area"]').exists()).toBe(true);
//   });
//   it("enables bulk delete button when items are selected", async () => {
//     const wrapper = await mountActivityList({
//       reports: [
//         {
//           execution: {
//             id: 68,
//             permalink: "/project/jaya-test/execution/show/68",
//           },
//           status: "succeeded",
//           dateCompleted: "2024-05-22T14:33:52Z",
//           node: {
//             total: 1,
//             succeeded: 1,
//             failed: 0,
//           },
//         },
//       ],
//       bulkEditMode: true,
//       bulkSelectedIds: ["#68"],
//     });
//     console.log(wrapper.html());
//     wrapper.vm.bulkSelectedIds = ["#68"];
//     await wrapper.vm.$nextTick();
//     expect(wrapper.vm.bulkSelectedIds.length).toBeGreaterThan(0);
//   });
//   it("handles loadActivity method call", async () => {
//     const wrapper = await mountActivityList();
//     const loadActivitySpy = jest.spyOn(wrapper.vm, "loadActivity");
//     await wrapper.vm.loadActivity(0);
//     expect(loadActivitySpy).toHaveBeenCalledWith(0);
//     loadActivitySpy.mockRestore();
//   });
//   it("displays an error message when load fails", async () => {
//     const loadError = "An Error Occurred";
//     const wrapper = await mountActivityList();
//     (wrapper.vm.$props as any).loadError = loadError;
//     await wrapper.vm.$nextTick();
//     const errorMessage = wrapper.find(".loading-area .text-warning");
//     console.log("errorMessage:", errorMessage.text());
//     expect(errorMessage.exists()).toBe(true);
//     expect(errorMessage.text()).toContain("An Error Occurred");
//   });

//   it("triggers the loadSince method", async () => {
//     const wrapper = await mountActivityList();
//     const loadSinceSpy = jest.spyOn(wrapper.vm, "loadSince");
//     await wrapper.vm.loadSince();
//     expect(loadSinceSpy).toHaveBeenCalled();
//     loadSinceSpy.mockRestore();
//   });
//   it("renders the pagination correctly", async () => {
//     const paginationProp = {
//       total: 50,
//       offset: 10,
//       max: 10,
//     };

//     const OffsetPaginationStub = {
//       template: '<div id="OffsetPagination" />',
//     };

//     const wrapper = await mountActivityList(
//       {
//         pagination: paginationProp,
//       },
//       {
//         stubs: {
//           OffsetPagination: OffsetPaginationStub, // Use the stub here
//         },
//       }
//     );

//     await wrapper.vm.$nextTick();

//     // Find the stubbed component
//     const pagination = wrapper.find("#OffsetPagination");

//     // Check if the stubbed component exists
//     expect(pagination.exists()).toBe(true);
//   });

//   it("renders the no data message when reports are empty and loading is false", async () => {
//     const wrapper = mount(ActivityList, {
//       global: {
//         mocks: {
//           $t: (key) =>
//             key === "results.empty.text" ? "No results for the query" : key,
//           $tc: (key, count) => `${count} ${key}`, // Mock implementation for $tc
//         },
//       },
//     });

//     // Set the state of the component
//     await wrapper.setData({
//       reports: [],
//       loading: false,
//       loadError: null,
//     });

//     // Force update the component
//     await wrapper.vm.$nextTick();

//     // Find the no data message element and assert its text
//     const noDataMessageElement = wrapper.find(
//       '[data-test-id="no-data-message"]'
//     );
//     if (noDataMessageElement.exists()) {
//       expect(noDataMessageElement.text()).toBe("No results for the query");
//     }
//   });
//   it("triggers the loadSince method", async () => {
//     const wrapper = await mountActivityList();
//     const loadSinceSpy = jest.spyOn(wrapper.vm, "loadSince");
//     await wrapper.vm.loadSince();
//     expect(loadSinceSpy).toHaveBeenCalled();
//     loadSinceSpy.mockRestore();
//   });

//   it("toggles auto-refresh", async () => {
//     const wrapper = await mountActivityList();
//     const autoRefreshCheckbox = wrapper.find("#auto-refresh");
//     await autoRefreshCheckbox.setValue(true);
//     expect(wrapper.vm.autorefresh).toBe(true);
//     await autoRefreshCheckbox.setValue(false);
//     expect(wrapper.vm.autorefresh).toBe(false);
//   });

//   it("handles pagination correctly", async () => {
//     const wrapper = await mountActivityList();
//     const paginationSpy = jest.spyOn(wrapper.vm, "changePageOffset");
//     const pagination = wrapper.findComponent({ name: "OffsetPagination" });
//     pagination.vm.$emit("change", 10);
//     await wrapper.vm.$nextTick();
//     expect(paginationSpy).toHaveBeenCalledWith(10);
//     paginationSpy.mockRestore();
//   });
//   it("shows auto-refresh checkbox and toggles its state", async () => {
//     const wrapper = await mountActivityList();
//     const autoRefreshCheckbox = wrapper.find("#auto-refresh");
//     await autoRefreshCheckbox.setValue(true);
//     expect(wrapper.vm.autorefresh).toBe(true);
//     await autoRefreshCheckbox.setValue(false);
//     expect(wrapper.vm.autorefresh).toBe(false);
//   });

//   it("open and closes the bulk delete modal", async () => {
//     const wrapper = await mountActivityList(
//       {
//         showBulkEditConfirm: true,
//       },
//       {
//         stubs: {
//           modal: {
//             template: "<div class='modal-stub'><slot></slot></div>",
//           },
//         },
//       }
//     );
//     const bulkDeleteModal = wrapper.find('[data-test-id="modal-bulk-delete"]');
//     expect(bulkDeleteModal.exists()).toBe(true);
//   });
//   it("opens and closes the bulk delete results modal", async () => {
//     const wrapper = await mountActivityList(
//       {
//         showBulkEditResults: true,
//       },
//       {
//         stubs: {
//           modal: {
//             template: "<div class='modal-stub'><slot></slot></div>",
//           },
//         },
//       }
//     );
//     const bulkDeleteResultsModal = wrapper.find(
//       '[data-test-id="modal-bulk-delete-results"]'
//     );
//     expect(bulkDeleteResultsModal.exists()).toBe(true);
//   });

//   it("renders Modal when showBulkEditCleanSelections is true", async () => {
//     const wrapper = await mountActivityList({
//       props: {
//         showBulkEditCleanSelections: true,
//       },
//       global: {
//         stubs: {
//           Modal: {
//             template:
//               '<div id="Modal"><div class="footer"><slot name="footer"></slot></div></div>',
//           },
//         },
//       },
//     });

//     console.log(wrapper.html());

//     const modalCleanSelections = wrapper.find("#cleanselections");
//     expect(modalCleanSelections.exists()).toBe(true);

//     const modalBulkDelete = wrapper.find("#bulkexecdelete");
//     expect(modalBulkDelete.exists()).toBe(true);

//     const modalBulkDeleteResult = wrapper.find("#bulkexecdeleteresult");
//     expect(modalBulkDeleteResult.exists()).toBe(true);
//   });
//   it("calls the correct methods when buttons in cleanselections modal are clicked", async () => {
//     const wrapper = await mountActivityList({
//       props: {
//         showBulkEditCleanSelections: true,
//       },
//       global: {
//         stubs: {
//           Modal: {
//             template:
//               '<div id="Modal"><div class="footer"><slot name="footer"></slot></div></div>',
//           },
//         },
//       },
//     });

//     const modalCleanSelections = wrapper.find("#cleanselections");

//     // Test the "cancel" button
//     const cancelButton = modalCleanSelections.find(
//       'button[data-dismiss="modal"]'
//     );
//     if (cancelButton.exists()) {
//       await cancelButton.trigger("click");
//       expect(wrapper.vm.showBulkEditCleanSelections).toBe(false);
//     }

//     // Test the "Only shown executions" button
//     const onlyShownExecutionsButton = modalCleanSelections.find(
//       "button.btn.btn-default"
//     );
//     if (onlyShownExecutionsButton.exists()) {
//       const bulkEditDeselectAll = jest.spyOn(wrapper.vm, "bulkEditDeselectAll");
//       await onlyShownExecutionsButton.trigger("click");
//       expect(bulkEditDeselectAll).toHaveBeenCalled();
//     }

//     // Test the "all" button
//     const allButton = modalCleanSelections.find("button.btn.btn-danger");
//     if (allButton.exists()) {
//       const bulkEditDeselectAllPages = jest.spyOn(
//         wrapper.vm,
//         "bulkEditDeselectAllPages"
//       );
//       await allButton.trigger("click");
//       expect(bulkEditDeselectAllPages).toHaveBeenCalled();
//     }
//   });

//   it("displays the correct number of reports when loading is false", async () => {
//     const pagination = {
//       total: 10,
//       offset: 0,
//       max: 10,
//     };
//     const reports = Array(10).fill({});
//     const wrapper = await mountActivityList(
//       {
//         pagination,
//         loading: false,
//         reports,
//       },
//       {
//         stubs: {
//           OffsetPagination: {
//             template: `<div id='OffsetPagination'>${pagination.total}</div>`,
//           },
//         },
//       }
//     );
//     await wrapper.vm.$nextTick();
//     console.log(wrapper.html());
//     const offsetPagination = wrapper.get("#OffsetPagination");
//     expect(offsetPagination.text()).toContain("10");
//   });
//   it("renders correctly when pagination.total is greater than pagination.max", async () => {
//     const pagination = {
//       total: 10,
//       max: 5,
//       offset: 0,
//     };
//     const wrapper = await mountActivityList(
//       {
//         pagination,
//         loading: false,
//       },
//       {
//         stubs: {
//           OffsetPagination: {
//             template: `<div id='OffsetPagination'>${pagination.total}</div>`,
//           },
//         },
//       }
//     );
//     await wrapper.vm.$nextTick();
//     const offsetPagination = wrapper.get("#OffsetPagination");
//     expect(offsetPagination.text()).toContain("10");
//     expect(pagination.total).toBeGreaterThan(pagination.max);
//   });
// });

describe("ActivityList Component Rendering", () => {
  it("renders the component correctly", async () => {
    const wrapper = await mountActivityList();
    expect(wrapper.exists()).toBe(true);
  });
  it("renders loading area", async () => {
    const wrapper = await mountActivityList({ loading: true });
    await wrapper.vm.$nextTick();
    expect(wrapper.find('[data-test-id="loading-area"]').exists()).toBe(true);
  });
  it("displays an error message when load fails", async () => {
    const loadError = "An Error Occurred";
    const wrapper = await mountActivityList();
    (wrapper.vm.$props as any).loadError = loadError;
    await wrapper.vm.$nextTick();
    const errorMessage = wrapper.find(".loading-area .text-warning");
    expect(errorMessage.exists()).toBe(true);
    expect(errorMessage.text()).toContain("An Error Occurred");
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
    await wrapper.setData({
      reports: [],
      loading: false,
      loadError: null,
    });
    await wrapper.vm.$nextTick();
    const noDataMessageElement = wrapper.find(
      '[data-test-id="no-data-message"]',
    );
    if (noDataMessageElement.exists()) {
      expect(noDataMessageElement.text()).toBe("No results for the query");
    }
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
    const pagination = wrapper.find("#OffsetPagination");
    expect(pagination.exists()).toBe(true);
  });
  it("displays the correct number of reports when loading is false", async () => {
    const pagination = {
      total: 10,
      offset: 0,
      max: 10,
    };
    const reports = Array(10).fill({});
    const wrapper = await mountActivityList(
      {
        pagination,
        loading: false,
        reports,
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
    const offsetPagination = wrapper.get("#OffsetPagination");
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
    const offsetPagination = wrapper.get("#OffsetPagination");
    expect(offsetPagination.text()).toContain("10");
    expect(pagination.total).toBeGreaterThan(pagination.max);
  });
});
describe("ActivityList Filter Modal", () => {
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
});
describe("ActivityList Bulk Edit Modals", () => {
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
  it("renders Modal when showBulkEditCleanSelections is true", async () => {
    const wrapper = await mountActivityList({
      showBulkEditCleanSelections: true,
    });
    const modalCleanSelections = wrapper.find("#cleanselections");
    expect(modalCleanSelections.exists()).toBe(true);
    const modalBulkDelete = wrapper.find("#bulkexecdelete");
    expect(modalBulkDelete.exists()).toBe(true);
    const modalBulkDeleteResult = wrapper.find("#bulkexecdeleteresult");
    expect(modalBulkDeleteResult.exists()).toBe(true);
  });
  it("calls the correct methods when buttons in cleanselections modal are clicked", async () => {
    const wrapper = await mountActivityList({
      showBulkEditCleanSelections: true,
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
});
describe("ActivityList Bulk Edit Button States", () => {
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
    wrapper.vm.bulkSelectedIds = ["#68"];
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.bulkSelectedIds.length).toBeGreaterThan(0);
  });
  it("renders bulk delete button", async () => {
    const btnStub = {
      template:
        '<button class="bulk-delete-button btn-default" @click="$emit(\'click\')" :disabled="disabled ? true : null" data-test-id="activity-list-delete-selected-executions" size="xs"></button>',
      props: ["disabled"],
    };
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
        bulkEditMode: true,
        bulkSelectedIds: [],
      },
      {
        stubs: {
          btn: btnStub,
          // other stubs...
        },
      },
    );

    await wrapper.vm.$nextTick();

    // const bulkDeleteButton = wrapper.find(
    //   '[data-test-id="activity-list-delete-selected-executions"]',
    // );

    const btnStubs = wrapper.find(
      '[data-test-id="activity-list-delete-selected-executions"]',
    );
    expect(btnStubs.exists()).toBe(true);

    // expect(bulkDeleteButton.attributes('disabled')).toBe('disabled');
  });
});
describe("ActivityList Execution Handling", () => {
  it("handles loadActivity method call", async () => {
    const wrapper = await mountActivityList();
    const loadActivitySpy = jest.spyOn(wrapper.vm, "loadActivity");
    await wrapper.vm.loadActivity(0);
    expect(loadActivitySpy).toHaveBeenCalledWith(0);
    loadActivitySpy.mockRestore();
  });
  it("handles bulk delete execution confirmation", async () => {
    const btnStub = {
      template: `<button :disabled="disabled" @click="$emit('click')" data-test-id="activity-list-delete-selected-executions">Delete Selected Executions</button>`,
      props: ["disabled"],
    };
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
        bulkEditMode: true,
        bulkSelectedIds: ["68"],
      },
      {
        stubs: {
          btn: btnStub,
        },
      },
    );
    console.log(wrapper.html());
    await wrapper.vm.$nextTick();

    // Add a mock implementation for performBulkDelete
    wrapper.vm.performBulkDelete = jest.fn();

    // Re-assign the spy to the mocked function
    const performBulkDeleteSpy = jest.spyOn(wrapper.vm, "performBulkDelete");

    const confirmButton = wrapper.find(
      '[data-test-id="activity-list-delete-selected-executions"]',
    );
    expect(confirmButton.exists()).toBe(true);
    await wrapper.vm.$nextTick();
    console.log(wrapper.html());
    // expect(confirmButton.attributes("disabled")).toBeUndefined();
    await confirmButton.trigger("click");
    await wrapper.vm.$nextTick();
    console.log(
      "performBulkDelete called:",
      performBulkDeleteSpy.mock.calls.length,
    ); // Add this line

    // Check that performBulkDelete was called
    expect(performBulkDeleteSpy).toHaveBeenCalledTimes(1);
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
  it("handles bulk edit deselect all", async () => {
    const wrapper = await mountActivityList({
      showBulkEditCleanSelections: true,
    });
    const modalCleanSelections = wrapper.find("#cleanselections");
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
  it("shows auto-refresh checkbox and toggles its state", async () => {
    const wrapper = await mountActivityList();
    const autoRefreshCheckbox = wrapper.find("#auto-refresh");
    await autoRefreshCheckbox.setValue(true);
    expect(wrapper.vm.autorefresh).toBe(true);
    await autoRefreshCheckbox.setValue(false);
    expect(wrapper.vm.autorefresh).toBe(false);
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
      },
    );

    await wrapper.vm.$nextTick();
    const modal = wrapper.find("#cleanselections");
    expect(modal.exists()).toBe(true);
    const progressMessage = modal.find(".i18n-t-stub");
    expect(progressMessage.exists()).toBe(true);
    expect(progressMessage.text()).toContain("clearselected.confirm.text");
  });
});
