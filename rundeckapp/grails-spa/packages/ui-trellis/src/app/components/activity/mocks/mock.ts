// import messages from "../../../../../src/app/utilities/locales/en_US";
// export const i18nMocks = {
//   $t: (msg) => {
//     return messages[msg] || msg;
//   },
//   $tc: (msg, count) => {
//     const translations = {
//       execution: `${count} executions`,
//       "info.newexecutions.since.0":
//         count > 1
//           ? `${count} New Results. Click to load.`
//           : "1 New Result. Click to load.",
//       "In the last Day": "In the last Day",
//     };
//     return translations[msg] || msg;
//   },
// };
// export const rundeckServiceMock = {
//   getRundeckContext: jest.fn().mockReturnValue({ projectName: "test" }),
//   url: jest.fn().mockImplementation((path) => `http://localhost${path}`),
// };
// export const rundeckClientMock = {
//   RundeckBrowser: jest.fn().mockImplementation(() => ({
//     executionBulkDelete: jest.fn().mockResolvedValue({ allsuccessful: true }),
//     executionListRunning: jest.fn().mockResolvedValue({ executions: [] }),
//   })),
// };
// export const axiosMock = {
//   get: jest.fn(),
//   post: jest.fn(),
// };
// export const mockEventBus = {
//   on: jest.fn(),
//   emit: jest.fn(),
//   off: jest.fn(),
//   all: new Map(),
// };

import messages from "../../../../../src/app/utilities/locales/en_US";
import { RundeckBrowser } from "@rundeck/client";
import { NavItem } from "../../../../library/stores/NavBar";
import { RootStore } from "../../../../library/stores/RootStore";
import { EventBus } from "../../../../library";

export const i18nMocks = {
  $t: (msg) => {
    return messages[msg] || msg;
  },
  $tc: (msg, count) => {
    const translations = {
      execution: `${count} executions`,
      "info.newexecutions.since.0":
        count > 1
          ? `${count} New Results. Click to load.`
          : "1 New Result. Click to load.",
      "In the last Day": "In the last Day",
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
export const rundeckServiceMock = {
  getRundeckContext: jest.fn().mockReturnValue({
    eventBus: mockEventBus as unknown as typeof EventBus,
    rdBase: "mocked-rdBase",
    apiVersion: "47",
    projectName: "test",
    activeTour: "",
    activeStep: "",
    activeTourStep: "",
    appMeta: {},
    token: { TOKEN: "mocked-token", URI: "mocked-uri" },
    tokens: { default: { TOKEN: "mocked-token", URI: "mocked-uri" } },
    rundeckClient: new RundeckBrowser("mocked-token", "mocked-uri"),
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
      query: {},
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
export const rundeckClientMock = {
  RundeckBrowser: jest.fn().mockImplementation(() => ({
    executionBulkDelete: jest.fn().mockResolvedValue({ allsuccessful: true }),
    executionListRunning: jest.fn().mockResolvedValue({ executions: [] }),
  })),
};
export const axiosMock = {
  get: jest.fn(),
  post: jest.fn(),
};
// Ensure the mock context is set up for tests
export const setupRundeckContext = () => {
  window._rundeck = rundeckServiceMock.getRundeckContext();
};
