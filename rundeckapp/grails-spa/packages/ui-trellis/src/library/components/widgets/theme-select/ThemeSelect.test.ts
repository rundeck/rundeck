import { shallowMount } from "@vue/test-utils";
import ThemeSelect from "./ThemeSelect.vue";
import { PrefersColorScheme, Theme } from "../../../stores/Theme";
import { RundeckContext } from "@rundeck/client";
import { EventBus } from "../../../utilities/vueEventBus";
import { RootStore } from "../../../stores/RootStore";
import { RundeckToken } from "../../../interfaces/rundeckWindow";

describe("ThemeSelect", () => {
  it("calls a method when an option is selected", async () => {
    const mockMethod = jest.fn(); // Create a mock function to track method calls
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
      rootStore: {
        theme: {
          setUserTheme: mockMethod,
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
    const wrapper = shallowMount(ThemeSelect);

    // Simulate selecting an option
    const optionIndex = 1;
    const option = wrapper.find(`option:nth-child(${optionIndex + 1})`);
    await option.setValue(true);

    // Assert that the method was called with the correct option
    expect(mockMethod).toHaveBeenCalledWith("light");
  });
});
