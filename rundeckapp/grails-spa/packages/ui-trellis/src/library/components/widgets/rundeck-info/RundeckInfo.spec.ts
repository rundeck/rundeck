import { mount } from "@vue/test-utils";
import RundeckInfo from "./RundeckInfo.vue";

jest.mock("../../../rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({ rdBase: "mocked-rdBase" }),
  url: jest.fn().mockReturnValue("http://localhost"),
}));

const mountRundeckInfo = async (props?: Record<string, any>) => {
  return mount(RundeckInfo, {
    props: {
      appInfo: { title: "Rundeck", logocss: "some-css" },
      ...props,
    },
  });
};
describe("RundeckInfo", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders RundeckLogo when appInfo.title is 'Rundeck'", async () => {
    const wrapper = await mountRundeckInfo({});
    expect(wrapper.findComponent({ name: "RundeckLogo" }).exists()).toBe(true);
    expect(wrapper.findComponent({ name: "PagerdutyLogo" }).exists()).toBe(
      false,
    );
  });
  it("renders PagerdutyLogo when appInfo.title is 'Pagerduty'", async () => {
    const wrapper = await mountRundeckInfo({ appInfo: { title: "Pagerduty" } });

    expect(wrapper.findComponent({ name: "RundeckLogo" }).exists()).toBe(false);
    expect(wrapper.findComponent({ name: "PagerdutyLogo" }).exists()).toBe(
      true,
    );
  });
  it("renders RundeckVersions information correct props based on version data", async () => {
    const versionProps = {
      logo: false,
      logocss: "some-css",
      number: "1.0.0",
      tag: "stable",
      title: "Rundeck",
    };
    const wrapper = await mountRundeckInfo({ version: versionProps });

    await wrapper.vm.$nextTick();
    const versionComponent = wrapper.findComponent({ name: "RundeckVersion" });
    expect(versionComponent.exists()).toBe(true);
    expect(versionComponent.props()).toEqual(versionProps);
  });
  it("renders ServerDisplay correctly based on a server props", async () => {
    const serverProps = {
      name: "localHost",
      icon: "paperclip",
      uuid: "uuid1",
      showId: true,
    };
    const wrapper = await mountRundeckInfo({ server: serverProps });
    await wrapper.vm.$nextTick();
    const serverComponent = wrapper.findComponent({ name: "ServerDisplay" });
    expect(serverComponent.exists()).toBe(true);
    expect(serverComponent.props()).toEqual({
      name: serverProps.name,
      glyphicon: serverProps.icon,
      uuid: serverProps.uuid,
      showId: serverProps.showId,
    });
  });

  it("renders version display correctly", async () => {
    const versionDisplay = {
      name: "Rundeck",
      icon: "icon-url",
      color: "lamp",
    };
    const wrapper = await mountRundeckInfo({ version: versionDisplay });
    const versionDisplayComponent = wrapper.findComponent({
      name: "VersionDisplay",
    });

    expect(versionDisplayComponent.props("text")).toBe("Rundeck lamp icon-url");
  });

  it("renders latest release information correctly ", async () => {
    const latestProps = {
      full: "2.0.0",
      tag: "GA",
    };
    const wrapper = await mountRundeckInfo({
      latest: latestProps,
    });

    const latestSection = wrapper
      .find(".rundeck-info-widget__latest")
      .findComponent({ name: "RundeckVersion" });
    expect(latestSection.exists()).toBe(true);

    expect(latestSection.props("number")).toEqual(latestProps.full);
    expect(latestSection.props("tag")).toEqual(latestProps.tag);
  });

  it("should have an anchor with the correct href", async () => {
    const wrapper = await mountRundeckInfo();
    const expectedHref = "http://localhost";
    const anchorElement = wrapper.find(`a[href="${expectedHref}"]`);
    expect(anchorElement.exists()).toBe(true);
    expect(anchorElement.attributes("href")).toBe(expectedHref);
  });
});
