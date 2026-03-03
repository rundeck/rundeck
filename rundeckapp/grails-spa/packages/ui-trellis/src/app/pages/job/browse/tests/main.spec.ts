/**
 * Tests for job browse main.ts - ensures ui-socket widgets are registered for
 * both the jobs list page and the job editor "Choose a job" modal.
 *
 * Uses real RootStore, JobPageStore, UIStore, and loadJsonData. Only mocks
 * external boundaries: RundeckClient (no network), api (no HTTP), and
 * getRundeckContext (to inject our real store).
 */
import { mount, flushPromises } from "@vue/test-utils";
import { createPinia } from "pinia";
import { RootStore } from "../../../../../library/stores/RootStore";
import { loadJsonData } from "../../../../utilities/loadJsonData";
import JobRefFormFields from "../../../../components/job/workflow/JobRefFormFields.vue";

import "../main";

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

function triggerInit() {
  window.dispatchEvent(new Event("DOMContentLoaded"));
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

describe("job browse main", () => {
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
    clearJsonDataElement("pageUiMeta");
    delete (window as any)._rundeck;
  });

  describe("jobPageStore initialization", () => {
    it("resets browsePath and query when pageQueryParams has no groupPath", () => {
      setupJsonDataElement("jobTreeUiMeta", { hideActions: true });
      setupJsonDataElement("pageQueryParams", null);

      triggerInit();

      expect(rootStore.jobPageStore.browsePath).toBe("");
      expect(rootStore.jobPageStore.query["groupPath"]).toBe("");
    });

    it("sets browsePath and query from pageQueryParams when groupPath exists", () => {
      setupJsonDataElement("pageQueryParams", {
        queryParams: { groupPath: "folder/subfolder" },
      });
      setupJsonDataElement("jobTreeUiMeta", { hideActions: true });

      triggerInit();

      expect(rootStore.jobPageStore.browsePath).toBe("folder/subfolder");
      expect(rootStore.jobPageStore.query["groupPath"]).toBe(
        "folder/subfolder",
      );
    });

    it("handles pageQueryParams with empty queryParams", () => {
      setupJsonDataElement("pageQueryParams", { queryParams: {} });
      setupJsonDataElement("jobTreeUiMeta", { hideActions: true });

      triggerInit();

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

      triggerInit();

      expect(rootStore.ui.items.length).toBeGreaterThan(0);
    });

    it("registers job-list-page tree-browser widget", () => {
      triggerInit();

      const treeBrowser = rootStore.ui.items.find(
        (i) => i.section === "job-list-page" && i.location === "tree-browser",
      );

      expect(treeBrowser).toBeDefined();
      expect(treeBrowser!.visible).toBe(true);
      expect(treeBrowser!.widget).toBeDefined();
    });

    it("registers all required job-browse-item widgets", () => {
      triggerInit();

      const jobBrowseItems = rootStore.ui.items.filter(
        (i) => i.section === "job-browse-item",
      );
      const locations = jobBrowseItems.map((i) => i.location);

      expect(locations).toContain("after-job-name:meta:schedule");
      expect(locations).toContain("before-job-name");
      expect(locations).toContain("after-job-name");
    });

    it("registers 8 items; job-list-page/main and main-content/before are hidden when hideHeader", () => {
      setupJsonDataElement("jobTreeUiMeta", {
        hideActions: true,
        hideHeader: true,
      });

      triggerInit();

      expect(rootStore.ui.items).toHaveLength(8);
      const main = rootStore.ui.items.find(
        (i) => i.section === "job-list-page" && i.location === "main",
      );
      const header = rootStore.ui.items.find(
        (i) => i.section === "main-content" && i.location === "before",
      );
      expect(main!.visible).toBe(false);
      expect(header!.visible).toBe(false);
    });

    it("registers 8 items; job-list-page/main and main-content/before are visible when showHeader", () => {
      setupJsonDataElement("pageUiMeta", { uiType: "next" });
      setupJsonDataElement("jobTreeUiMeta", { hideHeader: false });

      triggerInit();

      expect(rootStore.ui.items).toHaveLength(8);
      const main = rootStore.ui.items.find(
        (i) => i.section === "job-list-page" && i.location === "main",
      );
      const header = rootStore.ui.items.find(
        (i) => i.section === "main-content" && i.location === "before",
      );
      expect(main!.visible).toBe(true);
      expect(header!.visible).toBe(true);
    });
  });

  describe("jobTreeUiMeta (hideActions)", () => {
    it("hides JobRunButton and JobActionsMenu when hideActions is true", () => {
      setupJsonDataElement("jobTreeUiMeta", { hideActions: true });

      triggerInit();

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

      triggerInit();

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
      setupJsonDataElement("pageUiMeta", { uiType: "next" });
      setupJsonDataElement("jobTreeUiMeta", null);

      triggerInit();

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

      triggerInit();

      expect(rootStore.jobPageStore.browsePath).toBe("from/dom");
    });
  });

  describe("getJobBrowser usage", () => {
    it("creates JobBrowserStore via page.getJobBrowser()", () => {
      setupJsonDataElement("jobTreeUiMeta", { hideActions: true });

      const browserBefore = rootStore.jobPageStore.browser;
      expect(browserBefore).toBeUndefined();

      triggerInit();

      const browserAfter = rootStore.jobPageStore.browser;
      expect(browserAfter).toBeDefined();
      expect(browserAfter).toHaveProperty("loadItems");
    });
  });

  describe("run condition", () => {
    it("does not run when uiType is not next and jobTreeUiMeta is absent", () => {
      setupJsonDataElement("pageUiMeta", { uiType: "current" });

      triggerInit();

      expect(rootStore.ui.items).toHaveLength(0);
    });

    it("runs when jobTreeUiMeta exists (job editor)", () => {
      setupJsonDataElement("jobTreeUiMeta", {
        hideActions: true,
        hideHeader: true,
      });

      triggerInit();

      expect(rootStore.ui.items.length).toBeGreaterThan(0);
    });

    it("runs when uiType is next (jobs page)", () => {
      setupJsonDataElement("pageUiMeta", { uiType: "next" });
      setupJsonDataElement("jobTreeUiMeta", { hideHeader: false });

      triggerInit();

      expect(rootStore.ui.items.length).toBeGreaterThan(0);
    });
  });

  describe("JobRefFormFields integration", () => {
    it("renders real JobListPage in modal when tree-browser is registered", async () => {
      setupJsonDataElement("jobTreeUiMeta", { hideActions: true });
      triggerInit();

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
