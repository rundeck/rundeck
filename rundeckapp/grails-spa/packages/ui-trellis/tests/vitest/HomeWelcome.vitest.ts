import { shallowMount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import HomeWelcome from "../../src/app/components/home/HomeWelcome.vue";
import messages from "../../src/app/utilities/locales/en_US.js";

vi.mock("@/library", () => ({
  getRundeckContext: vi.fn().mockReturnValue({
    rdBase: "http://localhost:4440",
  }),
}));

const VMarkdownViewStub = {
  name: "VMarkdownView",
  props: ["content"],
  template: "<div>{{ content }}</div>",
};

describe("HomeWelcome", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders component content with a mocked Rundeck context", () => {
    const wrapper = shallowMount(HomeWelcome, {
      props: {
        appTitle: "Test App",
        buildIdent: "1.0.0",
        logoImage: "test-logo.png",
        helpLinkUrl: "/help",
      },
      global: {
        mocks: {
          $t: (baseString: string, replacements?: string[]) => {
            const message = messages[baseString];
            return replacements
              ? message.replace(/{(\d+)}/g, (_, index: string) =>
                  replacements[Number.parseInt(index, 10)],
                )
              : message;
          },
        },
        stubs: {
          VMarkdownView: VMarkdownViewStub,
          FirstRun: true,
        },
      },
    });

    expect(wrapper.find(".card-title").text()).toBe("Welcome to Test App 1.0.0");
    expect(wrapper.find("img").attributes("src")).toBe("/assets/test-logo.png");
    expect(wrapper.findComponent(VMarkdownViewStub).props("content")).toContain(
      "/help",
    );
  });
});
