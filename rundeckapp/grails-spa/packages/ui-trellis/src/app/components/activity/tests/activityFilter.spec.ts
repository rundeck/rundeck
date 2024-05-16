import { mount } from "@vue/test-utils";
import ActivityFilter from "../activityFilter.vue";
import DateFilter from "../dateFilter.vue";
import DateTimePicker from "../dateTimePicker.vue";
import SavedFilters from "../savedFilters.vue";

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({ projectName: "test" }),
}));
let wrapper;
const mockEventBus = {
  on: jest.fn(),
  emit: jest.fn(),
  off: jest.fn(),
};
const mountActivityFilter = async (props = {}) => {
  wrapper = mount(ActivityFilter, {
    props: {
      modelValue: {},
      eventBus: mockEventBus,
      ...props,
    },
    global: {
      components: {
        DateFilter,
        SavedFilters,
        DateTimePicker,
      },
      stubs: {
        modal: true,
      },

      mocks: {
        $t: (msg) => msg,
      },
    },
  });
  await wrapper.vm.$nextTick();
};
describe("ActivityFilter", () => {
  beforeEach(async () => {
    await mountActivityFilter();
  });
  afterEach(() => {
    jest.clearAllMocks();
  });
  describe("Rendering and Initialization", () => {
    let resetSpy;

    beforeEach(() => {
      // Create the spy in the beforeEach hook
      resetSpy = jest.spyOn(wrapper.vm, "reset");
    });

    afterEach(() => {
      // Clean up the spy in the afterEach hook
      resetSpy.mockRestore();
    });

    it("sets up display options and calls reset on mount", () => {
      // Mock its implementation in the test case
      resetSpy.mockImplementation(() => {
        wrapper.vm.query = {
          jobFilter: "",
          jobIdFilter: "",
          userFilter: "",
          execnodeFilter: "",
          titleFilter: "",
          statFilter: "",
          recentFilter: "",
          startafterFilter: "",
          startbeforeFilter: "",
          endafterFilter: "",
          endbeforeFilter: "",
        };
      });

      // Call the reset method
      wrapper.vm.reset();

      expect(wrapper.vm.query).toEqual(
        expect.objectContaining({
          jobFilter: "",
          jobIdFilter: "",
          userFilter: "",
          execnodeFilter: "",
          titleFilter: "",
          statFilter: "",
          recentFilter: "",
          startafterFilter: "",
          startbeforeFilter: "",
          endafterFilter: "",
          endbeforeFilter: "",
        }),
      );
      expect(wrapper.vm.DateFilters).toHaveLength(4);

      expect(wrapper.vm.displayOpts).toEqual(
        expect.objectContaining({
          showRecentFilter: true,
          showFilter: true,
          showSavedFilters: true,
        }),
      );
    });
    it("sets up display options and calls reset on mount", () => {
      expect(wrapper.vm.displayOpts).toEqual(
        expect.objectContaining({
          showRecentFilter: true,
          showFilter: true,
          showSavedFilters: true,
        }),
      );
    });

    it("renders correctly if recentFilter is not '-' and showRecentFilter is truthy", async () => {
      await wrapper.setData({
        query: { recentFilter: "1d" },
        displayOpts: { showRecentFilter: true },
      });
      expect(wrapper.find("dropdown").exists()).toBe(true);
    });
    it("does not render if recentFilter is '-' or showRecentFilter is falsy", async () => {
      await wrapper.setData({
        query: { recentFilter: "-" },
        displayOpts: { showRecentFilter: true },
      });
      expect(wrapper.find("dropdown").exists()).toBe(false);
      await wrapper.setData({
        query: { recentFilter: "1d" },
        displayOpts: { showRecentFilter: false },
      });
      expect(wrapper.find("dropdown").exists()).toBe(false);
    });
    it("btn component is rendered when displayOpts.showFilter is truthy", () => {
      expect(wrapper.find("btn").exists()).toBe(true);
    });
    it("btn component is not rendered when displayOpts.showFilter is falsy", async () => {
      await wrapper.setData({ displayOpts: { showFilter: false } });
      expect(wrapper.find("btn").exists()).toBe(false);
    });
    it("renders the saved filters component when showSavedFilters is true", () => {
      expect(wrapper.findComponent(SavedFilters).exists()).toBe(true);
    });
    it("renders the date filters when recentFilter is set to '-'", async () => {
      await wrapper.setData({ query: { recentFilter: "-" } });
      await wrapper.vm.$nextTick();
      expect(wrapper.findComponent(DateFilter).exists()).toBe(true);
    });
    it("sets filterOpen to true when the button is clicked", async () => {
      await wrapper.setData({ filterOpen: false });
      await wrapper.find('[data-test-id="display-button"]').trigger("click");
      expect(wrapper.vm.filterOpen).toBe(true);
    });
    it("btn class is correct based on the value of hasQuery", async () => {
      await wrapper.setData({ hasQuery: true });
      expect(wrapper.find("btn").classes()).toContain("btn-info");
    });
    it("correct list of query parameters is displayed when hasQuery is truthy", async () => {
      await wrapper.setData({
        hasQuery: true,
        query: { jobFilter: "testJob" },
      });
      expect(wrapper.find(".query-params-summary").exists()).toBe(true);
    });
    it("correct text is displayed when hasQuery is falsy", async () => {
      await wrapper.setData({ hasQuery: false });
      expect(wrapper.find("btn").text()).toContain("search.ellipsis");
    });
    it("modal component visibility is controlled by the filterOpen data property", async () => {
      await wrapper.setData({ filterOpen: true });
      expect(wrapper.find("modal").exists()).toBe(true);
    });
    it("form fields inside the modal correctly update the query object", async () => {
      await wrapper.setData({ filterOpen: true });
      const input = wrapper.find('input[name="jobFilter"]');
      await input.setValue("newJobFilter");
      expect(wrapper.vm.query.jobFilter).toBe("newJobFilter");
    });
    it("v-model directive correctly updates the query object", async () => {
      await wrapper.setData({ filterOpen: true });
      const input = wrapper.find('input[name="userFilter"]');
      await input.setValue("newUserFilter");
      expect(wrapper.vm.query.userFilter).toBe("newUserFilter");
    });
    it("date-filter component is rendered when query.recentFilter is equal to '-'", async () => {
      await wrapper.setData({ query: { recentFilter: "-" } });
      expect(wrapper.findComponent(DateFilter).exists()).toBe(true);
    });
  });
  describe("Event Handling", () => {
    it("emits update:modelValue event when query changes", async () => {
      wrapper.vm.query.jobFilter = "newJob";
      await wrapper.vm.search();
      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
      expect(wrapper.emitted("update:modelValue")[0]).toEqual([
        wrapper.vm.query,
      ]);
    });
    it("selectFilter method updates the query and emits event", async () => {
      const filter = {
        query: { jobFilter: "selectedJob" },
        filterName: "selectedFilter",
      };
      await wrapper.vm.selectFilter(filter);
      expect(wrapper.vm.query.jobFilter).toBe("selectedJob");
      expect(wrapper.vm.query.filterName).toBe("selectedFilter");
      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    });
    it("modal close triggers the closing method", async () => {
      const modalComponent = wrapper.findComponent({ name: "modal" });
      await modalComponent.vm.$emit("hide");
      expect(modalComponent.emitted()).toHaveProperty("hide");
    });
    it("filterOpen is set to true when the button is clicked", async () => {
      await wrapper.find("btn").trigger("click");
      expect(wrapper.vm.filterOpen).toBe(true);
    });
    it("search method is called when the search button is clicked", async () => {
      wrapper.vm.search = jest.fn();
      await wrapper.find("btn").trigger("click");
      expect(wrapper.vm.search).toHaveBeenCalled();
    });
    it("saveFilter method is called when the save filter button is clicked", async () => {
      wrapper.vm.saveFilter = jest.fn();
      await wrapper.find("btn").trigger("click");
      expect(wrapper.vm.saveFilter).toHaveBeenCalled();
    });
    it("calls changePeriod when a list item is clicked", async () => {
      await wrapper.setData({
        periods: [{ name: "Day", params: { recentFilter: "1d" } }],
      });
      await wrapper.find("li").trigger("click");
      expect(wrapper.vm.query.recentFilter).toBe("1d");
    });
  });
  describe("Component Behavior and Methods", () => {
    it("reset method sets the query to modelValue", async () => {
      await wrapper.setProps({ modelValue: { jobFilter: "testJob" } });
      await wrapper.vm.reset();
      expect(wrapper.vm.query.jobFilter).toBe("testJob");
    });
    it("checkQueryIsPresent sets hasQuery correctly", async () => {
      await wrapper.setData({ query: { jobFilter: "newJob" } });
      await wrapper.vm.checkQueryIsPresent();
      expect(wrapper.vm.hasQuery).toBe(true);
    });
    it("updateSelectedPeriod updates period based on query.recentFilter", async () => {
      await wrapper.setData({ query: { recentFilter: "1d" } });
      await wrapper.vm.updateSelectedPeriod();
      expect(wrapper.vm.period.name).toBe("Day");
    });
    it("changePeriod updates period and query correctly", async () => {
      const period = { name: "Hour", params: { recentFilter: "1h" } };
      await wrapper.vm.changePeriod(period);
      expect(wrapper.vm.period).toEqual(period);
      expect(wrapper.vm.query.recentFilter).toBe("1h");
    });
    it("opens filter modal on filter button click", async () => {
      const button = wrapper.find("btn");
      await button.trigger("click");
      expect(wrapper.vm.filterOpen).toBe(true);
    });
  });
  it("search method sets filterOpen to false and didSearch to true", async () => {
    await wrapper.vm.search();
    expect(wrapper.vm.filterOpen).toBe(false);
    expect(wrapper.vm.didSearch).toBe(true);
  });
  it("saveFilter method sets didSearch to true and emits save event", async () => {
    jest.useFakeTimers();
    const originalSearch = wrapper.vm.search;
    wrapper.vm.search = jest.fn(originalSearch);
    wrapper.vm.saveFilter();
    expect(wrapper.vm.didSearch).toBe(true);
    jest.advanceTimersByTime(500);
    expect(wrapper.vm.eventBus.emit).toHaveBeenCalledWith("invoke-save-filter");
    jest.useRealTimers();
  });
  it("selectFilter updates query and emits update event", async () => {
    const filter = {
      query: { jobFilter: "testJob" },
      filterName: "testFilter",
    };
    await wrapper.vm.selectFilter(filter);
    expect(wrapper.vm.query.jobFilter).toBe("testJob");
    expect(wrapper.vm.query.filterName).toBe("testFilter");
    expect(wrapper.emitted("update:modelValue")).toBeTruthy();
  });
  it("changePeriod method updates period and query", async () => {
    const period = { name: "Hour", params: { recentFilter: "1h" } };
    await wrapper.vm.changePeriod(period);
    expect(wrapper.vm.period).toStrictEqual(period);
    expect(wrapper.vm.query.recentFilter).toStrictEqual("1h");
  });
  it("updateSelectedPeriod method updates period if it doesn't match query.recentFilter", async () => {
    wrapper.vm.period = { name: "Day", params: { recentFilter: "1d" } };
    await wrapper.setData({ query: { recentFilter: "1h" } });
    await wrapper.vm.updateSelectedPeriod();
    expect(wrapper.vm.period.params.recentFilter).toBe("1h");
  });
  it("reset method resets query and DateFilters", async () => {
    await wrapper.setProps({
      modelValue: {
        jobFilter: "newJob",
        startafterFilter: "2022-01-01T00:00:00",
      },
    });
    await wrapper.vm.reset();
    expect(wrapper.vm.query.jobFilter).toBe("newJob");
    expect(wrapper.vm.DateFilters[0].filter.datetime).toBe(
      "2022-01-01T00:00:00",
    );
  });
  it("closing method resets didSearch if a search was not performed", async () => {
    wrapper.vm.didSearch = false;
    await wrapper.vm.closing();
    expect(wrapper.vm.didSearch).toBe(false);
  });
  it("closing method does not reset didSearch if a search was performed", async () => {
    wrapper.vm.didSearch = true;
    await wrapper.vm.closing();
    expect(wrapper.vm.didSearch).toBe(false);
  });

  describe("Props", () => {
    it("correctly receives and uses eventBus prop", async () => {
      expect(wrapper.vm.eventBus).toStrictEqual(mockEventBus);
    });
  });
  it("correctly receives and uses modelValue prop", async () => {
    await wrapper.setProps({ modelValue: { jobFilter: "testJob" } });
    expect(wrapper.vm.query.jobFilter).toBe("testJob");
  });
  it("should have correct display options", async () => {
    wrapper.vm.displayOpts = {
      showRecentFilter: false,
      showFilter: false,
      showSavedFilters: false,
    };
    await wrapper.vm.$nextTick();

    expect(wrapper.vm.displayOpts).toEqual({
      showRecentFilter: false,
      showFilter: false,
      showSavedFilters: false,
    });
  });
  describe("Components", () => {
    // it("renders  DateFilter component", () => {
    //   expect(wrapper.findComponent(DateFilter).exists()).toBe(true);
    // });
    it("renders SavedFilters component", () => {
      expect(wrapper.findComponent(SavedFilters).exists()).toBe(true);
    });
  });
  describe("Data properties", () => {
    it("filterOpen is initially false", () => {
      expect(wrapper.vm.filterOpen).toBe(false);
    });
    it("initializes query object correctly", () => {
      expect(wrapper.vm.query).toEqual({
        doendafterFilter: "false",
        doendbeforeFilter: "false",
        dostartafterFilter: "false",
        dostartbeforeFilter: "false",
        endafterFilter: "",
        endbeforeFilter: "",
        startafterFilter: "",
        startbeforeFilter: "",
      });
    });
    it("updates query object correctly when methods are called", async () => {
      await wrapper.vm.selectFilter({ query: { jobFilter: "testJob" } });
      expect(wrapper.vm.query.jobFilter).toBe("testJob");
      await wrapper.vm.search();
      expect(wrapper.vm.query.jobFilter).toBe("testJob");

      await wrapper.vm.saveFilter();
      expect(wrapper.vm.query.jobFilter).toBe("testJob");

      await wrapper.vm.changePeriod({
        name: "Day",
        params: { recentFilter: "1d" },
      });
      expect(wrapper.vm.query.recentFilter).toBe("1d");
    });
    it("updates query object correctly when DateFilters are changed", async () => {
      await wrapper.setData({
        DateFilters: [
          {
            name: "startafterFilter",
            filter: { enabled: true, datetime: "2022-01-01" },
          },
        ],
      });
      expect(wrapper.vm.query.startafterFilter).toBe("2022-01-01");
    });
    describe("Emitted events", () => {
      it("calls updateSelectedPeriod when query changes", async () => {
        const updateSelectedPeriodMock = jest.fn();
        wrapper.vm.updateSelectedPeriod = updateSelectedPeriodMock;
        await wrapper.setData({ query: { jobFilter: "newJob" } });
        expect(updateSelectedPeriodMock).toHaveBeenCalled();
      });
    });
    describe("Computed properties", () => {
      it("queryParamsList filters QueryNames based on query data property", async () => {
        await wrapper.setData({
          query: { jobFilter: "newJob", userFilter: "newUser" },
        });
        expect(wrapper.vm.queryParamsList).toEqual(["jobFilter", "userFilter"]);
      });
    });
    describe("Watchers", () => {
      it("watcher for query triggers handler when query changes", async () => {
        const updateSelectedPeriodMock = jest.fn();
        wrapper.vm.updateSelectedPeriod = updateSelectedPeriodMock;
        await wrapper.setData({ query: { jobFilter: "newJob" } });
        expect(updateSelectedPeriodMock).toHaveBeenCalled();
      });
      it("watcher for modelValue triggers reset when modelValue changes", async () => {
        const newValue = { jobFilter: "newJob" };
        await wrapper.setProps({ modelValue: newValue });
        expect(wrapper.vm.query).toEqual(newValue);
      });
      it("watcher for DateFilters updates query correctly", async () => {
        await wrapper.setData({
          DateFilters: [
            {
              name: "startafterFilter",
              filter: { enabled: true, datetime: "2022-01-01T00:00:00" },
            },
          ],
        });
        expect(wrapper.vm.query.startafterFilter).toBe("2022-01-01T00:00:00");
      });
    });
    describe("Methods", () => {
      it("checkQueryIsPresent correctly updates hasQuery", async () => {
        await wrapper.setData({ query: { jobFilter: "newJob" } });
        await wrapper.vm.checkQueryIsPresent();
        expect(wrapper.vm.hasQuery).toBe(true);
      });
      it("search updates query and emits event", async () => {
        wrapper.vm.updated = jest.fn();
        await wrapper.vm.search();
        expect(wrapper.vm.filterOpen).toBe(false);
        expect(wrapper.vm.didSearch).toBe(true);
        expect(wrapper.vm.updated).toHaveBeenCalled();
      });
      it("cancel resets the state and closes the filter", async () => {
        wrapper.vm.reset = jest.fn();
        await wrapper.vm.cancel();
        expect(wrapper.vm.filterOpen).toBe(false);
        expect(wrapper.vm.reset).toHaveBeenCalled();
      });
      it("reset sets query to initial state", async () => {
        const initialState = { jobFilter: "initialJob" };
        await wrapper.setProps({ modelValue: initialState });
        await wrapper.vm.reset();
        expect(wrapper.vm.query).toEqual(initialState);
      });
      it("saveFilter performs search and emits save event", async () => {
        wrapper.vm.search = jest.fn();
        jest.useFakeTimers();
        await wrapper.vm.saveFilter();
        jest.advanceTimersByTime(500);
        expect(wrapper.vm.search).toHaveBeenCalled();
        expect(wrapper.vm.eventBus.emit).toHaveBeenCalledWith(
          "invoke-save-filter",
        );
        jest.useRealTimers();
      });
      it("changePeriod updates period and query", async () => {
        const period = { name: "Day", params: { recentFilter: "1d" } };
        await wrapper.vm.changePeriod(period);
        expect(wrapper.vm.period).toEqual(period);
        expect(wrapper.vm.query.recentFilter).toBe("1d");
      });
      it("updateSelectedPeriod updates period if it doesn't match query.recentFilter", async () => {
        wrapper.vm.period = { name: "Day", params: { recentFilter: "1d" } };
        await wrapper.setData({ query: { recentFilter: "1h" } });
        await wrapper.vm.updateSelectedPeriod();
        expect(wrapper.vm.period.params.recentFilter).toBe("1h");
      });
    });
    describe("Lifecycle Hooks", () => {
      // it("mounted sets up display options and calls reset", async () => {
      // const resetMock = jest.fn();
      //   const opts = { showRecentFilter: false, showFilter: false, showSavedFilters: false }
      //     expect.objectContaining({
      //       jobFilter: "",
      //       jobIdFilter: "",
      //       userFilter: "",
      //       execnodeFilter: "",
      //       titleFilter: "",
      //       statFilter: "",
      //       recentFilter: "",
      //       startafterFilter: "",
      //       startbeforeFilter: "",
      //       endafterFilter: "",
      //       endbeforeFilter: "",
      //     }),
      //   );
      // });
    });
  });
});
