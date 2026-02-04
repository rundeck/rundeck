import { mount, flushPromises } from "@vue/test-utils";
import SettingsModal from "../SettingsModal.vue";
import mitt from "mitt";
import type { EventType, Emitter } from "mitt";

const mockSetUserTheme = jest.fn();
const mockGetPageUiMeta = jest.fn();
const mockCookies = {
  get: jest.fn(),
  set: jest.fn(),
};

let eventBus: Emitter<Record<EventType, any>>;

jest.mock("../../../../rundeckService", () => ({
  getRundeckContext: jest.fn(() => ({
    eventBus: eventBus,
    rootStore: {
      theme: {
        userPreferences: {
          theme: "dark",
        },
        setUserTheme: mockSetUserTheme,
      },
    },
  })),
}));

jest.mock("../services/pageUiMetaService", () => ({
  getPageUiMeta: () => mockGetPageUiMeta(),
}));

const mountSettingsModal = async () => {
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
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it("does not render overlay initially", async () => {
    const wrapper = await mountSettingsModal();
    expect(wrapper.find(".settings-modal-overlay").exists()).toBe(false);
  });

  it("opens modal when settings:open-modal event is emitted", async () => {
    const wrapper = await mountSettingsModal();

    eventBus.emit("settings:open-modal", "theme");
    await wrapper.vm.$nextTick();
    await flushPromises();

    expect(wrapper.find(".settings-modal-overlay").exists()).toBe(true);
    expect(wrapper.find(".settings-modal__tab--active").text()).toBe("settings.tabs.theme");
  });

  it("opens to specified tab via event parameter", async () => {
    const wrapper = await mountSettingsModal();

    eventBus.emit("settings:open-modal", "ui-early-access");
    await wrapper.vm.$nextTick();
    await flushPromises();

    const tabs = wrapper.findAll(".settings-modal__tab");
    expect(tabs[1].classes()).toContain("settings-modal__tab--active");
  });

  it("does not respond to events after unmount", async () => {
    const wrapper = await mountSettingsModal();
    wrapper.unmount();

    eventBus.emit("settings:open-modal", "theme");
    await flushPromises();

    expect(wrapper.find(".settings-modal-overlay").exists()).toBe(false);
  });

  it("closes modal when close button is clicked", async () => {
    const wrapper = await mountSettingsModal();

    eventBus.emit("settings:open-modal", "theme");
    await wrapper.vm.$nextTick();
    await flushPromises();

    await wrapper.find(".settings-modal__close").trigger("click");
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".settings-modal-overlay").exists()).toBe(false);
  });

  it("switches tabs when tab button is clicked", async () => {
    const wrapper = await mountSettingsModal();

    eventBus.emit("settings:open-modal", "theme");
    await wrapper.vm.$nextTick();
    await flushPromises();

    const tabs = wrapper.findAll(".settings-modal__tab");
    await tabs[1].trigger("click");
    await wrapper.vm.$nextTick();

    expect(tabs[1].classes()).toContain("settings-modal__tab--active");
    expect(wrapper.find(".settings-panel__title").text()).toBe("settings.uiEarlyAccess.title");
  });

  it("initializes theme select with correct value", async () => {
    const wrapper = await mountSettingsModal();

    eventBus.emit("settings:open-modal", "theme");
    await wrapper.vm.$nextTick();
    await flushPromises();

    const select = wrapper.find("select");
    expect(select.element.value).toBe("dark");
  });

  it("calls setUserTheme when theme is changed", async () => {
    const wrapper = await mountSettingsModal();

    eventBus.emit("settings:open-modal", "theme");
    await wrapper.vm.$nextTick();
    await flushPromises();

    await wrapper.find("select").setValue("light");
    await flushPromises();

    expect(mockSetUserTheme).toHaveBeenCalledWith("light");
  });

  it("initializes nextUi toggle based on page state", async () => {
    mockGetPageUiMeta.mockReturnValue({
      nextUiCapable: true,
      isNextUiPage: true,
    });

    const wrapper = await mountSettingsModal();

    eventBus.emit("settings:open-modal", "ui-early-access");
    await wrapper.vm.$nextTick();
    await flushPromises();

    const toggle = wrapper.find(".settings-toggle__input");
    expect((toggle.element as HTMLInputElement).checked).toBe(true);
  });

  it("sets cookie and reloads when nextUi toggle changes", async () => {
    const wrapper = await mountSettingsModal();

    eventBus.emit("settings:open-modal", "ui-early-access");
    await wrapper.vm.$nextTick();
    await flushPromises();

    await wrapper.find(".settings-toggle__input").setValue(true);
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
});
