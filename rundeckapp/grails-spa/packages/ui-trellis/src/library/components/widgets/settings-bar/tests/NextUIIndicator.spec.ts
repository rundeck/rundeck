import { mount } from "@vue/test-utils";
import NextUIIndicator from "../NextUIIndicator.vue";

const mockEmit = jest.fn();
const mockGetPageUiMeta = jest.fn();

jest.mock("../../../../rundeckService", () => ({
  getRundeckContext: jest.fn(() => ({
    eventBus: {
      emit: mockEmit,
      on: jest.fn(),
      off: jest.fn(),
    },
  })),
}));

jest.mock("../services/pageUiMetaService", () => ({
  getPageUiMeta: () => mockGetPageUiMeta(),
}));

const mountNextUIIndicator = async () => {
  const wrapper = mount(NextUIIndicator, {
    global: {
      mocks: {
        $t: (key: string) => key,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("NextUIIndicator", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("does not render when page is not nextUi capable", async () => {
    mockGetPageUiMeta.mockReturnValue({
      nextUiCapable: false,
      isNextUiPage: false,
    });

    const wrapper = await mountNextUIIndicator();
    const button = wrapper.find("button");
    expect(button.exists()).toBe(false);
  });

  it("renders when page is nextUi capable", async () => {
    mockGetPageUiMeta.mockReturnValue({
      nextUiCapable: true,
      isNextUiPage: false,
    });

    const wrapper = await mountNextUIIndicator();
    const button = wrapper.find("button");
    expect(button.exists()).toBe(true);
    expect(button.classes()).toContain("settings-bar__button");
    expect(button.classes()).toContain("settings-bar__nextui-indicator");
  });

  it("displays 'available' text when nextUi is capable but not active", async () => {
    mockGetPageUiMeta.mockReturnValue({
      nextUiCapable: true,
      isNextUiPage: false,
    });

    const wrapper = await mountNextUIIndicator();
    const text = wrapper.find(".settings-bar__nextui-text");
    expect(text.text()).toBe("settings.nextUi.available");
  });

  it("displays 'enabled' text when on a nextUi page", async () => {
    mockGetPageUiMeta.mockReturnValue({
      nextUiCapable: true,
      isNextUiPage: true,
    });

    const wrapper = await mountNextUIIndicator();
    const text = wrapper.find(".settings-bar__nextui-text");
    expect(text.text()).toBe("settings.nextUi.enabled");
  });

  it("has correct title attribute", async () => {
    mockGetPageUiMeta.mockReturnValue({
      nextUiCapable: true,
      isNextUiPage: false,
    });

    const wrapper = await mountNextUIIndicator();
    const button = wrapper.find("button");
    expect(button.attributes("title")).toBe("settings.nextUi.indicatorTitle");
  });

  it("emits settings:open-modal event with ui-early-access tab when clicked", async () => {
    mockGetPageUiMeta.mockReturnValue({
      nextUiCapable: true,
      isNextUiPage: false,
    });

    const wrapper = await mountNextUIIndicator();
    const button = wrapper.find("button");

    await button.trigger("click");

    expect(mockEmit).toHaveBeenCalledWith(
      "settings:open-modal",
      "ui-early-access",
    );
    expect(mockEmit).toHaveBeenCalledTimes(1);
  });
});
