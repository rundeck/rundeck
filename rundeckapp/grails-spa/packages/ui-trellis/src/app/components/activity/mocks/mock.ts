import messages from "../../../../../src/app/utilities/locales/en_US";
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
export const rundeckServiceMock = {
  getRundeckContext: jest.fn().mockReturnValue({ projectName: "test" }),
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
export const mockEventBus = {
  on: jest.fn(),
  emit: jest.fn(),
  off: jest.fn(),
  all: new Map(),
};
