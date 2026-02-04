import { shallowMount, VueWrapper } from "@vue/test-utils";
import SettingsBar from "../SettingsBar.vue";
import { PrefersColorScheme, Theme } from "../../../../stores/Theme";
import { EventBus } from "../../../../utilities/vueEventBus";
import { RootStore } from "../../../../stores/RootStore";
import { RundeckToken } from "../../../../interfaces/rundeckWindow";

// Mock loadJsonData
jest.mock("../../../../../app/utilities/loadJsonData", () => ({
  loadJsonData: jest.fn((id: string) => {
    if (id === "pageUiMeta") {
      return {
        uiType: "current",
        nextUiCapable: true,
        nextUiSystemEnabled: true,
      };
    }
    return null;
  }),
}));

describe("SettingsBar", () => {
  let wrapper: VueWrapper<any>;
  const mockSetUserTheme = jest.fn();
  const mockCookiesGet = jest.fn();
  const mockCookiesSet = jest.fn();

  beforeEach(() => {
    // Reset mocks
    mockSetUserTheme.mockClear();
    mockCookiesGet.mockClear();
    mockCookiesSet.mockClear();
    mockCookiesGet.mockReturnValue(null);

    // Setup window._rundeck
    window._rundeck = {
      eventBus: jest.fn() as unknown as typeof EventBus,
      rdBase: "mock_rdBase",
      apiVersion: "mock_apiVersion",
      projectName: "mock_projectName",
      activeTour: "mock_activeTour",
      activeTourStep: "mock_activeTourStep",
      appMeta: {},
      token: {} as RundeckToken,
      tokens: {},
      navbar: { items: [] as any },
      feature: {},
      data: {},
      rundeckClient: {} as any,
      appLinks: {
        help: "https://help.rundeck.com",
      },
      rootStore: {
        theme: {
          setUserTheme: mockSetUserTheme,
          theme: {} as Theme,
          handleSystemChange: jest.fn(),
          setTheme: jest.fn(),
          loadConfig: jest.fn(),
          saveConfig: jest.fn(),
          userPreferences: {
            theme: Theme.system,
          },
          prefersColorScheme: PrefersColorScheme.dark,
          themeMediaQuery: jest.fn() as unknown as MediaQueryList,
        },
      } as unknown as RootStore,
    };
  });

  const createWrapper = (props = {}) => {
    return shallowMount(SettingsBar, {
      props,
      global: {
        mocks: {
          $t: (key: string) => key,
          $cookies: {
            get: mockCookiesGet,
            set: mockCookiesSet,
          },
        },
      },
    });
  };

  it("renders help button with label", () => {
    wrapper = createWrapper();
    const helpButton = wrapper.find(".settings-bar__button");
    expect(helpButton.exists()).toBe(true);
    expect(helpButton.text()).toContain("settings.help.label");
  });

  it("renders settings button with cog icon", () => {
    wrapper = createWrapper();
    const buttons = wrapper.findAll(".settings-bar__button");
    const settingsButton = buttons[1];
    expect(settingsButton.find(".fa-cog").exists()).toBe(true);
  });

  it("shows NextUI indicator when system enabled and page capable", () => {
    wrapper = createWrapper();
    const indicator = wrapper.find(".settings-bar__nextui-indicator");
    expect(indicator.exists()).toBe(true);
    expect(indicator.text()).toContain("settings.nextUi");
  });

  it("opens modal when settings button is clicked", async () => {
    wrapper = createWrapper();
    
    // Modal should not exist initially
    expect(wrapper.find(".settings-modal").exists()).toBe(false);
    
    // Click settings button
    const settingsButton = wrapper.findAll(".settings-bar__button")[1];
    await settingsButton.trigger("click");
    
    // Modal should now be visible
    expect(wrapper.find(".settings-modal").exists()).toBe(true);
  });

  it("shows Theme tab content when clicking settings button", async () => {
    wrapper = createWrapper();
    const settingsButton = wrapper.findAll(".settings-bar__button")[1];
    await settingsButton.trigger("click");
    
    // Theme tab should be active (has active class)
    const tabs = wrapper.findAll(".settings-modal__tab");
    expect(tabs[0].classes()).toContain("settings-modal__tab--active");
    
    // Theme panel content should be visible
    expect(wrapper.find(".settings-panel__title").text()).toBe("settings.theme.title");
  });

  it("shows UI Early Access tab content when clicking NextUI indicator", async () => {
    wrapper = createWrapper();
    const indicator = wrapper.find(".settings-bar__nextui-indicator");
    await indicator.trigger("click");
    
    // UI Early Access tab should be active
    const tabs = wrapper.findAll(".settings-modal__tab");
    expect(tabs[1].classes()).toContain("settings-modal__tab--active");
    
    // UI Early Access panel content should be visible
    expect(wrapper.find(".settings-panel__title").text()).toBe("settings.uiEarlyAccess.title");
  });

  it("closes modal when close button is clicked", async () => {
    wrapper = createWrapper();
    
    // Open modal first
    const settingsButton = wrapper.findAll(".settings-bar__button")[1];
    await settingsButton.trigger("click");
    expect(wrapper.find(".settings-modal").exists()).toBe(true);

    // Close modal
    const closeButton = wrapper.find(".settings-modal__close");
    await closeButton.trigger("click");
    
    // Modal should be hidden
    expect(wrapper.find(".settings-modal").exists()).toBe(false);
  });

  it("switches to UI Early Access tab when tab button is clicked", async () => {
    wrapper = createWrapper();
    
    // Open modal with Theme tab
    const settingsButton = wrapper.findAll(".settings-bar__button")[1];
    await settingsButton.trigger("click");

    // Click UI Early Access tab
    const tabs = wrapper.findAll(".settings-modal__tab");
    await tabs[1].trigger("click");
    
    // UI Early Access tab should now be active
    expect(tabs[1].classes()).toContain("settings-modal__tab--active");
    expect(wrapper.find(".settings-panel__title").text()).toBe("settings.uiEarlyAccess.title");
  });

  it("calls setUserTheme when theme dropdown is changed", async () => {
    wrapper = createWrapper();
    
    // Open modal
    const settingsButton = wrapper.findAll(".settings-bar__button")[1];
    await settingsButton.trigger("click");

    // Find theme select and change value
    const themeSelect = wrapper.find("select");
    await themeSelect.setValue("dark");

    expect(mockSetUserTheme).toHaveBeenCalledWith("dark");
  });

  it("opens help URL when help button is clicked", async () => {
    const mockOpen = jest.fn();
    window.open = mockOpen;

    wrapper = createWrapper({ helpUrl: "https://custom-help.com" });
    const helpButton = wrapper.find(".settings-bar__button");
    await helpButton.trigger("click");

    expect(mockOpen).toHaveBeenCalledWith("https://custom-help.com", "_blank");
  });

  it("sets cookie and reloads when NextUI toggle is changed", async () => {
    // Mock window.location.reload
    const mockReload = jest.fn();
    Object.defineProperty(window, "location", {
      value: { reload: mockReload },
      writable: true,
    });

    wrapper = createWrapper();
    
    // Open modal with UI Early Access tab
    const indicator = wrapper.find(".settings-bar__nextui-indicator");
    await indicator.trigger("click");

    // Find and click the toggle checkbox
    const toggle = wrapper.find(".settings-toggle__input");
    await toggle.setValue(true);

    // Should set cookie and reload
    expect(mockCookiesSet).toHaveBeenCalledWith(
      "nextUi",
      "true",
      "1y",
      "/",
      "",
      false,
      "Strict"
    );
    expect(mockReload).toHaveBeenCalled();
  });

  it("closes modal when clicking overlay background", async () => {
    wrapper = createWrapper();
    
    // Open modal
    const settingsButton = wrapper.findAll(".settings-bar__button")[1];
    await settingsButton.trigger("click");
    expect(wrapper.find(".settings-modal").exists()).toBe(true);

    // Click overlay (outside modal)
    const overlay = wrapper.find(".settings-modal-overlay");
    await overlay.trigger("click");
    
    // Modal should be closed
    expect(wrapper.find(".settings-modal").exists()).toBe(false);
  });
});
