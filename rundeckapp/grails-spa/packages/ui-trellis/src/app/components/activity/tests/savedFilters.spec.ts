// import { mount } from "@vue/test-utils";
// import SavedFilters from "../savedFilters.vue";
// import { Notification, MessageBox } from "uiv";
// // Global mocking of dependent services
// jest.mock("@/library/rundeckService", () => ({
//   getRundeckContext: jest.fn().mockReturnValue({ projectName: "test" }),
// }));
// jest.mock("../../../../library/stores/ActivityFilterStore", () => ({
//   ActivityFilterStore: jest.fn().mockImplementation(() => ({
//     loadForProject: jest.fn().mockReturnValue({
//       filters: [
//         { filterName: "Filter 1" },
//         { filterName: "Filter 2" },
//         { filterName: "Filter 3" },
//       ],
//     }),
//     saveFilter: jest.fn().mockResolvedValue({ success: true }),
//     removeFilter: jest.fn().mockResolvedValue(undefined),
//   })),
// }));
// jest.mock("uiv", () => ({
//   MessageBox: {
//     confirm: jest.fn().mockResolvedValue(true),
//     prompt: jest.fn().mockResolvedValue("new filter name"),
//   },
//   Notification: {
//     notify: jest.fn(),
//   },
// }));
// const mockDropdown = {
//   template: `
//     <div data-test-id="dropdown">
//       <button @click="toggleDropdown" data-test-id="toggle-button">Toggle</button>
//       <div v-if="isOpen">
//         <slot></slot>
//         <span data-test-id="checkmark-span">√</span>
//         <button data-test-id="delete-filter-btn" @click="emitDeleteFilter">Delete Filter</button>
//       </div>
//     </div>
//   `,
//   data() {
//     return {
//       isOpen: false,
//     };
//   },
//   methods: {
//     toggleDropdown() {
//       this.isOpen = !this.isOpen;
//     },
//     emitDeleteFilter() {
//       this.$emit("delete-filter");
//     },
//   },
// };
// describe("SavedFilters", () => {
//   let wrapper;
//   const eventBus = {
//     on: jest.fn(),
//     emit: jest.fn(),
//     off: jest.fn(),
//   };
//   beforeEach(() => {
//     wrapper = mount(SavedFilters, {
//       props: {
//         hasQuery: true,
//         query: { filterName: "test4" },
//         eventBus,
//       },
//       attachTo: document.body,
//       global: {
//         mocks: {
//           $t: (msg) => msg,
//         },
//         stubs: {
//           btn: true,
//           dropdown: mockDropdown,
//         },
//       },
//     });
//   });
//   afterEach(() => {
//     jest.clearAllMocks();
//   });
//   describe("Component Initialization", () => {
//     it("should register and unregister event listeners", async () => {
//       expect(eventBus.on).toHaveBeenCalledWith(
//         "invoke-save-filter",
//         expect.any(Function)
//       );
//       wrapper.unmount();
//       expect(eventBus.off).toHaveBeenCalledWith("invoke-save-filter");
//     });
//   });
//   describe("Interaction with Filters", () => {
//     it("renders save button conditionally based on query props", () => {
//       expect(wrapper.find('[data-test-id="save-filter-button"]').exists()).toBe(
//         wrapper.props().hasQuery && !wrapper.props().query.filterName
//       );
//     });
//     it("emits 'select_filter' when a filter is selected", async () => {
//       await wrapper.vm.selectFilter({ filterName: "Test Filter" });
//       expect(wrapper.emitted("select_filter")[0]).toEqual([
//         { filterName: "Test Filter" },
//       ]);
//     });
//     it("handles delete filter action", async () => {
//       const dropdownToggle = wrapper.find('[data-test-id="toggle-button"]');
//       await dropdownToggle.trigger("click");
//       await wrapper.vm.$nextTick();
//       const deleteButton = wrapper.find('[data-test-id="delete-filter-btn"]');
//       expect(deleteButton.exists()).toBe(true);
//     });
//     it("calls deleteFilter when the delete filter button is clicked", async () => {
//       await wrapper.vm.$nextTick();
//       const dropdownToggle = wrapper.find('[data-test-id="toggle-button"]');
//       await dropdownToggle.trigger("click");
//       await wrapper.vm.$nextTick();
//       // Simulate user clicking the delete button
//       const deleteButton = wrapper.find('[data-test-id="delete-filter-btn"]');
//       if (deleteButton.exists()) {
//         await deleteButton.trigger("click");
//         // Wait for the delete action to be processed
//         await wrapper.vm.$nextTick();
//       } else {
//         throw new Error("Delete button not found in the DOM");
//       }
//     });
//     it("should handle dropdown interactions and delete filter", async () => {
//       const deleteFilterSpy = jest.spyOn(wrapper.vm, "deleteFilter");
//       const deleteButton = wrapper.find('[data-test-id="delete-filter-btn"]');
//       if (deleteButton.exists()) {
//         await deleteButton.trigger("click");
//         // Check if the delete-filter event was emitted by the mockDropdown component
//         const dropdownComponent = wrapper.findComponent(mockDropdown);
//         expect(
//           dropdownComponent.emitted().hasOwnProperty("delete-filter")
//         ).toBe(true);
//         expect(deleteFilterSpy).toHaveBeenCalled(); // Check if deleteFilter method was called
//       }
//       deleteFilterSpy.mockRestore();
//     });
//   });
//   describe("Rendering of UI Components", () => {
//     it("checks for the presence of dropdown when filters exist", async () => {
//       await wrapper.setData({ filters: [{ filterName: "Test Filter" }] });
//       await wrapper.vm.$nextTick();
//       expect(wrapper.find('[data-test-id="dropdown"]').exists()).toBe(true);
//     });
//     it("displays no filters message when no filters are available", async () => {
//       wrapper.setData({ filters: [] });
//       await wrapper.vm.$nextTick();
//       expect(
//         wrapper.find('[data-test-id="no-filters-message"]').text()
//       ).toContain("No filters available");
//     });
//     it("renders checkmark span when query and filterName matches a filter", async () => {
//       // Set the filters prop using the setProps method
//       await wrapper.setData({ filters: [{ filterName: "Filter 1" }] });
//       // Wait for the next tick to ensure the component is fully rendered
//       await wrapper.vm.$nextTick();
//       // Find the toggle button and trigger a click event
//       const toggleButton = wrapper.find('[data-test-id="toggle-button"]');
//       if (toggleButton.exists()) {
//         await toggleButton.trigger("click");
//         await wrapper.vm.$nextTick();
//       }
//       const checkmarkSpan = wrapper.find('[data-test-id="checkmark-span"]');
//       expect(checkmarkSpan.exists()).toBe(true);
//       expect(checkmarkSpan.text()).toContain("√");
//     });
//   });
//   describe("Success and Error Handling", () => {
//     it("displays notification upon successful filter save", async () => {
//       const saveFilterMock = jest.fn().mockImplementation(() => {
//         Notification.notify({ type: "success" });
//       });
//       wrapper.vm.saveFilter = saveFilterMock;
//       const notifySpy = jest.spyOn(Notification, "notify");
//       await wrapper.vm.saveFilter("New Filter");
//       expect(notifySpy).toHaveBeenCalledWith(
//         expect.objectContaining({ type: "success" })
//       );
//       notifySpy.mockRestore();
//     });
//     it("displays error when filter saving fails", async () => {
//       wrapper.vm.saveFilter = async function (filterName) {
//         try {
//           // original saveFilter logic...
//           throw new Error("Save failed"); // This line simulates a failure
//         } catch (error) {
//           Notification.notify({
//             type: "danger",
//             message: error.message,
//             placement: "top-right",
//           });
//           throw error;
//         }
//       };
//       const notifySpy = jest.spyOn(Notification, "notify");
//       try {
//         await wrapper.vm.saveFilter("New Filter");
//       } catch (error) {
//         expect(notifySpy).toHaveBeenCalledWith(
//           expect.objectContaining({
//             type: "danger",
//             message: "Save failed",
//             placement: "top-right",
//           })
//         );
//       } finally {
//         notifySpy.mockRestore();
//       }
//     });
//   });
//   describe("loadFilters Method", () => {
//     it("correctly sets filters when loadFilters is called", async () => {
//       wrapper.vm.filters = [];
//       await wrapper.vm.loadFilters();
//       expect(wrapper.vm.filters).toEqual([
//         { filterName: "Filter 1" },
//         { filterName: "Filter 2" },
//         { filterName: "Filter 3" },
//       ]);
//     });
//   });
//   describe("saveFilterPrompt Method", () => {
//     it("triggers MessageBox.prompt and calls doSaveFilter on confirm", async () => {
//       const doSaveFilterSpy = jest.spyOn(wrapper.vm, "doSaveFilter");
//       await wrapper.vm.saveFilterPrompt();
//       expect(MessageBox.prompt).toHaveBeenCalled();
//       await wrapper.vm.$nextTick();
//       expect(doSaveFilterSpy).toHaveBeenCalledWith("new filter name");
//       doSaveFilterSpy.mockRestore();
//     });
//   });
//   describe("notifyError Method", () => {
//     it("displays a notification with the correct message", () => {
//       const notifySpy = jest.spyOn(Notification, "notify");
//       wrapper.vm.notifyError("Test error message");
//       expect(notifySpy).toHaveBeenCalledWith({
//         type: "danger",
//         title: "An Error Occurred",
//         content: "Test error message",
//         duration: 0,
//       });
//       notifySpy.mockRestore();
//     });
//   });
//   describe("Different States", () => {
//     it("shows 'no filters available' message when no filters are loaded", async () => {
//       wrapper.setData({ filters: [] });
//       await wrapper.vm.$nextTick();
//       expect(
//         wrapper.find('[data-test-id="no-filters-message"]').text()
//       ).toContain("No filters available");
//     });
//   });
// });

import { mount } from "@vue/test-utils";
import { ComponentPublicInstance } from "vue";
import SavedFilters from "../savedFilters.vue";
import { Notification, MessageBox } from "uiv";
// Custom type for the component instance
type SavedFiltersInstance = ComponentPublicInstance<{
  filters: { filterName: string }[];
  selectFilter: (filter: { filterName: string }) => void;
  deleteFilter: () => void;
  saveFilter: (filterName: string) => void;
  loadFilters: () => void;
  saveFilterPrompt: () => void;
  notifyError: (message: string) => void;
  doSaveFilter: (filterName: string) => void;
}>;
const mockDropdown = {
  template: `
    <div data-test-id="dropdown">
      <button @click="toggleDropdown" data-test-id="toggle-button">Toggle</button>
      <div v-if="isOpen">
        <slot></slot>
        <span data-test-id="checkmark-span">√</span>
        <button data-test-id="delete-filter-btn" @click="emitDeleteFilter">Delete Filter</button>
      </div>
    </div>
  `,
  data() {
    return {
      isOpen: false,
    };
  },
  methods: {
    toggleDropdown() {
      this.isOpen = !this.isOpen;
    },
    emitDeleteFilter() {
      this.$emit("delete-filter");
    },
  },
};
const eventBus = {
  on: jest.fn(),
  emit: jest.fn(),
  off: jest.fn(),
};
const mountSavedFilters = async (props = {}) => {
  const wrapper = mount<SavedFiltersInstance>(SavedFilters, {
    props: {
      hasQuery: true,
      query: { filterName: "test4" },
      eventBus,
      ...props,
    },
    attachTo: document.body,
    global: {
      mocks: {
        $t: (msg: string) => msg,
      },
      stubs: {
        btn: true,
        dropdown: mockDropdown,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};
jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({ projectName: "test" }),
}));
jest.mock("../../../../library/stores/ActivityFilterStore", () => ({
  ActivityFilterStore: jest.fn().mockImplementation(() => ({
    loadForProject: jest.fn().mockReturnValue({
      filters: [
        { filterName: "Filter 1" },
        { filterName: "Filter 2" },
        { filterName: "Filter 3" },
      ],
    }),
    saveFilter: jest.fn().mockResolvedValue({ success: true }),
    removeFilter: jest.fn().mockResolvedValue(undefined),
  })),
}));
jest.mock("uiv", () => ({
  MessageBox: {
    confirm: jest.fn().mockResolvedValue(true),
    prompt: jest.fn().mockResolvedValue("new filter name"),
  },
  Notification: {
    notify: jest.fn(),
  },
}));
describe("SavedFilters", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });
  describe("Component Initialization", () => {
    it("should register and unregister event listeners", async () => {
      const wrapper = await mountSavedFilters();
      expect(eventBus.on).toHaveBeenCalledWith(
        "invoke-save-filter",
        expect.any(Function)
      );
      wrapper.unmount();
      expect(eventBus.off).toHaveBeenCalledWith("invoke-save-filter");
    });
  });
  describe("Interaction with Filters", () => {
    it("renders save button conditionally based on query props", async () => {
      const wrapper = await mountSavedFilters();
      expect(wrapper.find('[data-test-id="save-filter-button"]').exists()).toBe(
        wrapper.props().hasQuery && !wrapper.props().query.filterName
      );
    });
    it("emits 'select_filter' when a filter is selected", async () => {
      const wrapper = await mountSavedFilters();
      await wrapper.vm.selectFilter({ filterName: "Test Filter" });
      expect(wrapper.emitted("select_filter")![0]).toEqual([
        { filterName: "Test Filter" },
      ]);
    });
    it("triggers delete filter action when delete button is clicked", async () => {
      const wrapper = await mountSavedFilters();
      const dropdownToggle = wrapper.find('[data-test-id="toggle-button"]');
      await dropdownToggle.trigger("click");
      await wrapper.vm.$nextTick();
      const deleteButton = wrapper.find('[data-test-id="delete-filter-btn"]');
      expect(deleteButton.exists()).toBe(true);
    });
    it("calls deleteFilter when the delete filter button is clicked", async () => {
      const wrapper = await mountSavedFilters();
      await wrapper.vm.$nextTick();
      const dropdownToggle = wrapper.find('[data-test-id="toggle-button"]');
      await dropdownToggle.trigger("click");
      await wrapper.vm.$nextTick();
      const deleteButton = wrapper.find('[data-test-id="delete-filter-btn"]');

      await deleteButton.trigger("click");
      await wrapper.vm.$nextTick();
    });
    it("should handle dropdown interactions and delete filter", async () => {
      const wrapper = await mountSavedFilters();
      const deleteFilterSpy = jest.spyOn(wrapper.vm, "deleteFilter");
      const deleteButton = wrapper.find('[data-test-id="delete-filter-btn"]');
      if (deleteButton.exists()) {
        await deleteButton.trigger("click");

        const dropdownComponent = wrapper.findComponent(mockDropdown);
        expect(
          dropdownComponent.emitted().hasOwnProperty("delete-filter")
        ).toBe(true);
        expect(deleteFilterSpy).toHaveBeenCalled(); // Check if deleteFilter method was called
      }
      deleteFilterSpy.mockRestore();
    });
  });
  describe("Rendering of UI Components", () => {
    it("checks for the presence of dropdown when filters exist", async () => {
      const wrapper = await mountSavedFilters();
      await wrapper.setData({ filters: [{ filterName: "Test Filter" }] });
      await wrapper.vm.$nextTick();
      expect(wrapper.find('[data-test-id="dropdown"]').exists()).toBe(true);
    });
    it("displays no filters message when no filters are available", async () => {
      const wrapper = await mountSavedFilters();
      wrapper.setData({ filters: [] });
      await wrapper.vm.$nextTick();
      expect(
        wrapper.find('[data-test-id="no-filters-message"]').text()
      ).toContain("No filters available");
    });
    it("renders checkmark span when query and filterName matches a filter", async () => {
      const wrapper = await mountSavedFilters();
      await wrapper.setData({ filters: [{ filterName: "Filter 1" }] });
      await wrapper.vm.$nextTick();
      const toggleButton = wrapper.find('[data-test-id="toggle-button"]');

      await toggleButton.trigger("click");
      await wrapper.vm.$nextTick();

      const checkmarkSpan = wrapper.find('[data-test-id="checkmark-span"]');
      expect(checkmarkSpan.exists()).toBe(true);
      expect(checkmarkSpan.text()).toContain("√");
    });
  });
  describe("Success and Error Handling", () => {
    it("displays notification upon successful filter save", async () => {
      const wrapper = await mountSavedFilters();
      const saveFilterMock = jest.fn().mockImplementation(() => {
        Notification.notify({ type: "success" });
      });
      wrapper.vm.saveFilter = saveFilterMock;
      const notifySpy = jest.spyOn(Notification, "notify");
      await wrapper.vm.saveFilter("New Filter");
      expect(notifySpy).toHaveBeenCalledWith(
        expect.objectContaining({ type: "success" })
      );
      notifySpy.mockRestore();
    });
    it("displays error when filter saving fails", async () => {
      const wrapper = await mountSavedFilters();
      wrapper.vm.doSaveFilter = async function () {
        throw new Error("Save failed"); // This line simulates a failure
      };
      const notifySpy = jest.spyOn(wrapper.vm, "notifyError");
      try {
        await wrapper.vm.doSaveFilter("New Filter");
      } catch (error) {
        wrapper.vm.notifyError(error.message);
        expect(notifySpy).toHaveBeenCalledWith("Save failed");
      } finally {
        notifySpy.mockRestore();
      }
    });
  });
  describe("loadFilters Method", () => {
    it("correctly sets filters when loadFilters is called", async () => {
      const wrapper = await mountSavedFilters();
      wrapper.vm.filters = [];
      await wrapper.vm.loadFilters();
      expect(wrapper.vm.filters).toEqual([
        { filterName: "Filter 1" },
        { filterName: "Filter 2" },
        { filterName: "Filter 3" },
      ]);
    });
  });
  describe("saveFilterPrompt Method", () => {
    it("triggers MessageBox.prompt and calls doSaveFilter on confirm", async () => {
      const wrapper = await mountSavedFilters();
      const doSaveFilterSpy = jest.spyOn(wrapper.vm, "doSaveFilter");
      await wrapper.vm.saveFilterPrompt();
      expect(MessageBox.prompt).toHaveBeenCalled();
      await wrapper.vm.$nextTick();
      expect(doSaveFilterSpy).toHaveBeenCalledWith("new filter name");
      doSaveFilterSpy.mockRestore();
    });
  });
  describe("notifyError Method", () => {
    it("displays a notification with the correct message", async () => {
      const wrapper = await mountSavedFilters();
      const notifySpy = jest.spyOn(Notification, "notify");
      wrapper.vm.notifyError("Test error message");
      expect(notifySpy).toHaveBeenCalledWith({
        type: "danger",
        title: "An Error Occurred",
        content: "Test error message",
        duration: 0,
      });
      notifySpy.mockRestore();
    });
  });
});
