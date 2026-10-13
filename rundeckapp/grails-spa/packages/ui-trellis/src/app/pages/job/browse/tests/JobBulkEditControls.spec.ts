import { mount, VueWrapper } from "@vue/test-utils";
import JobBulkEditControls from "../JobBulkEditControls.vue";
import { JobPageStoreInjectionKey } from "../../../../../library/stores/JobPageStore";
import { JobBrowserStoreInjectionKey } from "../../../../../library/stores/JobBrowser";

jest.mock("@/library/rundeckService.ts", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: { on: jest.fn(), off: jest.fn(), emit: jest.fn() },
    rdBase: "http://localhost:4440",
  })),
}));

interface MountOptions {
  props?: Record<string, any>;
  bulkEditMode?: boolean;
}

const createJobPageStoreStub = (bulkEditMode = true) => ({
  bulkEditMode,
  jobAuthz: {},
  selectedJobs: [],
  uploadJobHref: () => "",
});

const createWrapper = async (
  options: MountOptions = {},
): Promise<VueWrapper<any>> => {
  const wrapper = mount(JobBulkEditControls, {
    props: {
      ...options.props,
    },
    global: {
      provide: {
        [JobPageStoreInjectionKey as symbol]: createJobPageStoreStub(
          options.bulkEditMode,
        ),
        [JobBrowserStoreInjectionKey as symbol]: {},
      },
      stubs: {
        UiSocket: true,
        JobListScmStatus: true,
        JobListScmActions: true,
        CreateNewJobButton: true,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

const findByTestId = (wrapper: VueWrapper<any>, testId: string) =>
  wrapper.find(`[data-testid="${testId}"]`);

describe("JobBulkEditControls", () => {
  // RUN-4966: <btn type="simple"> rendered "btn-simple" alone, which matches
  // no CSS rule — .btn-simple is only styled paired with a variant class
  // such as .btn-default.btn-simple.
  it("renders the select all button with the btn-default and btn-simple classes", async () => {
    const wrapper = await createWrapper();

    const selectAllButton = findByTestId(
      wrapper,
      "job-bulk-edit-select-all-button",
    );

    expect(selectAllButton.exists()).toBe(true);
    expect(selectAllButton.classes()).toContain("btn-default");
    expect(selectAllButton.classes()).toContain("btn-simple");
    expect(selectAllButton.classes()).toContain("btn-xs");
    expect(selectAllButton.classes()).toContain("btn-hover");
  });

  it("renders the select none button with the btn-default and btn-simple classes", async () => {
    const wrapper = await createWrapper();

    const selectNoneButton = findByTestId(
      wrapper,
      "job-bulk-edit-select-none-button",
    );

    expect(selectNoneButton.exists()).toBe(true);
    expect(selectNoneButton.classes()).toContain("btn-default");
    expect(selectNoneButton.classes()).toContain("btn-simple");
    expect(selectNoneButton.classes()).toContain("btn-xs");
    expect(selectNoneButton.classes()).toContain("btn-hover");
  });

  it("does not render the select all/none buttons when bulk edit mode is off", async () => {
    const wrapper = await createWrapper({ bulkEditMode: false });

    expect(
      findByTestId(wrapper, "job-bulk-edit-select-all-button").exists(),
    ).toBe(false);
    expect(
      findByTestId(wrapper, "job-bulk-edit-select-none-button").exists(),
    ).toBe(false);
  });
});
