import { mount, VueWrapper } from "@vue/test-utils";
import JobSearchModal from "../JobSearchModal.vue";
import { JobPageStoreInjectionKey } from "../../../../../library/stores/JobPageStore";
import { JobListFilterStoreInjectionKey } from "../../../../../library/stores/JobListFilterStore";

// JobPageStore transitively imports services/api.ts, which reads
// getRundeckContext() at module load time.
jest.mock("@/library/rundeckService.ts", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    apiVersion: 41,
    rdBase: "http://localhost:4440",
  })),
}));

interface JobPageStoreStub {
  query: Record<string, string>;
}

interface MountOptions {
  props?: Record<string, any>;
  jobPageStore?: JobPageStoreStub;
}

const createJobPageStoreStub = (): JobPageStoreStub => ({
  query: {
    project: "",
    idlist: "",
    jobFilter: "",
    groupPath: "",
    descFilter: "",
    scheduledFilter: "",
    serverNodeUUIDFilter: "",
    jobUuidFilter: "",
  },
});

const createWrapper = async (
  options: MountOptions = {},
): Promise<VueWrapper<any>> => {
  const wrapper = mount(JobSearchModal, {
    props: {
      modelValue: true,
      ...options.props,
    },
    global: {
      provide: {
        [JobPageStoreInjectionKey as symbol]:
          options.jobPageStore || createJobPageStoreStub(),
        [JobListFilterStoreInjectionKey as symbol]: {},
      },
      stubs: {
        UiSocket: true,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

const findByTestId = (wrapper: VueWrapper<any>, testId: string) =>
  wrapper.find(`[data-testid="${testId}"]`);

describe("JobSearchModal", () => {
  // RUN-4966: <btn type="button"> treats "type" as the style variant, so it
  // rendered the nonexistent class "btn-button" instead of a styled button.
  it("renders the cancel button with the btn-default variant class", async () => {
    const wrapper = await createWrapper();

    const cancelButton = findByTestId(
      wrapper,
      "job-search-modal-cancel-button",
    );

    expect(cancelButton.exists()).toBe(true);
    expect(cancelButton.classes()).toContain("btn-default");
    expect(cancelButton.classes()).not.toContain("btn-button");
  });

  it("renders the clear search button with the btn-default variant class", async () => {
    const wrapper = await createWrapper();

    const clearButton = findByTestId(wrapper, "job-search-modal-clear-button");

    expect(clearButton.exists()).toBe(true);
    expect(clearButton.classes()).toContain("btn-default");
    expect(clearButton.classes()).not.toContain("btn-button");
  });

  it("renders the job UUID search input", async () => {
    const wrapper = await createWrapper();

    const uuidInput = findByTestId(wrapper, "job-search-modal-job-uuid-input");

    expect(uuidInput.attributes("name")).toBe("jobUuidFilter");
  });

  it("sets the jobUuidFilter query value when a job UUID is entered", async () => {
    const jobPageStore = createJobPageStoreStub();
    const wrapper = await createWrapper({ jobPageStore });

    await findByTestId(wrapper, "job-search-modal-job-uuid-input").setValue(
      "2020c78b-a228-4627-8015-3177ebaa00a7",
    );

    expect(jobPageStore.query.jobUuidFilter).toBe(
      "2020c78b-a228-4627-8015-3177ebaa00a7",
    );
  });
});
