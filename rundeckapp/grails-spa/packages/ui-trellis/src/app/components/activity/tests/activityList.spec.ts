import { mount, VueWrapper } from "@vue/test-utils";
import ActivityList from "../activityList.vue";
import ActivityFilter from "../activityFilter.vue";
import { RundeckContext } from "../../../../library";
import { EventBus } from "../../../../library";
import { RundeckBrowser } from "@rundeck/client";
import { RootStore } from "../../../../library/stores/RootStore";
import { NavItem } from "../../../../library/stores/NavBar";

// Mocking necessary services and modules
jest.mock("@rundeck/client", () => {
  return {
    RundeckBrowser: jest.fn().mockImplementation(() => ({
      executionBulkDelete: jest.fn().mockResolvedValue({ allsuccessful: true }),
      executionListRunning: jest.fn().mockResolvedValue({ executions: [] }),
    })),
  };
});
jest.mock("axios");
jest.mock("../../../../library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({ projectName: "test" }),
}));
const mockEventBus = {
  on: jest.fn(),
  emit: jest.fn(),
  off: jest.fn(),
  all: new Map(),
};
type ActivityListInstance = InstanceType<typeof ActivityList>;
const mountActivityList = async (props = {}) => {
  const wrapper = mount(ActivityList, {
    props: {
      pagination: {
        total: 10,
        offset: 0,
        max: 10,
      },
      reports: [],
      query: {},
      eventBus: mockEventBus,
      running: {
        executions: [],
      },
      ...props,
    },
    global: {
      components: {
        ActivityFilter,
      },
      stubs: {
        ActivityFilter: true,
        Modal: {
          template: "<div><slot></slot></div>",
        },
        ProgressBar: {
          template: "<div><slot></slot></div>",
        },
        btn: true,
      },
      mocks: {
        $t: (msg) => msg,
        $tc: (msg) => msg,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper as VueWrapper<ActivityListInstance>;
};
declare global {
  interface Window {
    _rundeck: RundeckContext;
  }
}
beforeAll(() => {
  jest.useFakeTimers();
  window._rundeck = {
    eventBus: mockEventBus as unknown as typeof EventBus,
    rdBase: "mocked-rdBase",
    apiVersion: "37",
    projectName: "test",
    activeTour: "",
    activeTourStep: "",
    appMeta: {},
    token: {
      TOKEN: "mocked-token",
      URI: "mocked-uri",
    },
    tokens: {
      default: {
        TOKEN: "mocked-token",
        URI: "mocked-uri",
      },
    },
    rundeckClient: new RundeckBrowser("mocked-token", "mocked-uri"),
    data: {
      jobslistDateFormatMoment: "MM/DD/YYYY",
      projectAdminAuth: true,
      deleteExecAuth: true,
      activityUrl: "/activity",
      nowrunningUrl: "/nowrunning",
      bulkDeleteUrl: "/bulkDelete",
      activityPageHref: "/activityPage",
      sinceUpdatedUrl: "/sinceUpdated",
      autorefreshms: 5000,
      pagination: {
        max: 10,
      },
      filterOpts: {},
      query: {},
      runningOpts: {
        allowAutoRefresh: true,
        loadRunning: true,
      },
      viewOpts: {
        showBulkDelete: true,
      },
    },
    feature: {
      exampleFeature: {
        enabled: true,
      },
    },
    navbar: {
      items: [] as Array<NavItem>,
    },
    rootStore: {} as RootStore,
  } as RundeckContext;
});
afterAll(() => {
  jest.useRealTimers();
});
afterEach(() => {
  jest.clearAllMocks();
});
describe("ActivityList", () => {
  it("renders the component correctly", async () => {
    const wrapper = await mountActivityList();
    expect(wrapper.exists()).toBe(true);
  });
  it("opens and closes the filter modal", async () => {
    const wrapper = await mountActivityList();
    const filterButton = wrapper.find(
      '[data-test-id="activity-list-filter-button"]',
    );
    await filterButton.trigger("click");
    expect(wrapper.findComponent(ActivityFilter).exists()).toBe(true);
  });
});
