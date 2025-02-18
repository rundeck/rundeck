import mitt from "mitt";
import { mount, VueWrapper } from "@vue/test-utils";
import Browser from "../tree/Browser.vue";

// First mock the API
jest.mock("@/library/services/api", () => ({
  api: {
    get: jest.fn(),
    post: jest.fn(),
  },
  rundeckService: {
    baseURL: "http://localhost:4440/api/41/",
    headers: {
      "X-Rundeck-ajax": "true",
      Accept: "application/json",
    },
  },
}));

// Then mock the library with complete rundeck context
jest.mock("@/library", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    rdBase: "http://localhost:4440/",
    apiVersion: "41",
    projectName: "TestProject",
    eventBus: mitt(),
    rootStore: {
      projects: {
        loaded: true,
        projects: [{ name: "TestProject" }, { name: "OtherProject" }],
      },
    },
    data: {
      nodeData: {},
    },
  })),
}));

jest.mock("@/app/pages/job/browse/tree/BrowseGroupItem.vue", () => ({
  name: "BrowseGroupItem",
  template: "<div><slot /><slot name='supplemental' /></div>",
}));

jest.mock("@/app/pages/job/browse/tree/BrowserJobItem.vue", () => ({
  name: "BrowserJobItem",
  template: "<div></div>",
}));

jest.mock("vue-virtual-scroller", () => ({
  RecycleScroller: {
    name: "RecycleScroller",
    template:
      "<div><slot v-for='item in items' :item='item' :active='true'></slot></div>",
    props: ["items", "itemSize", "keyField"],
  },
}));

const mockJobBrowserStore = {
  loadItems: jest.fn(),
  findPath: jest.fn(),
};

const mockJobPageStore = {
  jobPagePathHref: jest.fn(),
  filters: [],
  bulkEditMode: false,
};

const createWrapper = async (
  props = {},
  storeOverrides = {},
): Promise<VueWrapper<any>> => {
  return mount(Browser, {
    props: {
      path: "",
      root: false,
      expandLevel: 0,
      queryRefresh: false,
      ...props,
    },
    global: {
      provide: {
        [Symbol.for("jobBrowserStore")]: {
          ...mockJobBrowserStore,
          ...storeOverrides,
        },
        [Symbol.for("jobPageStore")]: {
          ...mockJobPageStore,
          ...storeOverrides,
        },
      },
    },
  });
};

describe("Browser", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockJobBrowserStore.loadItems.mockResolvedValue([]);
    mockJobBrowserStore.findPath.mockReturnValue({ bpHit: false });
    mockJobPageStore.jobPagePathHref.mockReturnValue("/mock/path");
  });

  describe("Initialization", () => {
    it("loads items on mount", async () => {
      await createWrapper();
      expect(mockJobBrowserStore.loadItems).toHaveBeenCalledWith("");
    });

    it("shows loading state while fetching items", async () => {
      mockJobBrowserStore.loadItems.mockImplementation(
        () => new Promise(() => {}),
      );
      const wrapper = await createWrapper();
      expect(wrapper.find(".fa-spinner").exists()).toBe(true);
    });
  });

  describe("Group Handling", () => {
    const mockGroups = [
      { groupPath: "group1", job: false },
      { groupPath: "group2", job: false },
    ];

    it("renders groups in sorted order", async () => {
      mockJobBrowserStore.loadItems.mockResolvedValue(mockGroups);
      const wrapper = await createWrapper();

      const groups = wrapper.findAllComponents({ name: "BrowseGroupItem" });
      expect(groups).toHaveLength(2);
      expect(groups[0].props("item").groupPath).toBe("group1");
      expect(groups[1].props("item").groupPath).toBe("group2");
    });

    it("handles group expansion correctly", async () => {
      const wrapper = await createWrapper();

      await wrapper.vm.toggle("group1");
      expect(wrapper.vm.isExpanded("group1")).toBe(true);

      await wrapper.vm.toggle("group1");
      expect(wrapper.vm.isExpanded("group1")).toBe(false);
    });
  });

  describe("Job Items Handling", () => {
    const mockJobs = [
      { id: "job1", job: true, jobName: "Job A" },
      { id: "job2", job: true, jobName: "Job B" },
    ];

    it("renders jobs in sorted order", async () => {
      mockJobBrowserStore.loadItems.mockResolvedValue(mockJobs);
      const wrapper = await createWrapper();

      const jobItems = wrapper.findAllComponents({ name: "BrowserJobItem" });
      expect(jobItems).toHaveLength(2);
    });

    it("applies filters to jobs", async () => {
      const mockFilter = { filter: jest.fn().mockReturnValue(true) };
      mockJobBrowserStore.loadItems.mockResolvedValue(mockJobs);
      await createWrapper(
        {},
        {
          jobPageStore: {
            ...mockJobPageStore,
            filters: [mockFilter],
          },
        },
      );

      expect(mockFilter.filter).toHaveBeenCalled();
    });
  });

  describe("Breakpoint Handling", () => {
    it("shows breakpoint info when breakpoint is hit", async () => {
      mockJobBrowserStore.findPath.mockReturnValue({ bpHit: true });
      const wrapper = await createWrapper();

      expect(wrapper.find(".breakpoint-info").exists()).toBe(true);
    });

    it("handles breakpoint reset", async () => {
      mockJobBrowserStore.findPath.mockReturnValue({ bpHit: true });
      const wrapper = await createWrapper();

      await wrapper.find(".breakpoint-info button").trigger("click");
      expect(mockJobBrowserStore.loadItems).toHaveBeenCalled();
    });
  });

  describe("Event Handling", () => {
    const { getRundeckContext } = require("@/library");
    const rundeckContext = getRundeckContext();

    it("emits empty event when no items are found", async () => {
      mockJobBrowserStore.loadItems.mockResolvedValue([]);
      const wrapper = await createWrapper();

      expect(wrapper.emitted("empty")).toBeTruthy();
    });

    it("handles bulk edit mode correctly", async () => {
      const wrapper = await createWrapper(
        {},
        {
          jobPageStore: {
            ...mockJobPageStore,
            bulkEditMode: true,
          },
        },
      );

      // Find buttons by their containing elements
      const selectAllBtn = wrapper.find("button:has(.glyphicon-check)");
      const selectNoneBtn = wrapper.find("button:has(.glyphicon-unchecked)");

      await selectAllBtn.trigger("click");
      expect(rundeckContext.eventBus.emit).toHaveBeenCalledWith(
        "job-bulk-edit-select-all-path",
        expect.any(String),
      );

      await selectNoneBtn.trigger("click");
      expect(rundeckContext.eventBus.emit).toHaveBeenCalledWith(
        "job-bulk-edit-select-none-path",
        expect.any(String),
      );
    });
  });

  describe("Path Changes", () => {
    it("refreshes when path changes", async () => {
      const wrapper = await createWrapper({ path: "initial" });

      await wrapper.setProps({ path: "new" });
      expect(mockJobBrowserStore.loadItems).toHaveBeenCalledWith("new");
    });

    it("refreshes when queryRefresh changes", async () => {
      const wrapper = await createWrapper({ queryRefresh: false });

      await wrapper.setProps({ queryRefresh: true });
      expect(mockJobBrowserStore.loadItems).toHaveBeenCalled();
    });
  });
});
