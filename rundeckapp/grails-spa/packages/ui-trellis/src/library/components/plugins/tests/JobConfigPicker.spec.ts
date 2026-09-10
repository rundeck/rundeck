import { flushPromises, mount, VueWrapper } from "@vue/test-utils";
import JobConfigPicker from "../JobConfigPicker.vue";

jest.mock("../../../modules/rundeckClient", () => ({
  client: {
    projectList: jest.fn().mockResolvedValue([{ name: "Project A" }]),
    jobList: jest.fn().mockResolvedValue([]),
  },
}));

interface MountOptions {
  props?: Record<string, unknown>;
}

const createWrapper = async (
  options: MountOptions = {},
): Promise<VueWrapper<any>> => {
  const wrapper = mount(JobConfigPicker, {
    props: {
      ...options.props,
    },
    global: {
      stubs: {
        modal: {
          template: "<div><slot /><slot name='footer' /></div>",
        },
        btn: {
          template: "<button><slot /></button>",
        },
      },
    },
  });
  await flushPromises();
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("JobConfigPicker", () => {
  beforeEach(() => {
    window._rundeck = {
      projectName: "",
    } as any;
  });

  afterEach(() => {
    jest.clearAllMocks();
    Reflect.deleteProperty(window, "_rundeck");
  });

  it("associates the project label with the project picker select", async () => {
    const wrapper = await createWrapper();

    expect(
      wrapper
        .find('[data-testid="job-config-picker-project-label"]')
        .attributes("for"),
    ).toBe(wrapper.find('[data-testid="project-select"]').attributes("id"));
  });

  it("generates a different project picker id for each instance", async () => {
    const firstWrapper = await createWrapper();
    const secondWrapper = await createWrapper();

    expect(
      firstWrapper.find('[data-testid="project-select"]').attributes("id"),
    ).not.toBe(
      secondWrapper.find('[data-testid="project-select"]').attributes("id"),
    );
  });
});
