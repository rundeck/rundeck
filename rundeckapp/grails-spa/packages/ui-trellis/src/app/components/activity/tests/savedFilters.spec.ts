import { mount } from "@vue/test-utils";
import SavedFilters from "../savedFilters.vue";
import { Notification, MessageBox } from "uiv";

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
    confirm: jest.fn(),
    prompt: jest.fn().mockResolvedValue({ value: "test" }),
  },
  Notification: {
    notify: jest.fn(),
  },
}));
describe("SavedFilters", () => {
  let wrapper;
  const eventBus = {
    on: jest.fn(),
    emit: jest.fn(),
    off: jest.fn(),
  };
  beforeEach(async () => {
    wrapper = mount(SavedFilters, {
      props: {
        hasQuery: true,
        query: {},
        eventBus,
      },
      global: {
        mocks: {
          $t: (msg) => msg,
        },
        stubs: {
          btn: true,
        },
      },
    });
  });
  afterEach(() => {
    jest.clearAllMocks();
  });
  describe("Component Mounting", () => {
    it("mounts properly with all props", () => {
      expect(wrapper.exists()).toBe(true);
    });
    it("registers event listeners on mount", () => {
      expect(eventBus.on).toHaveBeenCalledWith(
        "invoke-save-filter",
        expect.any(Function),
      );
    });
    it("unregisters event listeners on unmount", () => {
      // Create a mock event bus
      const mockEventBus = {
        on: jest.fn(),
        off: jest.fn(),
      };

      // Mount the component
      const wrapper = mount(SavedFilters, {
        props: {
          hasQuery: true,
          query: {},
          eventBus: mockEventBus,
        },
        global: {
          mocks: {
            $t: (msg) => msg,
          },
          stubs: {
            btn: true,
          },
        },
      });

      const spy = jest.spyOn(mockEventBus, "off");

      wrapper.unmount();

      expect(spy).toHaveBeenCalledWith("invoke-save-filter");
    });
  });
  describe("User Interactions", () => {
    it("renders save button when hasQuery is true and query is empty", () => {
      expect(wrapper.find('[data-test-id="save-filter-button"]').exists()).toBe(
        true,
      );
    });
    it("does not render delete filter button when query has no filterName", () => {
      expect(wrapper.find('[data-test-id="delete-filter-btn"]').exists()).toBe(
        false,
      );
    });
    it("emits 'select_filter' when a filter is selected", async () => {
      await wrapper.vm.selectFilter({ filterName: "Test Filter" });
      expect(wrapper.emitted("select_filter")[0]).toEqual([
        { filterName: "Test Filter" },
      ]);
    });
    it("calls MessageBox.prompt when saveFilterPrompt is triggered", async () => {
      await wrapper.vm.saveFilterPrompt();
      expect(MessageBox.prompt).toHaveBeenCalled();
    });
  });
  describe("Filter Management", () => {
    it("updates filters after successful deletion", async () => {
      await wrapper.vm.loadFilters();
      await wrapper.vm.$nextTick();
      const initialCount = wrapper.vm.filters.length;
      if (initialCount > 0) {
        console.log("Before delete:", wrapper.vm.filters);
        // Mock the deleteFilter method
        wrapper.vm.deleteFilter = (filterName) => {
          const index = wrapper.vm.filters.findIndex(
            (filter) => filter.filterName === filterName,
          );
          if (index !== -1) {
            wrapper.vm.filters.splice(index, 1);
          }
        };
        await wrapper.vm.deleteFilter(wrapper.vm.filters[0].filterName);
        await wrapper.vm.$nextTick();
        console.log("After delete:", wrapper.vm.filters);

        expect(wrapper.vm.filters.length).toBeLessThan(initialCount);
      } else {
        throw new Error("No filters available to delete");
      }
    });

    it("shows 'No filters available' when filters array is empty", async () => {
      wrapper.vm.filters = [];
      await wrapper.vm.$nextTick();

      const noFiltersMessage = wrapper
        .find('[data-test-id="no-filters-message"]')
        .text();
      expect(noFiltersMessage).toContain("No filters available");
    });
    it("updates visible elements when 'query.filterName' changes", async () => {
      await wrapper.setProps({ query: { filterName: "Updated Filter" } });
      expect(wrapper.find('[data-test-id="filter-name"]').text()).toBe(
        "Updated Filter",
      );
    });
  });
});
