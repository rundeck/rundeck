import { shallowMount, VueWrapper } from "@vue/test-utils";
import PluginDetails from "../PluginDetails.vue";

const createWrapper = async (propsData = {}): Promise<VueWrapper<any>> => {
  return shallowMount(PluginDetails, {
    props: {
      allowHtml: true,
      description: "First line\nRemaining lines with some more content.",
      cutoffMarker: "",
      ...propsData,
    },
    global: {
      mocks: {
        $t: (msg: string) => msg,
      },
      stubs: {
        VMarkdownView: {
          name: "VMarkdownView",
          props: ["content"],
          template: '<div class="markdown-body">{{content}}</div>',
        },
      },
    },
    attachTo: document.body,
  });
};

describe("PluginDetails", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders only short description by itself if showExtended is false", async () => {
    const wrapper = await createWrapper({ showExtended: false });

    const blockDescription = wrapper.find("[data-testid='block-description']");
    const detailsTag = wrapper.find("details");

    expect(blockDescription.text()).toBe("First line");
    expect(detailsTag.exists()).toBeFalsy();
  });

  it("renders only first line if there is no remaining line", async () => {
    const wrapper = await createWrapper({ description: "First line" });

    const blockDescription = wrapper.find("[data-testid='block-description']");
    const detailsTag = wrapper.find("details");

    expect(blockDescription.text()).toBe("First line");
    expect(detailsTag.exists()).toBeFalsy();
  });

  it("renders the short description inside summary tag when inlineDescription is true", async () => {
    const wrapper = await createWrapper({ inlineDescription: true });

    const blockDescription = wrapper.find("[data-testid='block-description']");
    const inlineDescription = wrapper.find(
      "[data-testid='inline-description']",
    );
    expect(blockDescription.exists()).toBeFalsy();
    expect(inlineDescription.exists()).toBeTruthy();
    expect(inlineDescription.text()).toBe("First line");
  });

  it("correctly calculates the remainingLine content", async () => {
    const wrapper = await createWrapper();
    const markdownContainer = wrapper.find(
      "[data-testid='markdown-container']",
    );

    expect(markdownContainer.text()).toBe(
      "Remaining lines with some more content.",
    );
  });

  it("correctly calculates the remainingLine content when there is a cutOffMarker", async () => {
    const wrapper = await createWrapper({
      cutoffMarker: "---",
      description:
        "First line\ntest---\neverything following that will be rendered in a separate tab using [Markdeep](https://casual-effects.com/markdeep",
    });
    const markdownContainer = wrapper.find(
      "[data-testid='markdown-container']",
    );

    expect(markdownContainer.text()).toBe(
      "test---\n" +
        "everything following that will be rendered in a separate tab using [Markdeep](https://casual-effects.com/markdeep",
    );
  });

  it("renders extraDescription without markdown if allowHtml is false", async () => {
    const wrapper = await createWrapper({ allowHtml: false });
    const markdownContainer = wrapper.find(
      "[data-testid='markdown-container']",
    );
    const detailsTag = wrapper.find("details");
    expect(markdownContainer.exists()).toBeFalsy();
    // text for more and less are both rendered, as in the UI they get hidden by css.
    expect(detailsTag.text()).toBe(
      "more less Remaining lines with some more content.",
    );
  });
});
