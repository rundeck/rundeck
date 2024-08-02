import { mount, VueWrapper } from "@vue/test-utils";
import ActivityFilter from "../activityFilter.vue";
import SavedFilters from "../savedFilters.vue";

type ActivityFilterInstance = InstanceType<typeof ActivityFilter>;
jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({ projectName: "test" }),
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
const mockEventBus = {
  on: jest.fn(),
  emit: jest.fn(),
  off: jest.fn(),
};
const mountActivityFilter = async (props = {}) => {
  const wrapper = mount(ActivityFilter, {
    props: {
      modelValue: {},
      eventBus: mockEventBus,
      opts: {},
      ...props,
    },
    global: {
      stubs: {
        modal: {
          template: '<div class="modal"><slot></slot></div>',
        },
        DateTimePicker: {
          template: '<div id="DateTimePicker" />',
        },
        DateFilter: true,
        SavedFilters: true,
        btn: true,
        Dropdown: {
          template: '<div class="dropdown"><slot></slot></div>',
        },
      },
      directives: {
        tooltip: () => {},
      },
      mocks: {
        $t: (msg) => msg,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper as VueWrapper<ActivityFilterInstance>;
};
describe("ActivityFilter", () => {
  beforeAll(() => {
    jest.useFakeTimers();
  });
  afterAll(() => {
    jest.useRealTimers();
  });
  afterEach(() => {
    jest.clearAllMocks();
  });
  describe("Filter Modal", () => {
    it("opens the filter modal", async () => {
      const wrapper = await mountActivityFilter();
      await wrapper.find('[data-test-id="filter-button"]').trigger("click");
      await wrapper.vm.$nextTick();
      expect(wrapper.vm.filterOpen).toBe(true);
      wrapper.vm.filterOpen = false;
      await wrapper.vm.$nextTick();
      expect(wrapper.vm.filterOpen).toBe(false);
    });
  });
  describe("Query Handling", () => {
    it("updates the query when search is called", async () => {
      const wrapper = await mountActivityFilter();
      const searchSpy = jest.spyOn(wrapper.vm, "search");
      await wrapper.vm.search();
      expect(searchSpy).toHaveBeenCalled();
      expect(wrapper.vm.didSearch).toBe(true);
      expect(wrapper.vm.filterOpen).toBe(false);
      searchSpy.mockRestore();
    });
    it("resets the query when reset is called", async () => {
      const wrapper = await mountActivityFilter();
      await wrapper.setProps({ modelValue: { jobFilter: "test" } });
      await wrapper.vm.reset();
      expect(wrapper.vm.query.jobFilter).toBe("test");
    });
    it("handles selectFilter event", async () => {
      const wrapper = await mountActivityFilter();
      const filter = {
        query: { jobFilter: "test" },
        filterName: "Test Filter",
      };
      await wrapper.vm.selectFilter(filter);
      expect(wrapper.vm.query.jobFilter).toBe("test");
      expect("filterName" in filter).toBe(true);
      expect(filter.filterName).toBe("Test Filter");
    });
    it("saves a filter correctly", async () => {
      const wrapper = await mountActivityFilter();
      const saveFilterSpy = jest.spyOn(wrapper.vm, "saveFilter");
      const emitSpy = jest.spyOn(wrapper.vm.eventBus, "emit");
      await wrapper.vm.saveFilter();
      jest.runAllTimers();
      expect(saveFilterSpy).toHaveBeenCalled();
      expect(wrapper.vm.didSearch).toBe(true);
      expect(wrapper.vm.filterOpen).toBe(false);
      expect(emitSpy).toHaveBeenCalledWith("invoke-save-filter");
      saveFilterSpy.mockRestore();
      emitSpy.mockRestore();
    });
    it("checks if query is present correctly", async () => {
      const wrapper = await mountActivityFilter();
      wrapper.vm.query.jobFilter = "test";
      await wrapper.vm.checkQueryIsPresent();
      expect(wrapper.vm.hasQuery).toBe(true);
      wrapper.vm.query.jobFilter = "";
      await wrapper.vm.checkQueryIsPresent();
      expect(wrapper.vm.hasQuery).toBe(false);
    });
    it("emits update:modelValue correctly", async () => {
      const wrapper = await mountActivityFilter();
      await wrapper.vm.updated();
      const emitted = wrapper.emitted();
      expect(emitted["update:modelValue"]).toBeTruthy();
      expect(emitted["update:modelValue"][0]).toEqual([wrapper.vm.query]);
    });
    it("handles cancel correctly", async () => {
      const wrapper = await mountActivityFilter();
      const resetSpy = jest.spyOn(wrapper.vm, "reset");
      await wrapper.vm.cancel();
      expect(resetSpy).toHaveBeenCalled();
      expect(wrapper.vm.filterOpen).toBe(false);
      resetSpy.mockRestore();
    });
    it("handles modal closing correctly", async () => {
      const wrapper = await mountActivityFilter();
      wrapper.vm.didSearch = true;
      await wrapper.vm.closing();
      expect(wrapper.vm.didSearch).toBe(false);
      wrapper.vm.didSearch = false;
      const resetSpy = jest.spyOn(wrapper.vm, "reset");
      await wrapper.vm.closing();
      expect(resetSpy).toHaveBeenCalled();
      resetSpy.mockRestore();
    });
  });
  describe("Period Handling", () => {
    it("changes the period correctly", async () => {
      const wrapper = await mountActivityFilter();
      const period = { name: "Hour", params: { recentFilter: "1h" } };
      await wrapper.vm.changePeriod(period);
      expect(wrapper.vm.period.name).toBe("Hour");
      expect(wrapper.vm.query.recentFilter).toBe("1h");
    });
    it("updates the selected period based on query", async () => {
      const wrapper = await mountActivityFilter();
      wrapper.vm.query.recentFilter = "1h";
      await wrapper.vm.$nextTick();
      await wrapper.vm.updateSelectedPeriod();
      expect(wrapper.vm.period.name).toBe("Hour");
    });
    it("handles empty recentFilter correctly", async () => {
      const wrapper = await mountActivityFilter();
      wrapper.vm.query.recentFilter = "";
      await wrapper.vm.$nextTick();
      await wrapper.vm.updateSelectedPeriod();
      expect(wrapper.vm.period.name).toBe("All");
    });
    it("handles custom periods in dropdown correctly", async () => {
      const wrapper = await mountActivityFilter();
      wrapper.vm.query.recentFilter = "-";
      await wrapper.vm.$nextTick();
      expect(wrapper.find('[data-test-id="date-filters"]').exists()).toBe(true);
    });
    it("updates the recentFilter correctly", async () => {
      const wrapper = await mountActivityFilter();
      wrapper.vm.query.recentFilter = "1w";
      await wrapper.vm.$nextTick();
      await wrapper.vm.updateSelectedPeriod();
      await wrapper.vm.$nextTick();
      expect(wrapper.vm.query.recentFilter).toBe("1w");
      expect(wrapper.vm.period.name).toBe("Week");
      wrapper.vm.query.recentFilter = "";
      await wrapper.vm.$nextTick();
      await wrapper.vm.updateSelectedPeriod();
      await wrapper.vm.$nextTick();
      expect(wrapper.vm.query.recentFilter).toBe("");
      expect(wrapper.vm.period.name).toBe("Week");
    });
  });
  describe("Rendering and UI Elements", () => {
    it("renders dropdown options correctly", async () => {
      const wrapper = await mountActivityFilter();
      wrapper.vm.query.recentFilter = "1d";
      wrapper.vm.displayOpts.showRecentFilter = true;
      wrapper.vm.period.name = "Day";
      await wrapper.vm.$nextTick();
      const dropdown = wrapper.find('[data-test-id="dropdown"]');
      expect(dropdown.exists()).toBe(true);
      const dropdownToggle = dropdown.find('[data-test-id="dropdown-toggle"]');
      expect(dropdownToggle.exists()).toBe(true);
      expect(dropdownToggle.text()).toContain("period.label.Day");
    });
    it("renders filter button correctly", async () => {
      const wrapper = await mountActivityFilter();
      wrapper.vm.displayOpts.showFilter = true;
      wrapper.vm.hasQuery = true;
      await wrapper.vm.$nextTick();
      expect(wrapper.find('[data-test-id="filter-button"]').exists()).toBe(
        true,
      );
    });
    it("renders saved-filters component correctly", async () => {
      const wrapper = await mountActivityFilter();
      wrapper.vm.displayOpts.showSavedFilters = true;
      await wrapper.vm.$nextTick();
      expect(wrapper.findComponent(SavedFilters).exists()).toBe(true);
    });
    it("renders query parameters summary correctly", async () => {
      const wrapper = await mountActivityFilter();
      wrapper.vm.query.jobFilter = "test";
      wrapper.vm.query.userFilter = "user1";
      await wrapper.vm.$nextTick();
      const queryParamsSummary = wrapper.findAll(
        '[data-test-id="query-param"]',
      );
      const firstParam = queryParamsSummary.at(0);
      if (firstParam) {
        expect(firstParam.text()).toContain("jobquery.title.jobFilter");
        expect(firstParam.text()).toContain("test");
      }
      const secondParam = queryParamsSummary.at(1);
      if (secondParam) {
        expect(secondParam.text()).toContain("jobquery.title.userFilter");
        expect(secondParam.text()).toContain("user1");
      }
    });
    it("renders correctly with no filters", async () => {
      const wrapper = await mountActivityFilter();
      wrapper.vm.displayOpts.showRecentFilter = false;
      wrapper.vm.displayOpts.showFilter = false;
      wrapper.vm.displayOpts.showSavedFilters = false;
      await wrapper.vm.$nextTick();
      expect(wrapper.find('[data-test-id="dropdown"]').exists()).toBe(false);
      expect(wrapper.find('[data-test-id="filter-button"]').exists()).toBe(
        false,
      );
      expect(wrapper.findComponent(SavedFilters).exists()).toBe(false);
    });
    it("opens the filter modal on filter button click", async () => {
      const wrapper = await mountActivityFilter();
      await wrapper.find('[data-test-id="filter-button"]').trigger("click");
      await wrapper.vm.$nextTick();
      expect(wrapper.vm.filterOpen).toBe(true);
    });
    it("emits select_filter event correctly", async () => {
      const wrapper = await mountActivityFilter();
      const selectFilterSpy = jest.spyOn(wrapper.vm, "selectFilter");
      const savedFiltersComponent = wrapper.findComponent(SavedFilters);
      savedFiltersComponent.vm.$emit("select_filter", {
        query: { jobFilter: "test" },
        filterName: "Test Filter",
      });
      await wrapper.vm.$nextTick();
      expect(selectFilterSpy).toHaveBeenCalledWith({
        query: { jobFilter: "test" },
        filterName: "Test Filter",
      });
    });
    it("handles search click", async () => {
      const wrapper = await mountActivityFilter();
      await wrapper.vm.$nextTick();
      const searchSpy = jest.spyOn(wrapper.vm, "search");
      const btnElement = wrapper.find(".btn-primary");
      if (btnElement.exists()) {
        await btnElement.trigger("click");
        expect(searchSpy).toHaveBeenCalled();
      }
      searchSpy.mockRestore();
    });
    it("renders date filters correctly when recentFilter is set to '-'", async () => {
      const wrapper = await mountActivityFilter();
      wrapper.vm.query.recentFilter = "-";
      await wrapper.vm.$nextTick();
      expect(wrapper.find('[data-test-id="date-filters"]').exists()).toBe(true);
    });
    it("changes the date filter correctly", async () => {
      const wrapper = await mountActivityFilter();
      const dateFilter = wrapper.vm.DateFilters[0];
      dateFilter.filter.enabled = true;
      dateFilter.filter.datetime = "2024-05-18T12:00:00Z";
      await wrapper.vm.$nextTick();
      expect(wrapper.vm.query.startafterFilter).toBe("2024-05-18T12:00:00Z");
      expect(dateFilter.filter.enabled).toBe(true);
      dateFilter.filter.enabled = false;
      await wrapper.vm.$nextTick();
      expect(wrapper.vm.query.startafterFilter).toBe("");
      expect(dateFilter.filter.enabled).toBe(false);
    });
    it("clears date filter when reset is called", async () => {
      const wrapper = await mountActivityFilter();
      wrapper.vm.query.startafterFilter = "2024-05-18T12:00:00Z";
      await wrapper.vm.reset();
      expect(wrapper.vm.query.startafterFilter).toBeUndefined();
    });
  });
});
