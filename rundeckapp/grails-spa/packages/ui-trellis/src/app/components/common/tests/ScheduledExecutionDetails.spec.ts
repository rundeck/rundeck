import { shallowMount, VueWrapper } from "@vue/test-utils";
import ScheduledExecutionDetails from "../ScheduledExecutionDetails.vue";

const createWrapper = async (propsData = {}): Promise<VueWrapper<any>> => {
  return shallowMount(ScheduledExecutionDetails, {
    props: {
      allowHtml: true,
      firstLineOnly: false,
      description: "First line\nRemaining lines with some more content.",
      mode: "collapsed",
      markdownCss: "markdown-css-class",
      textCss: "text-css-class",
      cutoffMarker: "",
      service: "testService",
      name: "testName",
      moreText: "Read more",
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

describe("ScheduledExecutionDetails", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders only first line if html isn't allowed", async () => {
    const wrapper = await createWrapper({ allowHtml: false });

    const firstLineSpan = wrapper.find("[data-testid='first-line-only']");
    expect(firstLineSpan.text()).toBe("First line");
    const detailsContainer = wrapper.find("[data-testid='details-container']");
    expect(detailsContainer.exists()).toBeFalsy();
  });

  it("renders only first line if there is no remaining line", async () => {
    const wrapper = await createWrapper({ description: "First line" });

    const firstLineSpan = wrapper.find(
      "[data-testid='first-line-html-allowed']",
    );

    expect(firstLineSpan.text()).toBe("First line");
    const detailsContainer = wrapper.find("[data-testid='details-container']");
    expect(detailsContainer.exists()).toBeFalsy();
  });

  it("renders the right attribute when drawer is closed", async () => {
    const wrapper = await createWrapper({ moreText: "" });

    const summary = wrapper.find("summary");
    expect(summary.text()).toContain("First line");

    const details = wrapper.find("details.more-info");
    expect(details.attributes("open")).toEqual("false");
  });

  it("renders the right attribute when drawer is open", async () => {
    const wrapper = await createWrapper({ mode: "expanded" });

    const summary = wrapper.find("summary");
    expect(summary.text()).toContain("First line");

    const details = wrapper.find("details.more-info");
    expect(details.attributes("open")).toEqual("true");
  });

  it("correctly calculates the remainingLine content", async () => {
    const wrapper = await createWrapper({ mode: "expanded" });

    const details = wrapper.find("details.more-info");
    expect(details.attributes("open")).toEqual("true");

    expect(wrapper.find(".markdown-body").text()).toBe(
      "Remaining lines with some more content.",
    );
  });

  it("correctly calculates the remainingLine content when there is a cutOffMarker", async () => {
    const wrapper = await createWrapper({
      mode: "expanded",
      cutoffMarker: "---",
      description:
        "First line\ntest---\neverything following that will be rendered in a separate tab using [Markdeep](https://casual-effects.com/markdeep",
    });

    const details = wrapper.find("details.more-info");
    expect(details.attributes("open")).toEqual("true");

    expect(wrapper.find(".markdown-body").text()).toBe(
      "test---\n" +
        "everything following that will be rendered in a separate tab using [Markdeep](https://casual-effects.com/markdeep",
    );
  });
});
