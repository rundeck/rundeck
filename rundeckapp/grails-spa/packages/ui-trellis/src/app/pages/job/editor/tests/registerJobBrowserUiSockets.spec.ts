/**
 * Tests for registerJobBrowserUiSockets - ensures the job editor can render
 * the "Choose a job" modal (JobRefFormFields) without loading job/browse.js.
 *
 * Uses real RootStore, JobPageStore, UIStore, and loadJsonData. Only mocks
 * external boundaries: RundeckClient (no network), api (no HTTP), and
 * getRundeckContext (to inject our real store).
 */
import { mount, flushPromises } from "@vue/test-utils";
import { createPinia } from "pinia";
import { RootStore } from "../../../../../library/stores/RootStore";
import { registerJobBrowserUiSockets } from "../registerJobBrowserUiSockets";
import { loadJsonData } from "../../../../utilities/loadJsonData";
import JobRefFormFields from "../../../../components/job/workflow/JobRefFormFields.vue";

const mockClient = {
  projectList: jest.fn().mockResolvedValue([]),
} as any;
let rootStore: RootStore;

function setupJsonDataElement(id: string, data: unknown) {
  const existing = document.getElementById(id);
  if (existing) existing.remove();
  const el = document.createElement("script");
  el.id = id;
  el.textContent = JSON.stringify(data);
  document.body.appendChild(el);
}

function clearJsonDataElement(id: string) {
  document.getElementById(id)?.remove();
}

jest.mock("@rundeck/client", () => ({
  RundeckClient: jest.fn().mockImplementation(() => mockClient),
}));

jest.mock("@/library/services/api", () => ({
  api: {
    get: jest.fn().mockResolvedValue({ status: 200, data: [] }),
  },
}));

jest.mock("@/library", () => {
  const context: Record<string, unknown> = {
    eventBus: { on: jest.fn(), emit: jest.fn(), off: jest.fn() },
    rdBase: "http://test/",
    projectName: "testProject",
  };
  return {
    getRundeckContext: () => context,
    __testContext: context,
  };
});

jest.mock("@/library/stores/NodesStorePinia", () => ({
  useNodesStore: () => ({
    total: 0,
    nodeFilterStore: { filter: "", setSelectedFilter: jest.fn() },
    currentNodes: [],
    lastCountFetched: 0,
    isResultsTruncated: false,
    fetchNodes: jest.fn(),
  }),
}));

jest.mock("@/library/components/utils/contextVariableUtils", () => ({
  getContextVariables: jest.fn().mockReturnValue([]),
  transformVariables: jest.fn().mockReturnValue([]),
}));

jest.mock("@/library/services/jobBrowse", () => ({
  getProjectMeta: jest.fn().mockResolvedValue([]),
  browsePath: jest.fn().mockResolvedValue({ items: [] }),
  queryPath: jest.fn().mockResolvedValue({ items: [] }),
}));

describe("registerJobBrowserUiSockets", () => {
  beforeEach(() => {
    (window as any)._rundeck = {
      navbar: { items: [] },
      rdBase: "http://test/",
      projectName: "testProject",
      eventBus: { on: jest.fn(), emit: jest.fn(), off: jest.fn() },
    };

    rootStore = new RootStore(mockClient);
    rootStore.ui.items = [];
    rootStore.jobPageStore.browsePath = "stale/path";
    rootStore.jobPageStore.query = { groupPath: "stale/path" };

    (window as any)._rundeck.rootStore = rootStore;

    const { __testContext } = require("@/library");
    __testContext.rootStore = rootStore;
  });


  afterEach(() => {
    clearJsonDataElement("pageQueryParams");
    clearJsonDataElement("jobTreeUiMeta");
    delete (window as any)._rundeck;
  });

  describe("jobPageStore initialization", () => {
    it("resets browsePath and query when pageQueryParams has no groupPath", () => {
      setupJsonDataElement("jobTreeUiMeta", { hideActions: true });
      setupJsonDataElement("pageQueryParams", null);

      registerJobBrowserUiSockets();

      expect(rootStore.jobPageStore.browsePath).toBe("");
      expect(rootStore.jobPageStore.query["groupPath"]).toBe("");
    });

    it("sets browsePath and query from pageQueryParams when groupPath exists", () => {
      setupJsonDataElement("pageQueryParams", {
        queryParams: { groupPath: "folder/subfolder" },
      });
      setupJsonDataElement("jobTreeUiMeta", { hideActions: true });

      registerJobBrowserUiSockets();

      expect(rootStore.jobPageStore.browsePath).toBe("folder/subfolder");
      expect(rootStore.jobPageStore.query["groupPath"]).toBe(
        "folder/subfolder",
      );
    });

    it("handles pageQueryParams with empty queryParams", () => {
      setupJsonDataElement("pageQueryParams", { queryParams: {} });
      setupJsonDataElement("jobTreeUiMeta", { hideActions: true });

      registerJobBrowserUiSockets();

      expect(rootStore.jobPageStore.browsePath).toBe("");
      expect(rootStore.jobPageStore.query["groupPath"]).toBe("");
    });
  });

  describe("ui-socket registration", () => {
    beforeEach(() => {
      setupJsonDataElement("jobTreeUiMeta", { hideActions: true });
    });

    it("adds items to the real UIStore", () => {
      expect(rootStore.ui.items).toHaveLength(0);

      registerJobBrowserUiSockets();

      expect(rootStore.ui.items.length).toBeGreaterThan(0);
    });

    it("registers job-list-page tree-browser widget", () => {
      registerJobBrowserUiSockets();

      const treeBrowser = rootStore.ui.items.find(
        (i) => i.section === "job-list-page" && i.location === "tree-browser",
      );

      expect(treeBrowser).toBeDefined();
      expect(treeBrowser!.visible).toBe(true);
      expect(treeBrowser!.widget).toBeDefined();
    });

    it("registers all required job-browse-item widgets", () => {
      registerJobBrowserUiSockets();

      const jobBrowseItems = rootStore.ui.items.filter(
        (i) => i.section === "job-browse-item",
      );
      const locations = jobBrowseItems.map((i) => i.location);

      expect(locations).toContain("after-job-name:meta:schedule");
      expect(locations).toContain("before-job-name");
      expect(locations).toContain("after-job-name");
    });

    it("registers exactly 6 ui-socket items", () => {
      registerJobBrowserUiSockets();

      expect(rootStore.ui.items).toHaveLength(6);
    });
  });

  describe("jobTreeUiMeta (hideActions)", () => {
    it("hides JobRunButton and JobActionsMenu when hideActions is true", () => {
      setupJsonDataElement("jobTreeUiMeta", { hideActions: true });

      registerJobBrowserUiSockets();

      const runButton = rootStore.ui.items.find(
        (i) => i.section === "job-browse-item" && i.order === 1,
      );
      const actionsMenu = rootStore.ui.items.find(
        (i) => i.section === "job-browse-item" && i.order === 1000,
      );

      expect(runButton!.visible).toBe(false);
      expect(actionsMenu!.visible).toBe(false);
    });

    it("shows JobRunButton and JobActionsMenu when hideActions is false", () => {
      setupJsonDataElement("jobTreeUiMeta", { hideActions: false });

      registerJobBrowserUiSockets();

      const runButton = rootStore.ui.items.find(
        (i) => i.section === "job-browse-item" && i.order === 1,
      );
      const actionsMenu = rootStore.ui.items.find(
        (i) => i.section === "job-browse-item" && i.order === 1000,
      );

      expect(runButton!.visible).toBe(true);
      expect(actionsMenu!.visible).toBe(true);
    });

    it("shows actions when jobTreeUiMeta is null (default)", () => {
      setupJsonDataElement("jobTreeUiMeta", null);

      registerJobBrowserUiSockets();

      const runButton = rootStore.ui.items.find(
        (i) => i.section === "job-browse-item" && i.order === 1,
      );
      expect(runButton!.visible).toBe(true);
    });
  });

  describe("loadJsonData integration", () => {
    it("uses real loadJsonData - reads from DOM", () => {
      setupJsonDataElement("pageQueryParams", {
        queryParams: { groupPath: "from/dom" },
      });
      setupJsonDataElement("jobTreeUiMeta", { hideActions: true });

      const pageQueryParams = loadJsonData("pageQueryParams");
      expect(pageQueryParams).toEqual({
        queryParams: { groupPath: "from/dom" },
      });

      registerJobBrowserUiSockets();

      expect(rootStore.jobPageStore.browsePath).toBe("from/dom");
    });
  });

  describe("getJobBrowser usage", () => {
    it("creates JobBrowserStore via page.getJobBrowser()", () => {
      setupJsonDataElement("jobTreeUiMeta", { hideActions: true });

      const browserBefore = rootStore.jobPageStore.browser;
      expect(browserBefore).toBeUndefined();

      registerJobBrowserUiSockets();

      const browserAfter = rootStore.jobPageStore.browser;
      expect(browserAfter).toBeDefined();
      expect(browserAfter).toHaveProperty("loadItems");
    });
  });

  describe("JobRefFormFields integration", () => {
    it("renders real JobListPage in modal when tree-browser is registered", async () => {
      setupJsonDataElement("jobTreeUiMeta", { hideActions: true });
      registerJobBrowserUiSockets();

      const pinia = createPinia();
      const wrapper = mount(JobRefFormFields, {
        props: {
          modelValue: {
            nodeStep: false,
            name: "",
            uuid: "",
            project: "testProject",
            group: "",
            args: "",
            failOnDisable: false,
            childNodes: false,
            importOptions: false,
            ignoreNotifications: false,
            nodefilters: {
              filter: "",
              dispatch: {
                threadcount: null,
                keepgoing: null,
                rankAttribute: null,
                rankOrder: null,
                nodeIntersect: null,
              },
            },
          },
          showValidation: false,
          extraAutocompleteVars: [],
        },
        global: {
          plugins: [pinia],
          stubs: {
            "node-filter-input": { template: "<div class='node-filter-stub' />" },
          },
          mocks: {
            $t: (key: string) => key,
          },
        },
        attachTo: document.body,
      });
      await flushPromises();

      const chooseBtn = wrapper.find(".act_choose_job");
      await chooseBtn.trigger("click");
      await flushPromises();

      const treeBrowser = document.querySelector(".job_list_browser");
      expect(treeBrowser).not.toBeNull();

      wrapper.unmount();
    });
  });
});
