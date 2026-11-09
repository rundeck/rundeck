import { shallowMount, VueWrapper, flushPromises } from "@vue/test-utils";
import { nextTick } from "vue";
import LogViewer from "../logViewer.vue";
import { EventBus } from "../../../utilities/vueEventBus";
import { mockStatsEntries } from "./mocks/logViewerEntries";

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

const findEventHandler = (eventBus: { on: jest.Mock }, eventName: string) => {
  const call = eventBus.on.mock.calls.find((args) => args[0] === eventName);
  return call ? call[1] : undefined;
};

interface MountOptions {
  props?: Record<string, unknown>;
}

/** Shape of the execution output store entry the viewer reads from */
interface MockViewer {
  init: jest.Mock;
  getOutput: jest.Mock;
  getEntriesFiltered: jest.Mock;
  entries: Array<{ node: string; log: string }>;
  completed: boolean;
  execCompleted: boolean;
  error?: string;
  size?: number;
  offset?: number;
}

// Setup function to mount the component with given props
const createWrapper = (options: MountOptions = {}): VueWrapper<any> => {
  return shallowMount(LogViewer, {
    props: {
      executionId: "test-exec-123",
      showStats: true,
      showSettings: true,
      follow: false,
      theme: "dark",
      maxLogSize: 3145728,
      useUserSettings: false,
      ...options.props,
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
        LogNodeChunk: {
          name: "LogNodeChunk",
          props: ["entries"],
          emits: ["line-select", "jumped", "follow-change", "content-size"],
          template: `
            <div data-testid="log-node-chunk">
              <div
                v-for="(entry, idx) in entries || []"
                :key="idx"
                :data-testid="'log-entry-' + entry.node"
              >
                {{ entry.log }}
              </div>
            </div>
          `,
        },
      },
    },
  });
};

describe("LogViewer", () => {
  let mockEventBus: typeof EventBus;
  let mockViewer: MockViewer;

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
    (
      window as unknown as { _rundeck: { eventBus: typeof EventBus } }
    )._rundeck = {
      eventBus: mockEventBus,
    };

    // Mock viewer
    mockViewer = {
      init: jest.fn().mockResolvedValue(undefined),
      getOutput: jest.fn().mockResolvedValue({}),
      getEntriesFiltered: jest.fn().mockReturnValue([]),
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
      const wrapper = createWrapper({ props: { showSettings: true } });
      expect(findByTestId(wrapper, "log-viewer-toolbar").exists()).toBe(true);
    });

    it("does not render toolbar when showSettings is false", () => {
      const wrapper = createWrapper({ props: { showSettings: false } });
      expect(findByTestId(wrapper, "log-viewer-toolbar").exists()).toBe(false);
    });
  });

  describe("Theme Styling", () => {
    it("applies theme class from config", () => {
      const wrapper = createWrapper({
        props: { config: { theme: "light" } },
      });
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
      mockViewer.completed = true;
      const wrapper = createWrapper();
      await flushPromises();

      expect(findByTestId(wrapper, "log-viewer-error-message").exists()).toBe(
        true,
      );
      expect(
        findByTestId(wrapper, "log-viewer-error-message").text(),
      ).toContain("Failed to load execution log");
    });

    it("displays whale emoji warning when log exceeds maxLogSize", async () => {
      mockViewer.size = 4000000;
      mockViewer.execCompleted = true;
      const wrapper = createWrapper({ props: { maxLogSize: 3145728 } });
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
        const wrapper = createWrapper({ props: { showSettings: true } });
        const settingsBtn = findByTestId(wrapper, "log-viewer-settings-btn");

        await settingsBtn.trigger("click");
        await nextTick();

        expect(
          findByTestId(wrapper, "log-viewer-settings-form").isVisible(),
        ).toBe(true);
      });

      it("changes theme class when user changes theme dropdown", async () => {
        const wrapper = createWrapper({
          props: {
            showSettings: true,
            useUserSettings: true,
          },
        });
        const themeSelect = findByTestId(wrapper, "log-viewer-theme-select");

        await themeSelect.setValue("light");
        await nextTick();

        expect(findByTestId(wrapper, "log-viewer").classes()).toContain(
          "execution-log--light",
        );
      });

      it("toggles stats visibility when user clicks Display Stats checkbox", async () => {
        const wrapper = createWrapper({ props: { showSettings: true } });
        const statsCheckbox = findByTestId(
          wrapper,
          "log-viewer-stats-checkbox",
        );

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
          props: {
            showSettings: true,
            useUserSettings: true,
          },
        });

        await findByTestId(wrapper, "log-viewer-settings-btn").trigger("click");
        await nextTick();

        const gutterCheckbox = findByTestId(
          wrapper,
          "log-viewer-gutter-checkbox",
        );
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
        const wrapper = createWrapper({
          props: { showSettings: true, follow: true },
        });
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
        const wrapper = createWrapper({
          props: { showSettings: true, follow: false },
        });
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
        mockViewer.entries = mockStatsEntries;
        mockViewer.offset = 56789;
        mockViewer.execCompleted = false;

        const wrapper = createWrapper({
          props: {
            showSettings: true,
            config: { stats: true },
          },
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
          props: {
            showSettings: true,
            config: { stats: true },
          },
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
        const wrapper = createWrapper({ props: { showSettings: true } });
        await flushPromises();

        expect(findByTestId(wrapper, "log-viewer-progress-bar").exists()).toBe(
          true,
        );
      });

      it("hides progress bar when execution is completed", async () => {
        mockViewer.execCompleted = true;
        mockViewer.completed = true;
        const wrapper = createWrapper({ props: { showSettings: true } });
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

  describe("Node view height", () => {
    it("bounds the scroller at 600px before the log content is measured", async () => {
      const wrapper = createWrapper({ props: { node: "node1" } });
      await flushPromises();

      expect(
        findByTestId(wrapper, "log-viewer-scroller").attributes("style"),
      ).toContain("height: 600px");
    });

    it("sizes the scroller to the log content when it is shorter than 600px", async () => {
      const wrapper = createWrapper({ props: { node: "node1" } });
      await flushPromises();

      await wrapper
        .findComponent({ name: "LogNodeChunk" })
        .vm.$emit("content-size", 120);
      await nextTick();

      expect(
        findByTestId(wrapper, "log-viewer-scroller").attributes("style"),
      ).toContain("height: 120px");
    });

    it("caps the scroller at 600px when the log content is taller", async () => {
      const wrapper = createWrapper({ props: { node: "node1" } });
      await flushPromises();

      await wrapper
        .findComponent({ name: "LogNodeChunk" })
        .vm.$emit("content-size", 5000);
      await nextTick();

      expect(
        findByTestId(wrapper, "log-viewer-scroller").attributes("style"),
      ).toContain("height: 600px");
    });

    it("does not set a scroller height outside the node view", async () => {
      const wrapper = createWrapper();
      await flushPromises();

      await wrapper
        .findComponent({ name: "LogNodeChunk" })
        .vm.$emit("content-size", 120);
      await nextTick();

      expect(
        findByTestId(wrapper, "log-viewer-scroller").attributes("style"),
      ).toBeUndefined();
    });
  });

  describe("Hide incomplete nodes", () => {
    it("renders hide incomplete checkbox in settings and keeps it disabled until it applies", async () => {
      const wrapper = createWrapper({ props: { showSettings: true } });
      await flushPromises();

      const checkbox = findByTestId(
        wrapper,
        "log-viewer-hide-incomplete-checkbox",
      );
      expect(checkbox.attributes("disabled")).toBeDefined();
    });

    it("hides log lines for incomplete nodes when hide state is published", async () => {
      mockViewer.entries = [
        { node: "runner", log: "skipped" },
        { node: "localhost", log: "ok" },
      ];
      const wrapper = createWrapper({ props: { showSettings: true } });
      await flushPromises();

      const stateHandler = findEventHandler(
        mockEventBus as unknown as { on: jest.Mock },
        "execution-hide-incomplete-nodes-state",
      );
      expect(stateHandler).toBeDefined();
      stateHandler({
        hide: true,
        applies: true,
        nodeNames: ["runner"],
      });
      await nextTick();

      expect(findByTestId(wrapper, "log-entry-runner").exists()).toBe(false);
      expect(findByTestId(wrapper, "log-entry-localhost").exists()).toBe(true);
      expect(findByTestId(wrapper, "log-entry-localhost").text()).toContain(
        "ok",
      );
    });

    it("emits toggle when user checks hide incomplete nodes", async () => {
      const wrapper = createWrapper({ props: { showSettings: true } });
      await flushPromises();

      const stateHandler = findEventHandler(
        mockEventBus as unknown as { on: jest.Mock },
        "execution-hide-incomplete-nodes-state",
      );
      stateHandler({ hide: false, applies: true, nodeNames: ["runner"] });
      await nextTick();

      await findByTestId(
        wrapper,
        "log-viewer-hide-incomplete-checkbox",
      ).setValue(true);
      await nextTick();

      expect(mockEventBus.emit).toHaveBeenCalledWith(
        "execution-hide-incomplete-nodes-toggle",
        true,
      );
    });
  });
});
