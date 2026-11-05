import { shallowMount, VueWrapper } from "@vue/test-utils";
import MarkdownView from "../MarkdownView.vue";

const createWrapper = async (content = ""): Promise<VueWrapper<any>> => {
  return shallowMount(MarkdownView, {
    props: { content },
  });
};

const renderedHtml = (wrapper: VueWrapper<any>): string => {
  return wrapper.find('[data-testid="markdown-view"]').html();
};

describe("MarkdownView", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders markdown as HTML", async () => {
    const wrapper = await createWrapper("See the [docs](https://example.com).");

    const html = renderedHtml(wrapper);
    expect(html).toContain('href="https://example.com"');
    expect(html).toContain("docs");
  });

  it("does not keep a script tag from the markdown source", async () => {
    const wrapper = await createWrapper("hello <script>alert(1)</script>");

    const html = renderedHtml(wrapper);
    expect(html).not.toContain("<script");
    expect(wrapper.find('[data-testid="markdown-view"]').text()).toContain(
      "hello",
    );
  });

  it("renders an empty string when there is no content", async () => {
    const wrapper = await createWrapper();

    expect(wrapper.find('[data-testid="markdown-view"]').text()).toBe("");
  });
});
