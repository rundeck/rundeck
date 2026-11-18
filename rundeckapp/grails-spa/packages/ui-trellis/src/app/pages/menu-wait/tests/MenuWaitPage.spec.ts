import { mount, flushPromises } from "@vue/test-utils";
import MenuWaitPage from "../MenuWaitPage.vue";
import {
  pollExportWait,
  EXPORT_WAIT_REQUEST_FAILED,
} from "../../../components/menu-wait/services/exportWaitService";
import { i18nGlobal } from "../../../components/menu-wait/tests/i18nTestUtils";

jest.mock("../../../components/menu-wait/services/exportWaitService", () => ({
  pollExportWait: jest.fn(),
  EXPORT_WAIT_REQUEST_FAILED: "EXPORT_WAIT_REQUEST_FAILED",
}));

const mountIt = (props: Record<string, any> = {}) =>
  mount(MenuWaitPage, {
    props: {
      token: "tok",
      project: "demo",
      initialReady: false,
      initialPercentage: 10,
      pollUrl: "/project/demo/exportWait/tok.json",
      downloadUrl: "/project/demo/exportWait/tok?download=true",
      refreshUrl: "/project/demo/exportWait/tok?instance=&iproject=",
      returnUrl: "/project/demo/export",
      ...props,
    },
    global: i18nGlobal,
  });

describe("MenuWaitPage", () => {
  beforeEach(() => {
    jest.useFakeTimers();
    (pollExportWait as jest.Mock).mockReset();
    document.title = "orig";
  });
  afterEach(() => jest.useRealTimers());

  it("shows the in-progress panel with refresh checked", () => {
    const wrapper = mountIt();

    expect(wrapper.text()).toContain("Exporting an archive of project demo");
    expect(
      (wrapper.get("#dorefresh").element as HTMLInputElement).checked,
    ).toBe(true);
    expect(wrapper.find("#export-download-btn").exists()).toBe(false);
    wrapper.unmount();
  });

  it("polls every 5 seconds and switches to ready, setting the title and stopping", async () => {
    (pollExportWait as jest.Mock).mockResolvedValue({ ready: true });
    const wrapper = mountIt();

    jest.advanceTimersByTime(5000);
    await flushPromises();

    expect(pollExportWait).toHaveBeenCalledWith(
      "/project/demo/exportWait/tok.json",
    );
    expect(wrapper.get("#export-download-btn").text()).toBe(
      "demo.rdproject.jar",
    );
    expect(document.title).toBe("[READY] Export archive");

    jest.advanceTimersByTime(15000);
    expect(pollExportWait).toHaveBeenCalledTimes(1);
    wrapper.unmount();
  });

  it("stops polling when the user unchecks refresh and resumes when rechecked", async () => {
    (pollExportWait as jest.Mock).mockResolvedValue({
      ready: false,
      percentage: 50,
    });
    const wrapper = mountIt();

    await wrapper.get("#dorefresh").setValue(false);
    jest.advanceTimersByTime(10000);
    expect(pollExportWait).not.toHaveBeenCalled();

    await wrapper.get("#dorefresh").setValue(true);
    jest.advanceTimersByTime(5000);
    expect(pollExportWait).toHaveBeenCalledTimes(1);
    wrapper.unmount();
  });

  it("shows the not found panel and stops polling", async () => {
    (pollExportWait as jest.Mock).mockResolvedValue({ notFound: true });
    const wrapper = mountIt();

    jest.advanceTimersByTime(5000);
    await flushPromises();

    expect(wrapper.text()).toContain("The token tok was not found");
    expect(wrapper.text()).not.toContain("Please wait");
    jest.advanceTimersByTime(10000);
    expect(pollExportWait).toHaveBeenCalledTimes(1);
    wrapper.unmount();
  });

  it("shows the error message and stops polling when the request fails", async () => {
    (pollExportWait as jest.Mock).mockRejectedValue(new Error("Server Down"));
    const wrapper = mountIt();

    jest.advanceTimersByTime(5000);
    await flushPromises();

    expect(wrapper.text()).toContain("Server Down");
    jest.advanceTimersByTime(10000);
    expect(pollExportWait).toHaveBeenCalledTimes(1);
    wrapper.unmount();
  });

  it("lists instance errors from a poll result", async () => {
    (pollExportWait as jest.Mock).mockResolvedValue({ errors: ["bad thing"] });
    const wrapper = mountIt({ instance: "http://o", iproject: "p" });

    jest.advanceTimersByTime(5000);
    await flushPromises();

    expect(wrapper.get("pre").text()).toBe("bad thing");
    wrapper.unmount();
  });

  it("starts ready without polling when the export is already complete", () => {
    const wrapper = mountIt({ initialReady: true });

    expect(wrapper.find("#export-download-btn").exists()).toBe(true);
    jest.advanceTimersByTime(10000);
    expect(pollExportWait).not.toHaveBeenCalled();
    wrapper.unmount();
  });

  it("shows the translated failure message and stops polling on a non-JSON body", async () => {
    (pollExportWait as jest.Mock).mockRejectedValue(
      new Error(EXPORT_WAIT_REQUEST_FAILED),
    );
    const wrapper = mountIt();

    jest.advanceTimersByTime(5000);
    await flushPromises();

    expect(wrapper.get('[data-testid="error-message-panel"]').text()).toContain(
      "Request failed",
    );
    expect(wrapper.text()).not.toContain(EXPORT_WAIT_REQUEST_FAILED);
    jest.advanceTimersByTime(10000);
    expect(pollExportWait).toHaveBeenCalledTimes(1);
    wrapper.unmount();
  });
});
