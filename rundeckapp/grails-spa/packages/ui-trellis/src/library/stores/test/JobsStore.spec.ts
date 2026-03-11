import { setActivePinia, createPinia } from "pinia";
import { useJobStore } from "../JobsStore";
import { mockJobDefinition } from "./mocks/mockJobData";
import { getJobDefinition, postJobDefinition } from "../../services/jobEdit";

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
const mockedPostJobDefinition = postJobDefinition as jest.MockedFunction<
  typeof postJobDefinition
>;

describe("Jobs Store", () => {
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

  it("initializes with context variables", async () => {
    const jobStore = useJobStore();
    expect(jobStore.contextVariables).toHaveProperty("error_handler");
    expect(jobStore.contextVariables).toHaveProperty("job");
    expect(jobStore.contextVariables).toHaveProperty("node");
    expect(jobStore.contextVariables).toHaveProperty("execution");
    expect(jobStore.contextVariables).not.toHaveProperty("options");
  });

  it("fetches a job definition and set it as active", async () => {
    mockedGetJobDefinition.mockResolvedValueOnce(mockJobDefinition);
    const jobStore = useJobStore();
    await jobStore.fetchJobDefinition("test");

    expect(jobStore.activeId).toBe("test");
    expect(jobStore.jobDefinition).toEqual(mockJobDefinition[0]);
  });

  it("fetches a job definition without setting it as active", async () => {
    mockedGetJobDefinition.mockResolvedValueOnce(mockJobDefinition);
    const jobStore = useJobStore();
    await jobStore.fetchJobDefinition("test", false);

    expect(jobStore.activeId).toBe("");
    // therefore getter will return undefined
    expect(jobStore.jobDefinition).toEqual(undefined);
  });

  it("initializes job placeholder with default values", () => {
    const jobStore = useJobStore();
    jobStore.initializeJobPlaceholder();

    expect(jobStore.jobs["!new"]).toBeDefined();
    expect(jobStore.jobs["!new"].id).toBe("!new");
    expect(jobStore.jobs["!new"].name).toBe("");
    expect(jobStore.jobs["!new"].scheduleEnabled).toBe(true);
    expect(jobStore.jobs["!new"].executionEnabled).toBe(true);
  });

  it("setActiveId sets the active ID", () => {
    const jobStore = useJobStore();
    jobStore.setActiveId("test-job-123");

    expect(jobStore.activeId).toBe("test-job-123");
  });

  it("setActiveId defaults to !new when given empty string", () => {
    const jobStore = useJobStore();
    jobStore.setActiveId("");

    expect(jobStore.activeId).toBe("!new");
  });

  it("hasJob returns false when job does not exist", () => {
    const jobStore = useJobStore();

    expect(jobStore.hasJob("non-existent-job")).toBe(false);
  });

  it("hasJob returns true when job exists", () => {
    const jobStore = useJobStore();
    jobStore.updateJobDefinition(
      { id: "test-123", name: "Test Job" } as any,
      "test-123",
    );

    expect(jobStore.hasJob("test-123")).toBe(true);
  });

  it("hasJob returns true for placeholder after initialization", () => {
    const jobStore = useJobStore();
    jobStore.initializeJobPlaceholder();

    expect(jobStore.hasJob("!new")).toBe(true);
  });

  it("resets placeholder after saving a new job", async () => {
    mockedPostJobDefinition.mockResolvedValueOnce({
      succeeded: [
        {
          index: 0,
          id: "saved-job-123",
          name: "My New Job",
          group: "",
          project: "testProject",
        },
      ],
      failed: [],
      skipped: [],
    });

    const jobStore = useJobStore();

    // Setup: initialize placeholder and set as active
    jobStore.initializeJobPlaceholder();
    jobStore.setActiveId("!new");

    // Modify the placeholder
    jobStore.updateJobDefinition({ name: "My New Job" } as any, "!new");

    // Save the job
    await jobStore.saveJobDefinition(jobStore.jobs["!new"], {});

    // Should have saved job under new ID
    expect(jobStore.jobs["saved-job-123"]).toBeDefined();
    expect(jobStore.jobs["saved-job-123"].id).toBe("saved-job-123");
    expect(jobStore.jobs["saved-job-123"].name).toBe("My New Job");

    // Should have reset the placeholder
    expect(jobStore.jobs["!new"].name).toBe("");
  });

  it("does not reset placeholder when saving existing job", async () => {
    mockedPostJobDefinition.mockResolvedValueOnce({
      succeeded: [
        {
          index: 0,
          id: "existing-job-456",
          name: "Existing Job",
          group: "",
          project: "testProject",
        },
      ],
      failed: [],
      skipped: [],
    });

    const jobStore = useJobStore();

    // Setup: working on existing job
    jobStore.updateJobDefinition(
      { id: "existing-job-456", name: "Existing Job" } as any,
      "existing-job-456",
    );
    jobStore.setActiveId("existing-job-456");

    // Save the job
    await jobStore.saveJobDefinition(jobStore.jobs["existing-job-456"], {});

    // Should still be on existing job
    expect(jobStore.activeId).toBe("existing-job-456");

    // Should not have created placeholder
    expect(jobStore.jobs["!new"]).toBeUndefined();
  });
});
