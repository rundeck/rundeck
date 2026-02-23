import { mount, flushPromises } from "@vue/test-utils";
import mitt from "mitt";
import type { EventType, Emitter } from "mitt";

let eventBus: Emitter<Record<EventType, any>>;
const mockSetUserTheme = jest.fn();
const mockGetPageUiMeta = jest.fn();
const mockNotificationNotify = jest.fn();
const mockCookies = {
  get: jest.fn(),
  set: jest.fn(),
};

jest.mock("../../../../rundeckService", () => ({
  getRundeckContext: jest.fn(() => ({
    get eventBus() {
      return eventBus;
    },
    rootStore: {
      theme: {
        userPreferences: {
          theme: "dark",
        },
        get setUserTheme() {
          return mockSetUserTheme;
        },
      },
    },
  })),
}));

jest.mock("../services/pageUiMetaService", () => ({
  getPageUiMeta: () => mockGetPageUiMeta(),
}));

jest.mock("uiv", () => ({
  Notification: {
    get notify() {
      return mockNotificationNotify;
    },
  },
}));

import SettingsModal from "../SettingsModal.vue";

const createWrapper = async () => {
  const wrapper = mount(SettingsModal, {
    global: {
      mocks: {
        $t: (key: string) => key,
        $cookies: mockCookies,
      },
    },
  });
  await flushPromises();
  return wrapper;
};

describe("SettingsModal", () => {
  let originalReload: any;

  beforeAll(() => {
    originalReload = window.location.reload;
    Object.defineProperty(window, "location", {
      writable: true,
      value: { reload: jest.fn() },
    });
  });

  afterAll(() => {
    window.location.reload = originalReload;
  });

  beforeEach(() => {
    eventBus = mitt();
    mockGetPageUiMeta.mockReturnValue({
      nextUiCapable: true,
      isNextUiPage: false,
    });
    mockCookies.get.mockReturnValue(null);
    localStorage.clear();
  });

  afterEach(() => {
    eventBus.all.clear();
    jest.clearAllMocks();
    localStorage.clear();
  });

  describe("Modal visibility", () => {
    it("does not render overlay initially", async () => {
      const wrapper = await createWrapper();
      expect(wrapper.find('[data-testid="settings-modal-overlay"]').exists()).toBe(false);
    });

    it("opens modal when settings:open-modal event is emitted", async () => {
      const wrapper = await createWrapper();

      eventBus.emit("settings:open-modal", "theme");
      await wrapper.vm.$nextTick();
      await flushPromises();

      expect(wrapper.find('[data-testid="settings-modal-overlay"]').exists()).toBe(true);
      expect(wrapper.find('[data-testid="settings-tab-theme"]').classes()).toContain("settings-modal__tab--active");
    });

    it("opens to specified tab via event parameter", async () => {
      const wrapper = await createWrapper();

      eventBus.emit("settings:open-modal", "ui-early-access");
      await wrapper.vm.$nextTick();
      await flushPromises();

      expect(wrapper.find('[data-testid="settings-tab-ui-early-access"]').classes()).toContain("settings-modal__tab--active");
    });

    it("closes modal when close button is clicked", async () => {
      const wrapper = await createWrapper();

      eventBus.emit("settings:open-modal", "theme");
      await wrapper.vm.$nextTick();
      await flushPromises();

      await wrapper.find('[data-testid="settings-modal-close"]').trigger("click");
      await wrapper.vm.$nextTick();

      expect(wrapper.find('[data-testid="settings-modal-overlay"]').exists()).toBe(false);
    });

    it("closes modal when overlay is clicked", async () => {
      const wrapper = await createWrapper();

      eventBus.emit("settings:open-modal", "theme");
      await wrapper.vm.$nextTick();
      await flushPromises();

      await wrapper.find('[data-testid="settings-modal-overlay"]').trigger("click");
      await wrapper.vm.$nextTick();

      expect(wrapper.find('[data-testid="settings-modal-overlay"]').exists()).toBe(false);
    });

    it("does not respond to events after unmount", async () => {
      const wrapper = await createWrapper();
      wrapper.unmount();

      eventBus.emit("settings:open-modal", "theme");
      await flushPromises();

      expect(wrapper.find('[data-testid="settings-modal-overlay"]').exists()).toBe(false);
    });
  });

  describe("Tab switching", () => {
    it("switches tabs when tab button is clicked", async () => {
      const wrapper = await createWrapper();

      eventBus.emit("settings:open-modal", "theme");
      await wrapper.vm.$nextTick();
      await flushPromises();

      await wrapper.find('[data-testid="settings-tab-ui-early-access"]').trigger("click");
      await wrapper.vm.$nextTick();

      expect(wrapper.find('[data-testid="settings-tab-ui-early-access"]').classes()).toContain("settings-modal__tab--active");
      expect(wrapper.find('[data-testid="settings-panel-title"]').text()).toBe("settings.uiEarlyAccess.title");
    });

    it("displays theme panel when theme tab is active", async () => {
      const wrapper = await createWrapper();

      eventBus.emit("settings:open-modal", "theme");
      await wrapper.vm.$nextTick();
      await flushPromises();

      expect(wrapper.find('[data-testid="settings-panel-theme"]').exists()).toBe(true);
      expect(wrapper.find('[data-testid="settings-panel-ui-early-access"]').exists()).toBe(false);
    });

    it("displays ui-early-access panel when ui-early-access tab is active", async () => {
      const wrapper = await createWrapper();

      eventBus.emit("settings:open-modal", "ui-early-access");
      await wrapper.vm.$nextTick();
      await flushPromises();

      expect(wrapper.find('[data-testid="settings-panel-ui-early-access"]').exists()).toBe(true);
      expect(wrapper.find('[data-testid="settings-panel-theme"]').exists()).toBe(false);
    });
  });

  describe("Theme selection", () => {
    it("initializes theme select with correct value", async () => {
      const wrapper = await createWrapper();

      eventBus.emit("settings:open-modal", "theme");
      await wrapper.vm.$nextTick();
      await flushPromises();

      const select = wrapper.find('[data-testid="theme-select"]');
      expect((select.element as HTMLSelectElement).value).toBe("dark");
    });

    it("calls setUserTheme when theme is changed", async () => {
      const wrapper = await createWrapper();

      eventBus.emit("settings:open-modal", "theme");
      await wrapper.vm.$nextTick();
      await flushPromises();

      await wrapper.find('[data-testid="theme-select"]').setValue("light");
      await flushPromises();

      expect(mockSetUserTheme).toHaveBeenCalledWith("light");
    });
  });

  describe("NextUI toggle", () => {
    it("initializes nextUi toggle based on page state when isNextUiPage is true", async () => {
      mockGetPageUiMeta.mockReturnValue({
        nextUiCapable: true,
        isNextUiPage: true,
      });

      const wrapper = await createWrapper();

      eventBus.emit("settings:open-modal", "ui-early-access");
      await wrapper.vm.$nextTick();
      await flushPromises();

      const toggle = wrapper.find('[data-testid="nextui-toggle"]');
      expect((toggle.element as HTMLInputElement).checked).toBe(true);
    });

    it("initializes nextUi toggle to false when isNextUiPage is false and no cookie", async () => {
      mockGetPageUiMeta.mockReturnValue({
        nextUiCapable: true,
        isNextUiPage: false,
      });
      mockCookies.get.mockReturnValue(null);

      const wrapper = await createWrapper();

      eventBus.emit("settings:open-modal", "ui-early-access");
      await wrapper.vm.$nextTick();
      await flushPromises();

      const toggle = wrapper.find('[data-testid="nextui-toggle"]');
      expect((toggle.element as HTMLInputElement).checked).toBe(false);
    });

    it("sets cookie and reloads when nextUi toggle is enabled", async () => {
      const wrapper = await createWrapper();

      eventBus.emit("settings:open-modal", "ui-early-access");
      await wrapper.vm.$nextTick();
      await flushPromises();

      await wrapper.find('[data-testid="nextui-toggle"]').setValue(true);
      await flushPromises();

      expect(mockCookies.set).toHaveBeenCalledWith(
        "nextUi",
        "true",
        "1y",
        "/",
        "",
        false,
        "Strict",
      );
      expect(window.location.reload).toHaveBeenCalled();
    });

    it("sets cookie to false and reloads when nextUi toggle is disabled", async () => {
      mockGetPageUiMeta.mockReturnValue({
        nextUiCapable: true,
        isNextUiPage: true,
      });

      const wrapper = await createWrapper();

      eventBus.emit("settings:open-modal", "ui-early-access");
      await wrapper.vm.$nextTick();
      await flushPromises();

      await wrapper.find('[data-testid="nextui-toggle"]').setValue(false);
      await flushPromises();

      expect(mockCookies.set).toHaveBeenCalledWith(
        "nextUi",
        "false",
        "1y",
        "/",
        "",
        false,
        "Strict",
      );
      expect(window.location.reload).toHaveBeenCalled();
    });

    it("stores 'enabled' message in localStorage when nextUi is enabled", async () => {
      const wrapper = await createWrapper();

      eventBus.emit("settings:open-modal", "ui-early-access");
      await wrapper.vm.$nextTick();
      await flushPromises();

      await wrapper.find('[data-testid="nextui-toggle"]').setValue(true);
      await flushPromises();

      expect(localStorage.getItem("nextUiToastMessage")).toBe("enabled");
    });

    it("stores 'disabled' message in localStorage when nextUi is disabled", async () => {
      mockGetPageUiMeta.mockReturnValue({
        nextUiCapable: true,
        isNextUiPage: true,
      });

      const wrapper = await createWrapper();

      eventBus.emit("settings:open-modal", "ui-early-access");
      await wrapper.vm.$nextTick();
      await flushPromises();

      await wrapper.find('[data-testid="nextui-toggle"]').setValue(false);
      await flushPromises();

      expect(localStorage.getItem("nextUiToastMessage")).toBe("disabled");
    });
  });

  describe("Cookie-based initialization", () => {
    it("initializes nextUi toggle to true when cookie is 'true'", async () => {
      mockCookies.get.mockReturnValue("true");
      mockGetPageUiMeta.mockReturnValue({
        nextUiCapable: true,
        isNextUiPage: false,
      });

      const wrapper = await createWrapper();

      eventBus.emit("settings:open-modal", "ui-early-access");
      await wrapper.vm.$nextTick();
      await flushPromises();

      const toggle = wrapper.find('[data-testid="nextui-toggle"]');
      expect((toggle.element as HTMLInputElement).checked).toBe(true);
    });

    it("initializes nextUi toggle to false when cookie is 'false'", async () => {
      mockCookies.get.mockReturnValue("false");
      mockGetPageUiMeta.mockReturnValue({
        nextUiCapable: true,
        isNextUiPage: false,
      });

      const wrapper = await createWrapper();

      eventBus.emit("settings:open-modal", "ui-early-access");
      await wrapper.vm.$nextTick();
      await flushPromises();

      const toggle = wrapper.find('[data-testid="nextui-toggle"]');
      expect((toggle.element as HTMLInputElement).checked).toBe(false);
    });

    it("prioritizes cookie value over isNextUiPage when cookie is 'true'", async () => {
      mockCookies.get.mockReturnValue("true");
      mockGetPageUiMeta.mockReturnValue({
        nextUiCapable: true,
        isNextUiPage: false,
      });

      const wrapper = await createWrapper();

      eventBus.emit("settings:open-modal", "ui-early-access");
      await wrapper.vm.$nextTick();
      await flushPromises();

      const toggle = wrapper.find('[data-testid="nextui-toggle"]');
      expect((toggle.element as HTMLInputElement).checked).toBe(true);
    });

    it("uses isNextUiPage when cookie value is not set", async () => {
      mockCookies.get.mockReturnValue(null);
      mockGetPageUiMeta.mockReturnValue({
        nextUiCapable: true,
        isNextUiPage: true,
      });

      const wrapper = await createWrapper();

      eventBus.emit("settings:open-modal", "ui-early-access");
      await wrapper.vm.$nextTick();
      await flushPromises();

      const toggle = wrapper.find('[data-testid="nextui-toggle"]');
      expect((toggle.element as HTMLInputElement).checked).toBe(true);
    });
  });

  describe("Toast notifications", () => {
    it("shows toast notification with enabled message when localStorage has 'enabled'", async () => {
      localStorage.setItem("nextUiToastMessage", "enabled");

      await createWrapper();
      await flushPromises();

      expect(mockNotificationNotify).toHaveBeenCalledWith({
        type: "info",
        title: "",
        content: "settings.uiEarlyAccess.toast.enabled",
        dismissible: true,
      });
      expect(localStorage.getItem("nextUiToastMessage")).toBeNull();
    });

    it("shows toast notification with disabled message when localStorage has 'disabled'", async () => {
      localStorage.setItem("nextUiToastMessage", "disabled");

      await createWrapper();
      await flushPromises();

      expect(mockNotificationNotify).toHaveBeenCalledWith({
        type: "info",
        title: "",
        content: "settings.uiEarlyAccess.toast.disabled",
        dismissible: true,
      });
      expect(localStorage.getItem("nextUiToastMessage")).toBeNull();
    });

    it("does not show toast notification when localStorage has no message", async () => {
      await createWrapper();
      await flushPromises();

      expect(mockNotificationNotify).not.toHaveBeenCalled();
    });
  });
});
