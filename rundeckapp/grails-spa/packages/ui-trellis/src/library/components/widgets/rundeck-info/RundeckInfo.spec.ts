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
      version: {
        logo: false,
        logocss: "some-css",
        number: "1.0.0",
        tag: "stable",
        title: "Rundeck",
      },
      server: {
        name: "localHost",
        icon: "paperclip",
        uuid: "uuid1",
        showId: true,
      },
      ...props,
    },
  });
};

describe("RundeckInfo", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders RundeckLogo when appInfo.title is 'Rundeck'", async () => {
    const wrapper = await mountRundeckInfo();
    expect(wrapper.findComponent({ name: "RundeckLogo" }).exists()).toBe(true);
    expect(wrapper.findComponent({ name: "PagerdutyLogo" }).exists()).toBe(
      false,
    );
  });
  it("renders PagerdutyLogo when appInfo.title is 'Pagerduty'", async () => {
    const wrapper = await mountRundeckInfo({ appInfo: { title: "Pagerduty" } });

    const versionProps = {
      logo: false,
      logocss: "some-css",
      number: "1.0.0",
      tag: "stable",
      title: "Rundeck",
    };
    expect(wrapper.findComponent({ name: "PagerdutyLogo" }).exists()).toBe(
      true,
    );
    expect(wrapper.findComponent({ name: "RundeckLogo" }).exists()).toBe(false);
  });
  it("renders RundeckVersions information with correct props based on version data", async () => {
    const versionProps = {
      logo: false,
      logocss: "some-css",
      number: "5.3.0",
      tag: "SNAPSHOT",
      title: "Rundeck",
    };

    const wrapper = await mountRundeckInfo({ version: versionProps });

    await wrapper.vm.$nextTick();
    const rundeckVersionComponent = wrapper.findComponent({
      name: "RundeckVersion",
    });
    expect(rundeckVersionComponent.exists()).toBe(true);
    expect(rundeckVersionComponent.props()).toEqual(versionProps);
    const versionText = rundeckVersionComponent.find("span").text();
    const expectedVersionText = `${versionProps.title} ${versionProps.number}`;
    expect(versionText).toBe(expectedVersionText);
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
    const latestProps = { full: "1.0.0", tag: "stable" };

    const wrapper = await mountRundeckInfo({
      latest: latestProps,
    });

    const rundeckVersionComponent = wrapper.findComponent({
      name: "RundeckVersion",
    });
    expect(rundeckVersionComponent.exists()).toBe(true);

    expect(rundeckVersionComponent.props("number")).toEqual(latestProps.full);
    expect(rundeckVersionComponent.props("tag")).toEqual(latestProps.tag);

    const versionTextSpan = rundeckVersionComponent.find("span");
    expect(versionTextSpan.exists()).toBe(true);
    const versionText = versionTextSpan.text();
    expect(versionText).toContain(latestProps.full);
  });

  it("should have an anchor with the correct href", async () => {
    const wrapper = await mountRundeckInfo();
    const expectedUrl = "http://localhost";
    const anchorElement = wrapper.find('[data-test-id="welcome-link"]');
    expect(anchorElement.exists()).toBe(true);
    expect(anchorElement.attributes("href")).toBe(expectedUrl);
  });
});
