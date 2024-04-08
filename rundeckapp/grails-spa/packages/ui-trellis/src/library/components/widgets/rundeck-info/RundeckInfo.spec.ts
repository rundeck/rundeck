import { shallowMount } from "@vue/test-utils";
import RundeckInfo from "./RundeckInfo.vue";

jest.mock("../../../rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({ rdBase: "mocked-rdBase" }),
  url: jest.fn().mockReturnValue("mocked-url"),
}));

const mountRundeckInfo = async (props?: Record<string, any>) => {
  return shallowMount(RundeckInfo, {
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
  it("renders RundeckVersions information correctly ", async () => {
    const versionProps = {
      logo: false,
      number: "1.0.0",
      tag: "stable",
    };
    const wrapper = await mountRundeckInfo({ version: versionProps });
    const versionComponent = wrapper.findComponent({ name: "RundeckVersion" });
    expect(versionComponent.exists()).toBe(true);
  });
  it("renders ServerDisplay with exact server props", async () => {
    const serverProps = {
      name: "localHost",
      icon: "paperclip",
      uuid: "uuid1",
      showId: true,
    };
    const wrapper = await mountRundeckInfo({ server: serverProps });
    const serverComponent = wrapper.findComponent({ name: "ServerDisplay" });
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

  it("renders latest release information ", async () => {
    const latestProps = {
      logo: false,
      full: "1.0.0",
      tag: "GA",
      logocss: "rdicon",
    };
    const wrapper = await mountRundeckInfo({
      latest: latestProps,
    });
    const latestVersionComponent = wrapper.findComponent({
      name: "RundeckVersion",
    });

    expect(latestVersionComponent.props()).toEqual({
      logo: latestProps.logo,
      number: latestProps.full,
      tag: latestProps.tag,
      logocss: latestProps.logocss,
    });
  });

  it("renders the correct url", async () => {
    const wrapper = await mountRundeckInfo();
    expect(wrapper.vm.welcomeUrl()).toBe("mocked-url");
  });
});
