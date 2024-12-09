import { setActivePinia, createPinia } from "pinia";
import { useJobStore } from "../JobsStore";
import { mockJobDefinition } from "./mocks/mockJobData";
import { getJobDefinition } from "../../services/jobEdit";

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: { on: jest.fn(), emit: jest.fn() },
    rdBase: "http://localhost:4440/",
    projectName: "testProject",
    apiVersion: "44",
  })),
}));
jest.mock("../../services/jobEdit");

const mockedGetJobDefinition = getJobDefinition as jest.MockedFunction<
  typeof getJobDefinition
>;

describe("Counter Store", () => {
  beforeEach(() => {
    // creates a fresh pinia and makes it active
    // so it's automatically picked up by any useStore() call
    // without having to pass it to it: `useStore(pinia)`
    setActivePinia(createPinia());
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it("initializes without an activeId", async () => {
    const jobStore = useJobStore();
    expect(jobStore.activeId).toBe("");
  });

  it("fetches a job definition and set it as active", async () => {
    mockedGetJobDefinition.mockResolvedValueOnce(mockJobDefinition);
    const jobStore = useJobStore();
    await jobStore.fetchJobDefinition("test");

    expect(jobStore.activeId).toBe("test");
    expect(jobStore.jobDefinition).toEqual(mockJobDefinition[0]);
  });
});
