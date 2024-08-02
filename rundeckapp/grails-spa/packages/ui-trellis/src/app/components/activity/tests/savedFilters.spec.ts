import { mount, VueWrapper } from "@vue/test-utils";
import SavedFilters from "../savedFilters.vue";
import { ActivityFilterStore } from "../../../../library/stores/ActivityFilterStore";
import { MessageBox } from "uiv";

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({ projectName: "test" }),
}));

jest.mock("uiv", () => ({
  MessageBox: {
    confirm: jest.fn().mockResolvedValue(true),
    prompt: jest.fn().mockResolvedValue({ value: "test" }),
  },
}));
const originalFilters = [
  { filterName: "Filter 1" },
  { filterName: "Filter 2" },
  { filterName: "Filter 3" },
];

jest.mock("../../../../library/stores/ActivityFilterStore", () => {
  const originalModule = jest.requireActual(
    "../../../../library/stores/ActivityFilterStore",
  );
  return {
    __esModule: true,
    ...originalModule,
    ActivityFilterStore: jest.fn().mockImplementation(() => {
      return {
        filters: [...originalFilters],
        loadForProject: jest.fn().mockImplementation(() => {
          return { filters: [...originalFilters] };
        }),
        removeFilter: jest
          .fn()
          .mockImplementation((projectName: string, filterName: string) => {
            const index = originalFilters.findIndex(
              (filter) => filter.filterName === filterName,
            );
            if (index !== -1) {
              originalFilters.splice(index, 1);
            }
            return Promise.resolve();
          }),
        saveFilter: jest.fn().mockResolvedValue({}),
      };
    }),
  };
});

const mountSavedFilters = (
  props = {},
  filterStoreOverrides = {},
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
        all: new Map(),
      },
      ...props,
    },
    global: {
      mocks: {
        $t: (msg) => msg,
        filterStore,
        $confirm: jest.fn().mockResolvedValue(true),
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
  let originalRemoveFilter;
  beforeEach(() => {
    originalRemoveFilter = ActivityFilterStore.prototype.removeFilter;
    ActivityFilterStore.prototype.loadForProject = jest.fn().mockReturnValue({
      filters: [...originalFilters],
    });
  });
  afterEach(() => {
    jest.clearAllMocks();
    ActivityFilterStore.prototype.removeFilter = originalRemoveFilter;
  });
  describe("Component Mounting", () => {
    it("registers event listeners on mount", () => {
      const wrapper = mountSavedFilters();
      expect(wrapper.props().eventBus.on).toHaveBeenCalledWith(
        "invoke-save-filter",
        expect.any(Function),
      );
    });
    it("unregisters event listeners on unmount", () => {
      const wrapper = mountSavedFilters();
      wrapper.unmount();
      expect(wrapper.props().eventBus.off).toHaveBeenCalledWith(
        "invoke-save-filter",
      );
    });
  });
  describe("User Interactions", () => {
    it("renders save button when hasQuery is true and query is empty", () => {
      const wrapper = mountSavedFilters();
      expect(wrapper.find('[data-test-id="save-filter-button"]').exists()).toBe(
        true,
      );
    });
    it("does not render delete filter button when query has no filterName", () => {
      const wrapper = mountSavedFilters();
      expect(wrapper.find('[data-test-id="delete-filter-btn"]').exists()).toBe(
        false,
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
        '[data-test-id="save-filter-button"]',
      );
      await saveFilterButton.trigger("click");
      expect(MessageBox.prompt).toHaveBeenCalled();
    });
  });
  describe("Filter Management", () => {
    it("does not call removeFilter and filters remain the same when filterName is not defined", async () => {
      const wrapper = mountSavedFilters({
        hasQuery: true,
      });
      const removeFilterSpy = jest.spyOn(
        wrapper.vm.filterStore,
        "removeFilter",
      );
      await wrapper.vm.$nextTick();
      const initialCount = wrapper.findAll('[data-test="filter-item"]').length;
      expect(initialCount).toBeGreaterThan(0);
      const deleteButton = wrapper.find('[data-test-id="delete-filter-btn"]');
      expect(deleteButton.exists()).toBe(false);
      const finalCount = wrapper.findAll('[data-test="filter-item"]').length;
      expect(removeFilterSpy).not.toHaveBeenCalled();
      expect(finalCount).toBe(initialCount);
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
        "Updated Filter",
      );
    });
    it("updates filters after successful deletion", async () => {
      const wrapper = mountSavedFilters({
        hasQuery: true,
        query: { filterName: "Filter 1" },
      });
      await wrapper.vm.$nextTick();
      const initialCount = wrapper.findAll('[data-test="filter-item"]').length;
      await wrapper.find('[data-test-id="delete-filter-btn"]').trigger("click");
      await wrapper.vm.$nextTick();
      const finalCount = wrapper.findAll('[data-test="filter-item"]').length;
      expect(finalCount).toBe(initialCount - 1);
    });
  });
});
