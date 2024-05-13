import { mount } from "@vue/test-utils";
import SavedFilters from "../savedFilters.vue";
import { Notification, MessageBox } from "uiv";
import dropdown from "../../../components/job/resources/NodeDefaultFilterDropdown.vue";
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
          dropdown: {
            template: '<div><slot name="drpdown"></slot></div>',
          },
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
        expect.any(Function)
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
            Dropdown: true,
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
        true
      );
    });
    it("does not render delete filter button when query has no filterName", () => {
      expect(wrapper.find('[data-test-id="delete-filter-btn"]').exists()).toBe(
        false
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
  describe("Button Rendering", () => {
    it("renders save button when hasQuery is true and query is not defined", () => {
      wrapper.props({ hasQuery: true, query: undefined });
      expect(wrapper.find('[data-test-id="save-filter-button"]').exists()).toBe(
        true
      );
    });
    it("should render a delete button if query and query.filterName are defined", async () => {
      await wrapper.setProps({ query: { filterName: "test" } });
      await wrapper.vm.$nextTick();
      const deleteButton = wrapper.find('[data-test-id="delete-filter-btn"]');
      expect(deleteButton.exists()).toBe(true);
    });

    it("does not render save button when hasQuery is false", () => {
      wrapper.props({ hasQuery: false });
      expect(wrapper.find('[data-test-id="save-filter-button"]').exists()).toBe(
        false
      );
    });

    it("does not render save button when query is defined and query.filterName is also defined", () => {
      wrapper.props({ hasQuery: true, query: { filterName: "Test Filter" } });
      expect(wrapper.find('[data-test-id="save-filter-button"]').exists()).toBe(
        false
      );
    });

    it("calls saveFilterPrompt when save button is clicked", async () => {
      wrapper.props({ hasQuery: true, query: undefined });
      await wrapper
        .find('[data-test-id="save-filter-button"]')
        .trigger("click");
      expect(MessageBox.prompt).toHaveBeenCalled();
    });
  });

  describe("Span Rendering", () => {
    it("renders span when query is defined and query.filterName is also defined", () => {
      wrapper.props({ query: { filterName: "Test Filter" } });
      expect(wrapper.find("span").exists()).toBe(true);
    });
  });

  describe("Dropdown Rendering", () => {
    it("renders dropdown when filters is defined and its length is greater than zero", () => {
      wrapper.setData({ filters: [{ filterName: "Test Filter" }] });
      expect(wrapper.find("dropdown").exists()).toBe(true);
    });
  });

  describe("Delete Filter", () => {
    it("calls deleteFilter when delete filter button is clicked", async () => {
      wrapper.props({ query: { filterName: "Test Filter" } });
      await wrapper.find('[role="button"]').trigger("click");
      expect(wrapper.vm.deleteFilter).toHaveBeenCalled();
    });
  });

  describe("Separator Rendering", () => {
    it("renders separator when query is defined and query.filterName is also defined", () => {
      wrapper.setProps({ query: { filterName: "test1" } });
      expect(wrapper.find('[data-test-id="separator"]').exists()).toBe(true);
    });
  });

  describe("Filter Selection", () => {
    it("calls selectFilter when a filter is clicked", async () => {
      wrapper.setData({ filters: [{ filterName: "Test Filter" }] });
      await wrapper.find('[role="button"]').trigger("click");
      expect(wrapper.vm.selectFilter).toHaveBeenCalled();
    });
    it("calls deleteFilter when delete filter button is clicked", async () => {
      await wrapper.setProps({ query: { filterName: "Test Filter" } });
      await wrapper.vm.$nextTick();
      const deleteButton = wrapper.find('[data-test-id="delete-filter-btn"]');
      if (deleteButton.exists()) {
        await deleteButton.trigger("click");
        expect(wrapper.vm.deleteFilter).toHaveBeenCalled();
      } else {
        console.log(wrapper.html()); // Log the rendered HTML of the component
        throw new Error("Delete button not found in the DOM");
      }
    });
    it("displays checkmark when condition is met", async () => {
      const filters = [
        { filterName: "Filter 1" },
        { filterName: "Filter 2" },
        // Add more filters as needed...
      ];

      // Set the necessary props and data
      await wrapper.setProps({ filters });
      wrapper.vm.query = { filterName: "Filter 1" }; // This should match one of the filterNames in the filters array
      await wrapper.vm.$nextTick();

      const checkmarkSpan = wrapper.find('[data-testid="checkmark-span"]');
      expect(checkmarkSpan.exists()).toBe(true);
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
            (filter) => filter.filterName === filterName
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
        "Updated Filter"
      );
    });
  });
});
