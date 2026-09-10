import { mount } from "@vue/test-utils";
import RundeckInfo from "./RundeckInfo.vue";

jest.mock("../../../rundeckService", () => ({
  url: jest.fn().mockReturnValue("http://localhost"),
}));

const createWrapper = () =>
  mount(RundeckInfo, {
    props: {
      appInfo: { title: "Rundeck", logocss: "some-css" },
      version: {
        number: "1.0.0",
        tag: "stable",
        name: "Erebus",
        color: "red",
        icon: "glass",
        edition: "Community",
      },
    },
    global: {
      mocks: {
        $t: (message: string) =>
          message === "page.home.link.aria.label" ? "Home" : message,
      },
      stubs: {
        UiSocket: {
          template: "<div><slot /></div>",
        },
      },
    },
  });

describe("RundeckInfo accessibility", () => {
  it("uses the localized home label for the welcome link", () => {
    const wrapper = createWrapper();

    expect(
      wrapper.find('[data-testid="welcome-link"]').attributes("aria-label"),
    ).toBe("Home");
  });
});
