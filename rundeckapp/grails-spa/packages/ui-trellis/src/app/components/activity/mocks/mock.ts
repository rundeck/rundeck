import messages from "../../../../../src/app/utilities/locales/en_US";
import { NavItem } from "../../../../library/stores/NavBar";
import { RootStore } from "../../../../library/stores/RootStore";

export const i18nMocks = {
  $t: (msg) => {
    return messages[msg] || msg;
  },
  $tc: (msg, count) => {
    const translations = {
      execution: count === 1 ? "execution" : "executions",
      "info.newexecutions.since.0":
        count > 1
          ? `${count} New Results. Click to load.`
          : "1 New Result. Click to load.",
      "In the last Day": "In the last Day",
      "error.message": "An Error Occurred",
    };
    return translations[msg] || msg;
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
      nowrunningUrl: "/api/47/project/test/executions/running",
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

export const mockReports = [
  {
    node: "1/0/1",
    status: "succeed",
    actionType: "succeed",
    project: "aaa",
    tags: null,
    message: "Job status succeeded",
    dateStarted: "2024-07-05T16:55:00Z",
    dateCompleted: "2024-07-05T16:56:01Z",
    executionId: "42",
    jobId: "65",
    adhocExecution: false,
    adhocScript: null,
    abortedByUser: null,
    succeededNodeList: "localhost",
    failedNodeList: null,
    filterApplied: null,
    jobUuid: "ce8b97b8-bef0-4ec0-9387-65644cebbaf4",
    executionUuid: "f4279d8f-13a9-494b-8750-f6d6e4d17031",
    duration: 61552,
    execution: {
      jobId: "ce8b97b8-bef0-4ec0-9387-65644cebbaf4",
      dateStarted: "2024-07-05T16:55:00Z",
      dateCompleted: "2024-07-05T16:56:01Z",
      status: "succeeded",
      outputfilepath: "",
      execIdForLogStore: 42,
      failedNodeList: null,
      succeededNodeList: "localhost",
      abortedby: null,
      cancelled: false,
      argString: null,
      loglevel: "INFO",
      id: 42,
      uuid: "f4279d8f-13a9-494b-8750-f6d6e4d17031",
      doNodedispatch: false,
      executionType: "scheduled",
      project: "aaa",
      user: "admin",
      workflow: {
        keepgoing: false,
        strategy: "node-first",
        commands: [
          {
            script:
              '\n#!/bin/bash\n\nfor ((i=1; i<=60; i++)); do\n    echo "Count: $i"\n    sleep 1\ndone\n',
          },
        ],
      },
      extra: {
        useLocalRunner: true,
      },
    },
    executionHref: "/project/aaa/execution/show/42",
    jobName: "count to 60",
    user: "admin",
    executionString: "Plugin[builtin-script, nodeStep: true]",
  },
  {
    node: "1/0/1",
    status: "succeed",
    actionType: "succeed",
    project: "aaa",
    tags: null,
    message: "Job status succeeded",
    dateStarted: "2024-07-05T16:55:00Z",
    dateCompleted: "2024-07-05T16:56:01Z",
    executionId: "42",
    jobId: "65",
    adhocExecution: false,
    adhocScript: null,
    abortedByUser: null,
    succeededNodeList: "localhost",
    failedNodeList: null,
    filterApplied: null,
    jobUuid: "ce8b97b8-bef0-4ec0-9387-65644cebbaf4",
    executionUuid: "f4279d8f-13a9-494b-8750-f6d6e4d17031",
    duration: 61552,
    execution: {
      jobId: "ce8b97b8-bef0-4ec0-9387-65644cebbaf4",
      dateStarted: "2024-07-05T16:55:00Z",
      dateCompleted: "2024-07-05T16:56:01Z",
      status: "succeeded",
      outputfilepath: "",
      execIdForLogStore: 42,
      failedNodeList: null,
      succeededNodeList: "localhost",
      abortedby: null,
      cancelled: false,
      argString: null,
      loglevel: "INFO",
      id: 42,
      uuid: "f4279d8f-13a9-494b-8750-f6d6e4d17031",
      doNodedispatch: false,
      executionType: "scheduled",
      project: "aaa",
      user: "admin",
      workflow: {
        keepgoing: false,
        strategy: "node-first",
        commands: [
          {
            script:
              '\n#!/bin/bash\n\nfor ((i=1; i<=60; i++)); do\n    echo "Count: $i"\n    sleep 1\ndone\n',
          },
        ],
      },
      extra: {
        useLocalRunner: true,
      },
    },
    executionHref: "/project/aaa/execution/show/42",
    jobName: "count to 60",
    user: "admin",
    executionString: "Plugin[builtin-script, nodeStep: true]",
  },
];
