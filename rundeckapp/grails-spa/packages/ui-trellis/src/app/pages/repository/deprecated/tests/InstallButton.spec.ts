import { flushPromises, mount } from "@vue/test-utils";
import axios from "axios";
import InstallButton from "../InstallButton.vue";

jest.mock("axios");
const mockedAxios = axios as jest.MockedFunction<typeof axios>;

const remotePlugin = {
  object_id: "plugin-1",
  current_version: "1.2.0",
  post_slug: "plugin-1",
};

const createWrapper = async (props = {}) => {
  const wrapper = mount(InstallButton, {
    props: {
      plugin: remotePlugin,
      installedPlugins: [],
      installedPluginIds: [],
      repo: "official",
      ...props,
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("InstallButton", () => {
  beforeEach(() => {
    (window as any)._rundeck = {
      rdBase: "http://localhost:4440",
      apiVersion: "44",
    };
    mockedAxios.mockResolvedValue({ data: {} });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it("shows Update Available when the remote version is newer than the installed one", async () => {
    const wrapper = await createWrapper({
      installedPluginIds: ["plugin-1"],
      installedPlugins: [{ artifactId: "plugin-1", version: "1.1.0" }],
    });

    expect(wrapper.find('[data-testid="install-button-update"]').exists()).toBe(
      true,
    );
  });

  it("hides Update Available when the installed version is current", async () => {
    const wrapper = await createWrapper({
      installedPluginIds: ["plugin-1"],
      installedPlugins: [{ artifactId: "plugin-1", version: "1.2.0" }],
    });

    expect(
      wrapper.find('[data-testid="install-button-uninstall"]').exists(),
    ).toBe(true);
    expect(wrapper.find('[data-testid="install-button-update"]').exists()).toBe(
      false,
    );
  });

  it("hides Update Available after installing a plugin that was not installed", async () => {
    const wrapper = await createWrapper();

    await wrapper
      .find('[data-testid="install-button-install"]')
      .trigger("click");
    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(mockedAxios).toHaveBeenCalledWith(
      expect.objectContaining({
        url: "http://localhost:4440/repository/official/install/plugin-1",
      }),
    );
    expect(
      wrapper.find('[data-testid="install-button-uninstall"]').exists(),
    ).toBe(true);
    expect(wrapper.find('[data-testid="install-button-update"]').exists()).toBe(
      false,
    );
  });
});
