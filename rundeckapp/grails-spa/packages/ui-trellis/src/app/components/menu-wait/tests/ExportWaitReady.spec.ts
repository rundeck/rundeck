import { mount } from "@vue/test-utils";
import ExportWaitReady from "../ExportWaitReady.vue";
import { i18nGlobal } from "./i18nTestUtils";

const mountIt = (props: Record<string, any> = {}) =>
  mount(ExportWaitReady, {
    props: {
      project: "demo",
      downloadUrl: "/project/demo/exportWait/tok?download=true",
      returnUrl: "/project/demo/export",
      ...props,
    },
    global: i18nGlobal,
  });

describe("ExportWaitReady", () => {
  it("shows the download button with the archive name", () => {
    const wrapper = mountIt();
    const link = wrapper.get("#export-download-btn");

    expect(link.classes()).toContain("btn-cta");
    expect(link.text()).toBe("demo.rdproject.jar");
    expect(link.attributes("href")).toMatch(/\?download=true$/);
    expect(wrapper.text()).toContain("Download an archive of project demo");
    expect(wrapper.text()).toContain("This archive will expire");
  });

  it("links back to the export page", () => {
    const wrapper = mountIt();

    expect(wrapper.get('[data-testid="return-link"]').attributes("href")).toBe(
      "/project/demo/export",
    );
  });

  it("shows the success message and instance link in instance mode", () => {
    const wrapper = mountIt({
      instance: "http://other:4440",
      iproject: "target",
    });

    expect(wrapper.find("#export-download-btn").exists()).toBe(false);
    expect(wrapper.text()).toContain("Archive successfully imported");
    expect(
      wrapper.get('[data-testid="instance-link"]').attributes("href"),
    ).toBe("http://other:4440/?project=target");
    expect(wrapper.text()).toContain(
      "Exporting project demo to http://other:4440",
    );
  });

  it("emphasizes the project name in the title and renders markup as text", () => {
    const wrapper = mountIt({ project: "<img src=x onerror=alert(1)>" });

    expect(wrapper.get(".panel-heading strong").text()).toBe(
      "<img src=x onerror=alert(1)>",
    );
    expect(wrapper.find("img").exists()).toBe(false);
  });

  it("emphasizes project and instance in instance mode and renders markup as text", () => {
    const wrapper = mountIt({
      instance: "<img src=x onerror=alert(1)>",
      iproject: "t",
    });

    const bolds = wrapper.findAll(".panel-heading b").map((b) => b.text());
    expect(bolds).toEqual(["demo", "<img src=x onerror=alert(1)>"]);
    expect(wrapper.find("img").exists()).toBe(false);
  });

  it("does not render an instance link for a javascript: instance", () => {
    const wrapper = mountIt({ instance: "javascript:alert(1)", iproject: "t" });

    expect(wrapper.find('[data-testid="instance-link"]').exists()).toBe(false);
  });
});
