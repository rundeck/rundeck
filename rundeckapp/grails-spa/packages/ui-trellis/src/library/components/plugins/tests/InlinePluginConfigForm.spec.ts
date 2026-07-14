import { mount, flushPromises } from "@vue/test-utils";
import InlinePluginConfigForm from "../InlinePluginConfigForm.vue";

jest.mock("@/library/modules/rundeckClient", () => ({
  client: { sendRequest: jest.fn() },
}));

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: { on: jest.fn(), emit: jest.fn() },
  })),
}));

jest.mock("@/library/modules/pluginService", () => ({
  getServiceProviderDescription: jest.fn().mockResolvedValue({
    name: "exec",
    title: "Command",
    description: "Execute a command",
    props: [],
  }),
}));

const {
  getServiceProviderDescription,
} = require("@/library/modules/pluginService");

const mockErrorHandler = {
  type: "exec",
  config: { exec: "echo hello" },
};

const createWrapper = async (props = {}) => {
  const wrapper = mount(InlinePluginConfigForm, {
    props: {
      modelValue: mockErrorHandler,
      serviceName: "WorkflowNodeStep",
      ...props,
    },
  });
  await flushPromises();
  return wrapper;
};

describe("InlinePluginConfigForm", () => {
  afterEach(() => jest.clearAllMocks());

  it("user sees plugin title and description after form loads", async () => {
    const wrapper = await createWrapper();

    expect(wrapper.text()).toContain("Command");
    expect(wrapper.text()).toContain("Execute a command");
  });

  it("user sees loading indicator while provider loads", async () => {
    getServiceProviderDescription.mockImplementationOnce(
      () => new Promise(() => {}),
    );

    const wrapper = await createWrapper();

    expect(wrapper.text()).toContain("loading.text");
  });

  it("user clicks Save button, form emits save and update:modelValue", async () => {
    const wrapper = await createWrapper({ showButtons: true });

    await wrapper.find('[data-testid="save-button"]').trigger("click");

    expect(wrapper.emitted("save")).toHaveLength(1);
    const emitted = wrapper.emitted("update:modelValue") as any[];
    expect(emitted).toHaveLength(1);
    const emittedData = emitted[0][0];
    expect(emittedData.type).toBe("exec");
    expect(emittedData.config.exec).toBe("echo hello");
  });

  it("user clicks Cancel button, form emits cancel", async () => {
    const wrapper = await createWrapper({ showButtons: true });

    await wrapper.find('[data-testid="cancel-button"]').trigger("click");

    expect(wrapper.emitted("cancel")).toHaveLength(1);
  });

  it("user does not see Save or Cancel buttons when showButtons is false", async () => {
    const wrapper = await createWrapper({ showButtons: false });

    expect(wrapper.text()).not.toContain("Save");
    expect(wrapper.text()).not.toContain("Cancel");
  });
});
