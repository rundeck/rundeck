export const i18nMocks = {
  $t: (msg) => {
    const translations = {
      "error.message.0": "An Error Occurred",
      "results.empty.text": "No results for the query",
      "bulkresult.attempted.text": " Executions were attempted.",
      "pagination.of": "of",
      "Auto refresh": "Auto refresh",
      "bulk.selected.count": "selected",
      "select.all": "Select All",
      "select.none": "Select None",
      "delete.selected.executions": "Delete Selected Executions",
      "cancel.bulk.delete": "Cancel Bulk Delete",
      "bulk.delete": "Bulk Delete",
      "clearselected.confirm.text":
        "Clear all {0} selected items, or only items shown on this page?",
    };
    return translations[msg] || msg;
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
