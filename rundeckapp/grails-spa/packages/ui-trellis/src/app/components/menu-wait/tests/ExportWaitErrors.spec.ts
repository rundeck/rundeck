import { mount } from "@vue/test-utils";
import ExportWaitErrors from "../ExportWaitErrors.vue";
import { i18nGlobal } from "./i18nTestUtils";

const mountIt = (props: Record<string, any> = {}) =>
  mount(ExportWaitErrors, {
    props: {
      project: "demo",
      token: "tok1",
      returnUrl: "/project/demo/export",
      ...props,
    },
    global: i18nGlobal,
  });

describe("ExportWaitErrors", () => {
  it("renders nothing visible when there are no errors", () => {
    const wrapper = mountIt();

    expect(wrapper.find(".panel").exists()).toBe(false);
  });

  it("shows the not found panel with the token", () => {
    const wrapper = mountIt({ notFound: true });

    expect(wrapper.text()).toContain("Not Found");
    expect(wrapper.text()).toContain("The token tok1 was not found");
  });

  it("shows the error message", () => {
    const wrapper = mountIt({ errorMessage: "boom" });

    expect(wrapper.get('[data-testid="error-message-panel"]').text()).toContain(
      "boom",
    );
    expect(wrapper.text()).toContain("There was an error exporting");
  });

  it("lists instance errors with goto and return links", () => {
    const wrapper = mountIt({
      errors: ["e1", "e2"],
      instance: "http://other",
      iproject: "tgt",
    });

    expect(wrapper.findAll("pre").map((p) => p.text())).toEqual(["e1", "e2"]);
    expect(
      wrapper.get('[data-testid="instance-link"]').attributes("href"),
    ).toBe("http://other/?project=tgt");
    expect(wrapper.text()).toContain("Return");
  });

  it("shows the token in code emphasis and renders markup as text", () => {
    const wrapper = mountIt({
      notFound: true,
      token: "<img src=x onerror=alert(1)>",
    });

    expect(wrapper.get("code").text()).toBe("<img src=x onerror=alert(1)>");
    expect(wrapper.find("img").exists()).toBe(false);
  });

  it("emphasizes project and instance in the errors heading", () => {
    const wrapper = mountIt({
      errors: ["e"],
      instance: "https://other",
      iproject: "t",
    });

    expect(wrapper.findAll(".panel-heading b").map((b) => b.text())).toEqual([
      "demo",
      "https://other",
    ]);
  });

  it("hides the goto link for a javascript: instance", () => {
    const wrapper = mountIt({
      errors: ["e"],
      instance: "javascript:alert(1)",
      iproject: "t",
    });

    expect(wrapper.find('[data-testid="instance-link"]').exists()).toBe(false);
  });
});
