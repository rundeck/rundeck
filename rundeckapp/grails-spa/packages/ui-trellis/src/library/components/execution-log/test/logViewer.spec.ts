import { shallowMount, VueWrapper, flushPromises } from "@vue/test-utils";
import { nextTick } from "vue";
import LogViewer from "../logViewer.vue";
import { EventBus } from "../../../utilities/vueEventBus";

// Mock the CancelToken
jest.mock("@esfx/canceltoken", () => ({
  CancelToken: {
    source: jest.fn(() => ({
      token: { signaled: false },
      cancel: jest.fn(),
    })),
  },
}));

// Mock getRundeckContext - use let for hoisting
let mockCreateOrGet: jest.Mock;

jest.mock("../../../rundeckService", () => ({
  getRundeckContext: () => ({
    rootStore: {
      theme: { theme: "dark" },
      executionOutputStore: {
        get createOrGet() {
          return mockCreateOrGet;
        },
      },
    },
  }),
}));

// Helper function to find elements by testid
const findByTestId = (wrapper: VueWrapper<any>, testId: string) => {
  return wrapper.find(`[data-testid="${testId}"]`);
};

// Setup function to mount the component with given props
const createWrapper = (props = {}, options = {}): VueWrapper<any> => {
  return shallowMount(LogViewer, {
    props: {
      executionId: "test-exec-123",
      showStats: true,
      showSettings: true,
      follow: false,
      theme: "dark",
      maxLogSize: 3145728,
      useUserSettings: false,
      ...props,
    },
    global: {
      stubs: {
        RdDrawer: {
          template: "<div><slot></slot></div>",
        },
        UiSocket: true,
        ProgressBar: true,
        Btn: false,
        BtnGroup: false,
      },
      ...options,
    },
  });
};

describe("LogViewer", () => {
  let mockEventBus: typeof EventBus;
  let mockViewer: any;

  beforeEach(() => {
    // Initialize mock
    mockCreateOrGet = jest.fn();

    // Reset mocks
    jest.clearAllMocks();

    // Mock event bus
    mockEventBus = {
      on: jest.fn(),
      emit: jest.fn(),
      off: jest.fn(),
    } as unknown as typeof EventBus;

    // Mock window._rundeck
    (window as any)._rundeck = {
      eventBus: mockEventBus,
    };

    // Mock viewer
    mockViewer = {
      init: jest.fn().mockResolvedValue(undefined),
      getOutput: jest.fn().mockResolvedValue({}),
      entries: [],
      completed: true,
      execCompleted: true,
    };

    mockCreateOrGet.mockReturnValue(mockViewer);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe("Toolbar Visibility", () => {
    it("renders toolbar when showSettings is true", () => {
      const wrapper = createWrapper({ showSettings: true });
      expect(findByTestId(wrapper, "log-viewer-toolbar").exists()).toBe(true);
    });

    it("does not render toolbar when showSettings is false", () => {
      const wrapper = createWrapper({ showSettings: false });
      expect(findByTestId(wrapper, "log-viewer-toolbar").exists()).toBe(false);
    });
  });

  describe("Theme Styling", () => {
    it("applies theme class from config", () => {
      const wrapper = createWrapper({ config: { theme: "light" } as any });
      const logViewer = findByTestId(wrapper, "log-viewer");
      expect(logViewer.classes()).toContain("execution-log--light");
    });
  });

  describe("Warning and Error States", () => {
    it("displays no output message when execution completes with zero log lines", async () => {
      mockViewer.completed = true;
      mockViewer.entries = [];
      const wrapper = createWrapper();
      await flushPromises();

      expect(findByTestId(wrapper, "log-viewer-no-output").exists()).toBe(true);
      expect(findByTestId(wrapper, "log-viewer-no-output").text()).toContain(
        "No output",
      );
    });

    it("displays error message when viewer has an error", async () => {
      mockViewer.error = "Failed to load execution log";
      mockViewer.completed = false;
      const wrapper = createWrapper();
      await flushPromises();

      expect(findByTestId(wrapper, "log-viewer-error-message").exists()).toBe(
        true,
      );
      expect(findByTestId(wrapper, "log-viewer-error-message").text()).toContain(
        "Failed to load execution log",
      );
    });

    it("displays whale emoji warning when log exceeds maxLogSize", async () => {
      mockViewer.size = 4000000;
      mockViewer.execCompleted = true;
      const wrapper = createWrapper({ maxLogSize: 3145728 });
      await flushPromises();

      const warning = findByTestId(wrapper, "log-viewer-oversize-warning");
      expect(warning.exists()).toBe(true);
      expect(warning.text()).toContain("🐋");
      expect(warning.text()).toContain("MiB is a whale of a log!");
    });
  });

  describe("User Interactions", () => {
    describe("Settings Drawer", () => {
      it("opens settings drawer when user clicks Settings button", async () => {
        const wrapper = createWrapper({ showSettings: true });
        const settingsBtn = findByTestId(wrapper, "log-viewer-settings-btn");

        await settingsBtn.trigger("click");
        await nextTick();

        expect(
          findByTestId(wrapper, "log-viewer-settings-form").isVisible(),
        ).toBe(true);
      });

      it("changes theme class when user changes theme dropdown", async () => {
        const wrapper = createWrapper({
          showSettings: true,
          useUserSettings: true,
        });
        const themeSelect = findByTestId(wrapper, "log-viewer-theme-select");

        await themeSelect.setValue("light");
        await nextTick();

        expect(findByTestId(wrapper, "log-viewer").classes()).toContain(
          "execution-log--light",
        );
      });

      it("toggles stats visibility when user clicks Display Stats checkbox", async () => {
        const wrapper = createWrapper({ showSettings: true });
        const statsCheckbox = findByTestId(wrapper, "log-viewer-stats-checkbox");

        await statsCheckbox.setValue(true);
        await nextTick();

        expect(findByTestId(wrapper, "log-viewer-stats").exists()).toBe(true);

        await statsCheckbox.setValue(false);
        await nextTick();

        expect(findByTestId(wrapper, "log-viewer-stats").exists()).toBe(false);
      });

      it("persists settings to localStorage when drawer is open and settings change", async () => {
        const setItemSpy = jest.spyOn(Storage.prototype, "setItem");
        const wrapper = createWrapper({
          showSettings: true,
          useUserSettings: true,
        });

        await findByTestId(wrapper, "log-viewer-settings-btn").trigger("click");
        await nextTick();

        const gutterCheckbox = wrapper.find("#logview_gutter");
        await gutterCheckbox.setValue(false);
        await nextTick();

        expect(setItemSpy).toHaveBeenCalledWith(
          "execution-viewer",
          expect.stringContaining('"gutter":false'),
        );

        setItemSpy.mockRestore();
      });
    });

    describe("Follow Button", () => {
      it("shows eye icon when follow is enabled and toggles to eye-slash when clicked", async () => {
        mockViewer.execCompleted = false;
        const wrapper = createWrapper({ showSettings: true, follow: true });
        await flushPromises();
        const followIcon = findByTestId(wrapper, "log-viewer-follow-icon");

        expect(followIcon.classes()).toContain("fa-eye");
        expect(followIcon.classes()).not.toContain("fa-eye-slash");

        await findByTestId(wrapper, "log-viewer-follow-btn").trigger("click");
        await nextTick();

        expect(followIcon.classes()).toContain("fa-eye-slash");
        expect(followIcon.classes()).not.toContain("fa-eye");
      });

      it("shows eye-slash icon when follow is disabled and toggles to eye when clicked", async () => {
        const wrapper = createWrapper({ showSettings: true, follow: false });
        await flushPromises();
        const followIcon = findByTestId(wrapper, "log-viewer-follow-icon");

        expect(followIcon.classes()).toContain("fa-eye-slash");
        expect(followIcon.classes()).not.toContain("fa-eye");

        await findByTestId(wrapper, "log-viewer-follow-btn").trigger("click");
        await nextTick();

        expect(followIcon.classes()).toContain("fa-eye");
        expect(followIcon.classes()).not.toContain("fa-eye-slash");
      });
    });

    describe("Stats Content", () => {
      it("displays follow status, line count, size, and total time with correct formatting", async () => {
        mockViewer.entries = [
          { node: "node1", log: "line 1" },
          { node: "node1", log: "line 2" },
          { node: "node2", log: "line 3" },
        ];
        mockViewer.offset = 56789;
        mockViewer.execCompleted = false;

        const wrapper = createWrapper({
          showSettings: true,
          config: { stats: true } as any,
        });
        await flushPromises();

        const statsText = findByTestId(wrapper, "log-viewer-stats").text();

        expect(statsText).toContain("Following:true");
        expect(statsText).toContain("Lines:3");
        expect(statsText).toContain("Size:56789b");
        expect(statsText).toMatch(/TotalTime:\d+(\.\d+)?s/);
      });

      it("formats line count with commas for large numbers", async () => {
        const largeMockEntries = Array.from({ length: 12345 }, (_, i) => ({
          node: "node1",
          log: `line ${i}`,
        }));
        mockViewer.entries = largeMockEntries;
        mockViewer.offset = 100000;
        mockViewer.execCompleted = false;

        const wrapper = createWrapper({
          showSettings: true,
          config: { stats: true } as any,
        });
        await flushPromises();

        const statsText = findByTestId(wrapper, "log-viewer-stats").text();

        expect(statsText).toContain("Lines:12,345");
      });
    });

    describe("Progress Bar", () => {
      it("displays progress bar when execution is not completed", async () => {
        mockViewer.execCompleted = false;
        mockViewer.completed = false;
        const wrapper = createWrapper({ showSettings: true });
        await flushPromises();

        expect(findByTestId(wrapper, "log-viewer-progress-bar").exists()).toBe(
          true,
        );
      });

      it("hides progress bar when execution is completed", async () => {
        mockViewer.execCompleted = true;
        mockViewer.completed = true;
        const wrapper = createWrapper({ showSettings: true });
        await flushPromises();

        expect(findByTestId(wrapper, "log-viewer-progress-bar").exists()).toBe(
          false,
        );
      });
    });

    describe("Line Selection", () => {
      it("emits line-select event when LogNodeChunk emits line-select", async () => {
        const wrapper = createWrapper();
        const logNodeChunk = wrapper.findComponent({ name: "LogNodeChunk" });

        await logNodeChunk.vm.$emit("line-select", 42);

        expect(wrapper.emitted("line-select")).toBeTruthy();
        expect(wrapper.emitted("line-select")?.[0]).toEqual([42]);
      });

      it("emits line-deselect when user selects the same line twice", async () => {
        const wrapper = createWrapper();
        const logNodeChunk = wrapper.findComponent({ name: "LogNodeChunk" });

        await logNodeChunk.vm.$emit("line-select", 42);
        await logNodeChunk.vm.$emit("line-select", 42);

        expect(wrapper.emitted("line-deselect")).toBeTruthy();
        expect(wrapper.emitted("line-deselect")?.[0]).toEqual([42]);
      });
    });
  });
});
