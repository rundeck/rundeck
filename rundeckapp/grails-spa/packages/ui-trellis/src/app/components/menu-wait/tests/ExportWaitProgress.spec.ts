import { mount } from "@vue/test-utils";
import ExportWaitProgress from "../ExportWaitProgress.vue";
import { i18nGlobal } from "./i18nTestUtils";

const mountIt = (props: Record<string, any> = {}) =>
  mount(ExportWaitProgress, {
    props: {
      project: "demo",
      percentage: 40,
      refreshUrl: "/project/demo/exportWait/tok?instance=&iproject=",
      refresh: true,
      ...props,
    },
    global: i18nGlobal,
  });

describe("ExportWaitProgress", () => {
  it("shows the heading and waiting message", () => {
    const wrapper = mountIt();

    expect(wrapper.text()).toContain("Exporting an archive of project demo");
    expect(wrapper.text()).toContain(
      "Please wait, your request is being processed.",
    );
  });

  it("links to the refresh url", () => {
    const wrapper = mountIt();

    expect(wrapper.get('[data-testid="refresh-link"]').attributes("href")).toBe(
      "/project/demo/exportWait/tok?instance=&iproject=",
    );
  });

  it("shows the refresh checkbox checked when refresh is on", () => {
    const wrapper = mountIt();

    expect(
      (wrapper.get("#dorefresh").element as HTMLInputElement).checked,
    ).toBe(true);
  });

  it("emits update:refresh when the user unchecks the checkbox", async () => {
    const wrapper = mountIt();

    await wrapper.get("#dorefresh").setValue(false);

    expect(wrapper.emitted("update:refresh")![0]).toEqual([false]);
  });

  it("emphasizes the project name and renders markup in it as text", () => {
    const wrapper = mountIt({ project: "<img src=x onerror=alert(1)>" });

    expect(wrapper.get("strong").text()).toBe("<img src=x onerror=alert(1)>");
    expect(wrapper.find("img").exists()).toBe(false);
  });
});
