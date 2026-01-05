import messages from "../../../../../src/app/utilities/locales/en_US";
import { NavItem } from "../../../../library/stores/NavBar";
import { RootStore } from "../../../../library/stores/RootStore";

export const i18nMocks = {
  $t: (msg: string, count: number): string => {
    const translations: { [key: string]: string } = {
      execution: count === 1 ? "execution" : "executions",
      "info.newexecutions.since.0":
        count > 1
          ? `${count} New Results. Click to load.`
          : "1 New Result. Click to load.",
      "In the last Day": "In the last Day",
      "error.message": "An Error Occurred",
    };
    return translations[msg] || messages[msg] || msg;
  },
};
export const mockEventBus = {
  on: jest.fn(),
  emit: jest.fn(),
  off: jest.fn(),
  all: new Map(),
};
export const rundeckClientMock = {
  RundeckBrowser: jest.fn().mockImplementation(() => ({
    executionBulkDelete: jest.fn().mockResolvedValue({ allsuccessful: true }),
    executionListRunning: jest.fn().mockResolvedValue({ executions: [] }),
  })),
  executionBulkDelete: jest.fn().mockResolvedValue({ allsuccessful: true }),
  executionListRunning: jest.fn().mockResolvedValue({ executions: [] }),
};

export const rundeckServiceMock = {
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mocked-rdBase",
    apiVersion: "47",
    projectName: "test",
    activeTour: "",
    activeStep: "",
    activeTourStep: "",
    appMeta: {},
    token: { TOKEN: "mocked-token", URI: "mocked-uri" },
    tokens: { default: { TOKEN: "mocked-token", URI: "mocked-uri" } },
    rundeckClient: rundeckClientMock,
    data: {
      jobslistDateFormatMoment: "MM/DD/YYYY",
      projectAdminAuth: true,
      deleteExecAuth: true,
      activityUrl: "/project/test/events/eventsAjax",
      bulkDeleteUrl: "/execution/deleteBulkApi",
      activityPageHref: "/project/test/activity",
      sinceUpdatedUrl: "/project/test/events/since.json",
      autorefreshms: 5000,
      pagination: { max: 10 },
      filterOpts: {},
      query: null,
      runningOpts: { allowAutoRefresh: true, loadRunning: true },
      viewOpts: { showBulkDelete: true },
    },
    feature: { exampleFeature: { enabled: true } },
    navbar: {
      items: [] as Array<NavItem>,
    },
    rootStore: {} as RootStore,
  }),
  url: jest.fn().mockImplementation((path) => `http://localhost${path}`),
};

export const axiosMock = {
  get: jest.fn(),
  post: jest.fn(),
};
// Ensure the mock context is set up for tests
export const setupRundeckContext = () => {
  window._rundeck = rundeckServiceMock.getRundeckContext();
};
export const mockQueryRunning = jest.fn();
export const mockQueryExecutions = jest.fn();

export const mockReports = [
  {
    id: 42,
    href: "http://localhost:4440/api/56/execution/42",
    permalink: "http://localhost:4440/project/aaa/execution/show/42",
    status: "succeeded",
    project: "aaa",
    executionType: "scheduled",
    user: "admin",
    "date-started": {
      unixtime: 1720195500000,
      date: "2024-07-05T16:55:00Z",
    },
    "date-ended": {
      unixtime: 1720195561552,
      date: "2024-07-05T16:56:01Z",
    },
    job: {
      id: "ce8b97b8-bef0-4ec0-9387-65644cebbaf4",
      averageDuration: 61552,
      name: "count to 60",
      group: "",
      project: "aaa",
      description: "",
      options: {},
      href: "http://localhost:4440/api/56/job/ce8b97b8-bef0-4ec0-9387-65644cebbaf4",
      permalink: "http://localhost:4440/project/aaa/job/show/ce8b97b8-bef0-4ec0-9387-65644cebbaf4",
    },
    description: "Plugin[builtin-script, nodeStep: true]",
    argstring: null,
    serverUUID: "test-server-uuid",
    successfulNodes: ["localhost"],
    failedNodes: [],
  },
  {
    id: 43,
    href: "http://localhost:4440/api/56/execution/43",
    permalink: "http://localhost:4440/project/aaa/execution/show/43",
    status: "succeeded",
    project: "aaa",
    executionType: "user",
    user: "admin",
    "date-started": {
      unixtime: 1720195500000,
      date: "2024-07-05T16:55:00Z",
    },
    "date-ended": {
      unixtime: 1720195561552,
      date: "2024-07-05T16:56:01Z",
    },
    job: {
      id: "ce8b97b8-bef0-4ec0-9387-65644cebbaf4",
      averageDuration: 61552,
      name: "count to 60",
      group: "",
      project: "aaa",
      description: "",
      href: "http://localhost:4440/api/56/job/ce8b97b8-bef0-4ec0-9387-65644cebbaf4",
      permalink:
        "http://localhost:4440/project/aaa/job/show/ce8b97b8-bef0-4ec0-9387-65644cebbaf4",
    },
    description: "Plugin[builtin-script, nodeStep: true]",
    argstring: null,
    serverUUID: "test-server-uuid",
    successfulNodes: ["localhost"],
    failedNodes: [],
  },
];
