import { mount } from "@vue/test-utils";
import SettingsCogButton from "../SettingsCogButton.vue";

const mockEmit = jest.fn();

jest.mock("../../../../rundeckService", () => ({
  getRundeckContext: jest.fn(() => ({
    eventBus: {
      emit: mockEmit,
      on: jest.fn(),
      off: jest.fn(),
    },
  })),
}));

const mountSettingsCogButton = async () => {
  return mount(SettingsCogButton, {
    global: {
      mocks: {
        $t: (key: string) => key,
      },
    },
  });
};

describe("SettingsCogButton", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders a button with settings icon", async () => {
    const wrapper = await mountSettingsCogButton();

    const button = wrapper.find("button");
    expect(button.exists()).toBe(true);
    expect(button.classes()).toContain("settings-bar__button");

    const icon = button.find("i.fas.fa-cog");
    expect(icon.exists()).toBe(true);
  });

  it("has correct title attribute", async () => {
    const wrapper = await mountSettingsCogButton();
    const button = wrapper.find("button");
    expect(button.attributes("title")).toBe("settings.theme.title");
  });

  it("emits settings:open-modal event with theme tab when clicked", async () => {
    const wrapper = await mountSettingsCogButton();
    const button = wrapper.find("button");

    await button.trigger("click");

    expect(mockEmit).toHaveBeenCalledWith("settings:open-modal", "theme");
    expect(mockEmit).toHaveBeenCalledTimes(1);
  });
});
