import { mount, VueWrapper } from "@vue/test-utils";
import HomeBrowserItem from "../HomeBrowserItem.vue";
import HomeActionsMenu from "../HomeActionsMenu.vue";
import UiSocket from "@/library/components/utils/UiSocket.vue";
import { Dropdown, Tooltip } from "uiv";

const dropdownStub = {
  name: "dropdown",
  template: "<div></div>",
};

jest.mock("@/library", () => ({
  getRundeckContext: jest
    .fn()
    .mockReturnValue({ rdBase: "http://localhost:4440" }),
}));

const projectData = {
  name: "example",
  label: "Example Project",
  description: "This is an example project",
  meta: [
    {
      name: "config",
      data: { executionsEnabled: true, scheduleEnabled: true },
    },
    {
      name: "message",
      data: {
        readmeDisplay: ["projectList"],
        motdDisplay: ["projectList"],
        readme: {
          motdHTML: "<p>MOTD</p>",
          readmeHTML: "<p>Readme</p>",
        },
      },
    },
  ],
  execCount: 3,
  userCount: 2,
  userSummary: ["User1", "User2"],
  failedCount: 1,
};

// Helper function to mount the component
const mountHomeBrowserItem = async (options = {}): Promise<VueWrapper<any>> => {
  const wrapper = mount(HomeBrowserItem, {
    props: {
      project: projectData,
      index: 0,
      loaded: true,
      ...options,
    },
    global: {
      mocks: {
        $t: (msg: string) => msg,
        $tc: (msg: string) => msg,
      },
      stubs: {
        HomeActionsMenu: dropdownStub,
      },
    },
    components: {
      Tooltip,
      UiSocket,
    },
  });

  // Wait for the next Vue tick to allow for asynchronous rendering
  await wrapper.vm.$nextTick();

  return wrapper;
};

describe("HomeBrowserItem", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders project details correctly", async () => {
    const wrapper = await mountHomeBrowserItem();

    // Assert that project details are rendered correctly
    expect(wrapper.find(".project_list_item_link").attributes("href")).toBe(
      "http://localhost:4440/?project=example",
    );
    expect(wrapper.find(".h5").text()).toBe("Example Project");
    expect(wrapper.find(".text-secondary").text()).toBe(
      "This is an example project",
    );
  });

  it("displays icon glyphicon-pause when executions are disabled", async () => {
    // Update project data to disable executions
    projectData.meta[0].data.executionsEnabled = false;
    const wrapper = await mountHomeBrowserItem();

    // Assert that glyphicon-pause icon is rendered
    expect(wrapper.find(".glyphicon-pause").exists()).toBe(true);
  });

  it("displays icon glyphicon-ban-circle when schedule is disabled", async () => {
    // Update project data to disable schedule
    projectData.meta[0].data.scheduleEnabled = false;
    const wrapper = await mountHomeBrowserItem();

    // Assert that glyphicon-ban-circle icon is rendered
    expect(wrapper.find(".glyphicon-ban-circle").exists()).toBe(true);
  });

  it("renders project label instead of name if label is present", async () => {
    // Update project data to include a label
    projectData.label = "Project Label";
    const wrapper = await mountHomeBrowserItem();

    // Assert that project label is rendered
    expect(wrapper.find(".h5").text()).toBe("Project Label");
  });

  it("renders readme and motd HTML when present", async () => {
    // Ensure project has readme and motd HTML content
    projectData.meta[1].data.readme = {
      motdHTML: "<p>MOTD HTML</p>",
      readmeHTML: "<p>Readme HTML</p>",
    };
    const wrapper = await mountHomeBrowserItem();

    // Assert that MOTD and Readme HTML content is rendered
    expect(wrapper.find('[data-test="motd"]').html()).toContain(
      "<p>MOTD HTML</p>",
    );
    expect(wrapper.find('[data-test="readme"]').html()).toContain(
      "<p>Readme HTML</p>",
    );
  });

  it("renders loading state when loaded is false", async () => {
    const wrapper = await mountHomeBrowserItem({
      loaded: false,
    });

    // Assert that loading state is rendered
    expect(wrapper.find('[data-test="actions-loading"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="activity-loading"]').exists()).toBe(true);
  });
});
