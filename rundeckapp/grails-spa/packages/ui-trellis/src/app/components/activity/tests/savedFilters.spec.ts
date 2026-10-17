import { vi } from "vitest";
import { mount, VueWrapper } from "@vue/test-utils";
import SavedFilters from "../savedFilters.vue";
import { ActivityFilterStore } from "../../../../library/stores/ActivityFilterStore";
import { MessageBox } from "uiv";

vi.mock("@/library/rundeckService", () => ({
  getRundeckContext: vi.fn().mockReturnValue({ projectName: "test" }),
}));

vi.mock("uiv", () => ({
  MessageBox: {
    confirm: vi.fn().mockResolvedValue(true),
    prompt: vi.fn().mockResolvedValue({ value: "test" }),
  },
}));

const { originalFilters } = vi.hoisted(() => ({
  originalFilters: [
    { filterName: "Filter 1" },
    { filterName: "Filter 2" },
    { filterName: "Filter 3" },
  ],
}));

vi.mock("../../../../library/stores/ActivityFilterStore", async (importOriginal) => {
  const originalModule = await importOriginal<
    typeof import("../../../../library/stores/ActivityFilterStore")
  >();
  return {
    __esModule: true,
    ...originalModule,
    ActivityFilterStore: vi.fn(function ActivityFilterStore() {
      return {
        filters: [...originalFilters],
        loadForProject: vi.fn().mockImplementation(() => {
          return { filters: [...originalFilters] };
        }),
        removeFilter: vi.fn()
          .mockImplementation((projectName: string, filterName: string) => {
            const index = originalFilters.findIndex(
              (filter) => filter.filterName === filterName,
            );
            if (index !== -1) {
              originalFilters.splice(index, 1);
            }
            return Promise.resolve();
          }),
        saveFilter: vi.fn().mockResolvedValue({}),
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
        on: vi.fn(),
        emit: vi.fn(),
        off: vi.fn(),
        all: new Map(),
      },
      ...props,
    },
    global: {
      mocks: {
        filterStore,
        $confirm: vi.fn().mockResolvedValue(true),
      },
    },
  });
};
describe("SavedFilters", () => {
  let originalRemoveFilter: typeof ActivityFilterStore.prototype.removeFilter;
  beforeEach(() => {
    originalRemoveFilter = ActivityFilterStore.prototype.removeFilter;
    ActivityFilterStore.prototype.loadForProject = vi.fn().mockReturnValue({
      filters: [...originalFilters],
    });
  });
  afterEach(() => {
    vi.clearAllMocks();
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
      const removeFilterSpy = vi.spyOn(
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
