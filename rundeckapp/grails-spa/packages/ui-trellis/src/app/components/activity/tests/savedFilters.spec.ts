import { mount, VueWrapper } from "@vue/test-utils";
import SavedFilters from "../savedFilters.vue";
import { ActivityFilterStore } from "../../../../library/stores/ActivityFilterStore";
import { MessageBox } from "uiv";

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
    prompt: jest.fn().mockResolvedValue({ value: "test" }),
  },
  Notification: {
    notify: jest.fn(),
  },
}));
const mountSavedFilters = (
  propsData = {},
  filterStoreOverrides = {}
): VueWrapper<any> => {
  const filterStore = {
    ...new ActivityFilterStore(),
    ...filterStoreOverrides,
  };
  return mount(SavedFilters, {
    props: {
      hasQuery: true,
      query: {},
      eventBus: {
        on: jest.fn(),
        emit: jest.fn(),
        off: jest.fn(),
      },
      ...propsData,
    },
    global: {
      mocks: {
        $t: (msg) => msg,
        filterStore,
      },
      stubs: {
        btn: true,
        dropdown: {
          template: '<div class="dropdown"><slot name="dropdown"></slot></div>',
        },
      },
    },
  });
};
describe("SavedFilters", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });
  describe("Component Mounting", () => {
    it("registers event listeners on mount", () => {
      const wrapper = mountSavedFilters();
      expect(wrapper.props().eventBus.on).toHaveBeenCalledWith(
        "invoke-save-filter",
        expect.any(Function)
      );
    });
    it("unregisters event listeners on unmount", () => {
      const wrapper = mountSavedFilters();
      wrapper.unmount();
      expect(wrapper.props().eventBus.off).toHaveBeenCalledWith(
        "invoke-save-filter"
      );
    });
  });
  describe("User Interactions", () => {
    it("renders save button when hasQuery is true and query is empty", () => {
      const wrapper = mountSavedFilters();
      expect(wrapper.find('[data-test-id="save-filter-button"]').exists()).toBe(
        true
      );
    });
    it("does not render delete filter button when query has no filterName", () => {
      const wrapper = mountSavedFilters();
      expect(wrapper.find('[data-test-id="delete-filter-btn"]').exists()).toBe(
        false
      );
    });
    it("emits 'select_filter' when a filter is selected", async () => {
      const wrapper = mountSavedFilters();
      await wrapper.vm.$nextTick();
      const filterItem = wrapper.find('[data-test="filter-link"]');
      await filterItem.trigger("click");
      expect(wrapper.emitted().select_filter).toBeTruthy();
      expect(wrapper.emitted().select_filter[0]).toEqual([
        { filterName: "Filter 1" },
      ]);
    });
    it("calls MessageBox.prompt when saveFilterPrompt is triggered", async () => {
      const wrapper = mountSavedFilters();
      const saveFilterButton = wrapper.find(
        '[data-test-id="save-filter-button"]'
      );
      await saveFilterButton.trigger("click");
      expect(MessageBox.prompt).toHaveBeenCalled();
    });
  });
  describe("Filter Management", () => {
    it("does not call removeFilter and filters remain the same when filterName is not defined", async () => {
      const wrapper = mountSavedFilters();
      wrapper.vm.filterStore.removeFilter = jest
        .fn()
        .mockResolvedValue({ success: true });
      const removeFilterSpy = jest.spyOn(
        wrapper.vm.filterStore,
        "removeFilter"
      );
      await wrapper.vm.loadFilters();
      await wrapper.vm.$nextTick();
      const initialCount = wrapper.findAll(
        '[data-test-id="filter-item"]'
      ).length;
      expect(initialCount).toBeGreaterThan(0);
      const deleteButton = wrapper.find('[data-test-id="delete-filter-btn"]');
      if (deleteButton.exists()) {
        await deleteButton.trigger("click");
        await wrapper.vm.$nextTick();
        const finalCount = wrapper.findAll(
          '[data-test-id="filter-item"]'
        ).length;
        expect(removeFilterSpy).not.toHaveBeenCalled();
        expect(finalCount).toBe(initialCount);
      }
    });
    it("shows an empty dropdown when filters array is empty", async () => {
      const wrapper = mountSavedFilters({ filters: [] });
      const dropdownItems = wrapper.findAll('[data-test-id="filter-item"]');
      expect(dropdownItems.length).toBe(0);
    });
    it("updates visible elements when 'query.filterName' changes", async () => {
      const wrapper = mountSavedFilters({
        query: { filterName: "Updated Filter" },
      });
      expect(wrapper.find('[data-test-id="filter-name"]').text()).toBe(
        "Updated Filter"
      );
    });

    it("updates filters after successful deletion", async () => {
      const wrapper = mountSavedFilters();
      const initialCount = wrapper.findAll(
        '[data-test-id="filter-item"]',
      ).length;
      // Mock the $confirm to return a resolved promise
      wrapper.vm.$confirm = () => Promise.resolve();
      // Mock ActivityFilterStore.removeFilter
      ActivityFilterStore.prototype.removeFilter = jest
        .fn()
        .mockImplementation((filterName) => {
          const index = wrapper.vm.filters.findIndex(
            (filter) => filter.filterName === filterName
          );
          if (index !== -1) {
            wrapper.vm.filters.splice(index, 1);
          }
        });
      const firstFilterDeleteButton = wrapper.find(
        '[data-test-id="delete-filter-btn"]'
      );
      await firstFilterDeleteButton.trigger("click");
      await wrapper.vm.$nextTick();
      const finalCount = wrapper.findAll('[data-test-id="filter-item"]').length;
      expect(finalCount).toBeLessThan(initialCount);
    });
  });
});
