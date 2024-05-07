import { mount } from "@vue/test-utils";
import RundeckInfo from "./RundeckInfo.vue";
jest.mock("../../../rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({ rdBase: "mocked-rdBase" }),
  url: jest.fn().mockReturnValue("http://localhost"),
}));
const mountRundeckInfo = async (props = {}) => {
  return mount(RundeckInfo, {
    props: {
      appInfo: { title: "Rundeck", logocss: "some-css" },
      version: {
        number: "1.0.0",
        tag: "stable",
        name: "Erebus",
        color: "red",
        icon: "glass",
        edition: "Community",
      },
      server: {
        name: "localHost",
        icon: "paperclip",
        uuid: "uuid1",
      },
      latest: {
        title: "Rundeck",
        full: "v5.2.0-20240410",
        number: "5.2.0",
        tag: "GA",
        color: "aquamarine",
        date: new Date(),
        icon: "knight",
        edition: "Community",
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
      false
    );
  });
  it("renders PagerdutyLogo when appInfo.title is 'Pagerduty'", async () => {
    const wrapper = await mountRundeckInfo({ appInfo: { title: "Pagerduty" } });
    expect(wrapper.findComponent({ name: "PagerdutyLogo" }).exists()).toBe(
      true
    );
    expect(wrapper.findComponent({ name: "RundeckLogo" }).exists()).toBe(false);
  });

  it("renders the server's shortened uuid along with a title attribute", async () => {
    const wrapper = await mountRundeckInfo();
    await wrapper.vm.$nextTick();
    const serverComponent = wrapper.findComponent({ name: "ServerDisplay" });
    expect(serverComponent.exists()).toBe(true);
    const uuidShortElement = serverComponent.find(
      '[data-test-id="server-uuid-short"]'
    );
    expect(uuidShortElement.exists()).toBe(true);
    expect(uuidShortElement.text()).toBe("uu");
    const serverTitleElement = serverComponent.find(
      '[data-test-id="server-title"]'
    );
    expect(serverTitleElement.attributes("title")).toBe("paperclip-uu / uuid1");
  });

  it("renders Rundeck's version display name", async () => {
    const wrapper = await mountRundeckInfo();
    const versionDisplayComponent = wrapper.findComponent({
      name: "VersionDisplay",
    });
    expect(versionDisplayComponent.props("text")).toBe("Erebus red glass");
  });

  it("renders the link with the correct href", async () => {
    const wrapper = await mountRundeckInfo();
    const anchorElement = wrapper.find('[data-test-id="welcome-link"]');

    expect(anchorElement.attributes("href")).toBe("http://localhost");
  });

  it("renders the correct Rundeck version based on number and title props", async () => {
    const wrapper = await mountRundeckInfo();

    const rundeckVersionComponent = wrapper.findComponent({
      name: "RundeckVersion",
    });
    expect(rundeckVersionComponent.exists()).toBe(true);

    expect(rundeckVersionComponent.find("span").text()).toBe("Rundeck 1.0.0");
    expect(rundeckVersionComponent.props().title).toBe("Rundeck");
  });
  it("renders the latest release information correctly", async () => {
    const wrapper = await mountRundeckInfo();
    const rundeckVersionComponents = wrapper.findAllComponents({
      name: "RundeckVersion",
    });
    const latestReleaseComponent = rundeckVersionComponents[1];
    expect(latestReleaseComponent.exists()).toBe(true);
    expect(latestReleaseComponent.find("span").text()).toBe("v5.2.0-20240410");
    expect(latestReleaseComponent.props().number).toBe("v5.2.0-20240410");
    expect(latestReleaseComponent.props().tag).toBe("GA");
  });
  // it("renders the server's shortened uuid along with a title attribute", async () => {
  //   const wrapper = await mountRundeckInfo({
  //     server: { name: "uulocalHost", uuid: "uuid123456789", icon: "paperclip" },
  //   });
  //   await wrapper.vm.$nextTick();
  //   const serverComponent = wrapper.findComponent({ name: "ServerDisplay" });
  //   expect(serverComponent.exists()).toBe(true);
  //   const title = serverComponent
  //     .find('[data-test-id="server-title"]')
  //     .attributes("title");
  //   expect(title).toBe("paperclip-uu / uuid123456789");
  //   const uuidShortElement = serverComponent.find(
  //     '[data-test-id="server-uuid-short"]'
  //   );
  //   expect(uuidShortElement.exists()).toBe(true);
  //   expect(uuidShortElement.text()).toBe("uu");
  //   const serverNameElement = serverComponent.find(
  //     '[data-test-id="server-name"]'
  //   );
  //   expect(serverNameElement.text()).toBe("uulocalHost");
  // });
});
