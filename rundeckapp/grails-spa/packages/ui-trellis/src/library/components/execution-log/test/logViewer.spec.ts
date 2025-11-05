import { shallowMount, VueWrapper, flushPromises } from "@vue/test-utils";
import { nextTick } from "vue";
import LogViewer from "../logViewer.vue";
import { EventBus } from "../../../utilities/vueEventBus";
import * as rundeckService from "../../../rundeckService";

// Mock the CancelToken
jest.mock("@esfx/canceltoken", () => ({
  CancelToken: {
    source: jest.fn(() => ({
      token: { signaled: false },
      cancel: jest.fn(),
    })),
  },
}));

// Mock getRundeckContext
jest.mock("../../../rundeckService", () => {
  const mockFn = jest.fn();
  return {
    getRundeckContext: jest.fn(() => ({
      rootStore: {
        theme: { theme: "dark" },
        executionOutputStore: {
          createOrGet: mockFn,
        },
      },
    })),
    __getMockCreateOrGet: () => mockFn,
  };
});

jest.mock("../../containers/drawer/Drawer.vue", () => ({
  name: "RdDrawer",
  props: ["visible"],
  template: "<div class='mock-drawer' v-if='visible'><slot></slot></div>",
}));

jest.mock("../../utils/UiSocket.vue", () => ({
  name: "UiSocket",
  template: "<div class='mock-ui-socket'></div>",
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
  let mockCreateOrGet: jest.Mock;
  let getItemSpy: jest.SpyInstance;
  let setItemSpy: jest.SpyInstance;
  let removeItemSpy: jest.SpyInstance;

  beforeEach(() => {
    // Get the mock function from the mocked module
    mockCreateOrGet = (rundeckService as any).__getMockCreateOrGet();
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
      completed: false,
      execCompleted: false,
      percentLoaded: 0,
      offset: 0,
      size: 0,
      error: null,
    };

    mockCreateOrGet.mockReturnValue(mockViewer);

    // Mock localStorage
    getItemSpy = jest.spyOn(Storage.prototype, "getItem").mockReturnValue(null);
    setItemSpy = jest
      .spyOn(Storage.prototype, "setItem")
      .mockImplementation(() => {});
    removeItemSpy = jest
      .spyOn(Storage.prototype, "removeItem")
      .mockImplementation(() => {});
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

  describe("Stats Display", () => {
    it("shows stats when enabled via config", async () => {
      const wrapper = createWrapper({
        config: { stats: true } as any,
      });
      await nextTick();
      expect(findByTestId(wrapper, "log-viewer-stats").exists()).toBe(true);
    });

    it("hides stats when disabled via config", async () => {
      const wrapper = createWrapper({
        config: { stats: false } as any,
      });
      await nextTick();
      expect(findByTestId(wrapper, "log-viewer-stats").exists()).toBe(false);
    });
  });

  describe("Theme Styling", () => {
    it("applies dark theme class via config", () => {
      const wrapper = createWrapper({ config: { theme: "dark" } as any });
      const logViewer = findByTestId(wrapper, "log-viewer");
      expect(logViewer.classes()).toContain("execution-log--dark");
    });

    it("applies light theme class via config", () => {
      const wrapper = createWrapper({ config: { theme: "light" } as any });
      const logViewer = findByTestId(wrapper, "log-viewer");
      expect(logViewer.classes()).toContain("execution-log--light");
    });

    it("applies none theme class via config", () => {
      const wrapper = createWrapper({ config: { theme: "none" } as any });
      const logViewer = findByTestId(wrapper, "log-viewer");
      expect(logViewer.classes()).toContain("execution-log--none");
    });
  });

  describe("Edge Cases", () => {
    it("handles undefined node prop", () => {
      const wrapper = createWrapper({ node: undefined });
      expect(findByTestId(wrapper, "log-viewer").exists()).toBe(true);
    });

    it("handles undefined stepCtx prop", () => {
      const wrapper = createWrapper({ stepCtx: undefined });
      expect(findByTestId(wrapper, "log-viewer").exists()).toBe(true);
    });

    it("handles undefined config prop", () => {
      const wrapper = createWrapper({ config: undefined });
      expect(findByTestId(wrapper, "log-viewer").exists()).toBe(true);
    });

    it("handles undefined trimOutput prop", () => {
      const wrapper = createWrapper({ trimOutput: undefined });
      expect(findByTestId(wrapper, "log-viewer").exists()).toBe(true);
    });

    it("handles undefined jumpToLine prop", () => {
      const wrapper = createWrapper({ jumpToLine: undefined });
      expect(findByTestId(wrapper, "log-viewer").exists()).toBe(true);
    });
  });

  describe("LocalStorage Integration", () => {
    it("does not access localStorage when useUserSettings is false", () => {
      createWrapper({ useUserSettings: false });
      expect(getItemSpy).not.toHaveBeenCalled();
    });

    it("accesses localStorage when useUserSettings is true", () => {
      createWrapper({ useUserSettings: true });
      expect(getItemSpy).toHaveBeenCalledWith("execution-viewer");
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
        const statsCheckbox = wrapper.find("#logview_stats");

        await statsCheckbox.setValue(true);
        await nextTick();

        expect(findByTestId(wrapper, "log-viewer-stats").exists()).toBe(true);

        await statsCheckbox.setValue(false);
        await nextTick();

        expect(findByTestId(wrapper, "log-viewer-stats").exists()).toBe(false);
      });
    });

    describe("Follow Button", () => {
      it("shows eye-slash icon when follow is disabled initially", () => {
        const wrapper = createWrapper({ showSettings: true, follow: false });
        const followBtn = findByTestId(wrapper, "log-viewer-follow-btn");

        expect(followBtn.find(".fa-eye-slash").exists()).toBe(true);
      });

      it("shows eye icon when follow is enabled initially", () => {
        const wrapper = createWrapper({ showSettings: true, follow: true });
        const followBtn = findByTestId(wrapper, "log-viewer-follow-btn");

        expect(followBtn.find(".fa-eye").exists()).toBe(true);
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
