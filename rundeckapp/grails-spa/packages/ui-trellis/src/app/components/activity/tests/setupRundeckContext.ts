// setupRundeckContext.ts
import { EventBus } from "../../../../library";
import { RundeckBrowser } from "@rundeck/client";
import { RootStore } from "../../../../library/stores/RootStore";
import { NavItem } from "../../../../library/stores/NavBar";

export const mockEventBus = {
  on: jest.fn(),
  emit: jest.fn(),
  off: jest.fn(),
  all: new Map(),
};
type RundeckContext = {
  eventBus: typeof EventBus;
  rdBase: string;
  apiVersion: string;
  projectName: string;
  activeTour: string;
  activeStep: string;
  activeTourStep: string;
  appMeta: Record<string, unknown>;
  token: { TOKEN: string; URI: string };
  tokens: { default: { TOKEN: string; URI: string } };
  rundeckClient: RundeckBrowser;
  data: Record<string, unknown>;
  feature: Record<string, { enabled: boolean }>;
  navbar: { items: Array<NavItem> };
  rootStore: RootStore;
};

export const setupRundeckContext = () => {
  window._rundeck = {
    eventBus: mockEventBus as unknown as typeof EventBus,
    rdBase: "mocked-rdBase",
    apiVersion: "47",
    projectName: "test",
    activeTour: "",
    activeStep: "",
    activeTourStep: "",
    appMeta: {},
    token: {
      TOKEN: "mocked-token",
      URI: "mocked-uri",
    },
    tokens: {
      default: {
        TOKEN: "mocked-token",
        URI: "mocked-uri",
      },
    },
    rundeckClient: new RundeckBrowser("mocked-token", "mocked-uri"),
    data: {
      jobslistDateFormatMoment: "MM/DD/YYYY",
      projectAdminAuth: true,
      deleteExecAuth: true,
      activityUrl: "/project/jaya-test/events/eventsAjax",
      nowrunningUrl: "/api/47/project/jaya-test/executions/running",
      bulkDeleteUrl: "/execution/deleteBulkApi",
      activityPageHref: "/project/jaya-test/activity",
      sinceUpdatedUrl: "/project/jaya-test/events/since.json",
      autorefreshms: 5000,
      pagination: {
        max: 10,
      },
      filterOpts: {},
      query: {},
      runningOpts: {
        allowAutoRefresh: true,
        loadRunning: true,
      },
      viewOpts: {
        showBulkDelete: true,
      },
    },
    feature: {
      exampleFeature: {
        enabled: true,
      },
    },
    navbar: {
      items: [] as Array<NavItem>,
    },
    rootStore: {} as RootStore,
  } as RundeckContext;
};
