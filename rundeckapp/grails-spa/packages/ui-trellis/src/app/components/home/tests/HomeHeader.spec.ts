import { mount, VueWrapper } from "@vue/test-utils";
import HomeHeader from "../HomeHeader.vue";

// Mock getSummary method
jest.mock("@/app/components/home/services/homeServices", () => ({
  getSummary: jest.fn().mockResolvedValue({
    execCount: 3,
    totalFailedCount: 1,
    recentProjects: ["Project1", "Project2"],
    recentUsers: ["User1", "User2"],
  }),
}));

jest.mock("@/library", () => ({
  getAppLinks: jest.fn().mockReturnValue({
    frameworkCreateProject: "/create-project-url", // Replace with the actual URL
  }),
  getRundeckContext: jest
    .fn()
    .mockReturnValue({ rdBase: "http://localhost:4440" }),
}));

const createWrapper = (props = {}) => {
  return mount(HomeHeader, {
    props: {
      createProjectAllowed: false,
      projectCount: 3,
      summaryRefresh: false,
      refreshDelay: 30000,
      ...props,
    },
    global: {
      mocks: {
        $t: (msg: string) => msg,
        $tc: (msg: string) => msg,
      },
    },
  });
};

describe("HomeHeader", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders project count and create project button when loadedProjectNames is true", async () => {
    const wrapper = createWrapper({ createProjectAllowed: true });
    await wrapper.setData({ loaded: true });

    expect(wrapper.find("#projectCountNumber").text()).toBe("3");
    expect(wrapper.find(".btn.btn-primary").exists()).toBe(true);
  });

  it("renders loading spinner when projects were not fully loaded", async () => {
    const wrapper = createWrapper();
    await wrapper.setProps({ projectCount: 0 });
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".loading-spinner").exists()).toBe(true);
  });

  it('renders "..." when there is no data about last day executions', async () => {
    const wrapper = createWrapper();

    expect(wrapper.find(".text-muted").text()).toBe("...");
  });

  it("renders summary information when loadedProjectNames is true and data is loaded", async () => {
    const wrapper = createWrapper();
    await wrapper.vm.$nextTick();

    expect(wrapper.find("#projectCountNumber").text()).toBe("3");
    expect(wrapper.find("#projectCount").text()).toBe(
      "3 page.home.section.project.title.plural",
    );
  });
});
