import { vi } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import EditPluginModal from "../EditPluginModal.vue";
import { Modal, Btn } from "uiv";
import * as pluginService from "../../../modules/pluginService";

interface MockPluginConfig {
  props?: any;
}
vi.mock(
  "@/library/modules/pluginService",
  async (importOriginal) => ({
    ...(await importOriginal<typeof import("@/library/modules/pluginService")>()),
    getServiceProviderDescription: vi.fn().mockResolvedValue({
      name: "MockProvider",
      description: "Provider description",
    }),
    getPluginData: vi.fn().mockResolvedValue({}),
  }),
);

vi.mock("@/library/modules/rundeckClient", () => ({
  client: {
    sendRequest: vi.fn(),
    TOKEN: "mockToken",
    URI: "mockURI",
    rdBase: "mockBase",
  },
}));
vi.mock("../pluginConfig.vue", () => ({
  default: {
    name: "PluginConfig",
    methods: {
      loadPluginData: vi.fn().mockImplementation(function (
        this: MockPluginConfig,
        data: { props: any },
      ) {
        this.props = data.props;
      }),
    },
    template: `<div data-testid="plugin-config">PluginConfig Mock</div>`,
  },
}));
const createWrapper = async (props = {}) => {
  const wrapper = mount(EditPluginModal, {
    props: {
      title: "Edit Plugin",
      modelValue: {
        type: "mockType",
        name: "mockName",
        config: {},
      },
      serviceName: "testService",
      modalActive: true,
      validation: {},
      ...props,
    },
    global: {
      components: { Modal, Btn },
    },
  });
  await flushPromises();
  return wrapper;
};
describe("EditPluginModal", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });
  it("displays loading text when loading", async () => {
    // Mocking getServiceProviderDescription to simulate a loading state
    (
      pluginService.getServiceProviderDescription as vi.Mock
    ).mockImplementationOnce(() => new Promise(() => {}));
    const wrapper = await createWrapper();
    expect(wrapper.find(".fa-spinner").exists()).toBe(true);
    expect(wrapper.text()).toContain("loading.text");
  });
  it("displays plugin information when provider data is available", async () => {
    (
      pluginService.getServiceProviderDescription as vi.Mock
    ).mockResolvedValueOnce({
      name: "Test Plugin",
      description: "Plugin description",
    });
    const wrapper = await createWrapper();
    // Verify that the plugin-info component is present and displays the description.
    const pluginInfo = wrapper.findComponent({ name: "plugin-info" });
    expect(pluginInfo.exists()).toBe(true);
    expect(pluginInfo.text()).toContain("Plugin description");
    // Verify that plugin-config is also rendered for editing configuration.
    const pluginConfig = wrapper.findComponent({ name: "plugin-config" });
    expect(pluginConfig.exists()).toBe(true);
  });

  it('emits "cancel" event when cancel button is clicked', async () => {
    const wrapper = await createWrapper();
    const cancelButton = wrapper.find('[data-testid="cancel-button"]');
    await cancelButton.trigger("click");
    expect(wrapper.emitted("cancel")).toBeTruthy();
  });
  it("displays provider information once loaded", async () => {
    const wrapper = await createWrapper();
    await flushPromises();
    expect(wrapper.findComponent({ name: "plugin-info" }).exists()).toBe(true);
    expect(wrapper.find("p").text()).toContain("Provider description");
  });
  it('emits "save" and "update:modelValue" events with correct data when save button is clicked', async () => {
    const wrapper = await createWrapper();
    const saveButton = wrapper.find('[data-testid="save-button"]');
    await saveButton.trigger("click");
    expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    expect(wrapper.emitted("save")).toBeTruthy();
    expect(wrapper.emitted("update:modelValue")![0][0]).toEqual({
      type: "mockType",
      name: "mockName",
      config: {},
    });
  });
});
