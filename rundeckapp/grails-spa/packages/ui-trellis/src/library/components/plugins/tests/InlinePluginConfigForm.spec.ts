import { mount, flushPromises } from "@vue/test-utils";
import InlinePluginConfigForm from "../InlinePluginConfigForm.vue";

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: { on: jest.fn(), emit: jest.fn() },
    rdBase: "http://localhost:4440",
    projectName: "testProject",
    apiVersion: "44",
  })),
}));

jest.mock("@/library/modules/pluginService", () => ({
  getServiceProviderDescription: jest.fn().mockResolvedValue({
    name: "exec",
    title: "Command",
    description: "Execute a command",
    props: [
      {
        name: "exec",
        title: "Command",
        type: "String",
        required: true,
      },
    ],
  }),
}));

jest.mock("@/library/modules/rundeckClient", () => ({
  client: {
    sendRequest: jest.fn(),
  },
}));

const pluginConfigStub = {
  name: "pluginConfig",
  template: `<div data-testid="plugin-config-stub">
    <div data-testid="prop-field-exec">
      <input
        :value="modelValue?.config?.exec || ''"
        @input="$emit('update:modelValue', { type: modelValue.type, config: { ...modelValue.config, exec: $event.target.value } })"
      />
    </div>
  </div>`,
  props: [
    "modelValue",
    "mode",
    "pluginConfig",
    "serviceName",
    "validation",
    "showTitle",
    "showDescription",
    "contextAutocomplete",
    "scope",
    "defaultScope",
    "groupCss",
    "descriptionCss",
    "extraAutocompleteVars",
  ],
  emits: ["update:modelValue"],
};

const mockErrorHandler = {
  type: "exec",
  config: { exec: "echo hello" },
  nodeStep: true,
  keepgoingOnSuccess: false,
};

const createWrapper = async (props = {}, slots = {}) => {
  const wrapper = mount(InlinePluginConfigForm, {
    props: {
      modelValue: mockErrorHandler,
      serviceName: "WorkflowNodeStep",
      ...props,
    },
    slots,
    global: {
      stubs: { pluginConfig: pluginConfigStub },
    },
  });
  await flushPromises();
  return wrapper;
};

describe("InlinePluginConfigForm", () => {
  afterEach(() => jest.clearAllMocks());

  // ─── User sees plugin info and form ───────────────────────────────────────

  it("user sees plugin title and description after form loads", async () => {
    const wrapper = await createWrapper();

    expect(wrapper.text()).toContain("Command");
    expect(wrapper.text()).toContain("Execute a command");
  });

  it("user sees loading indicator while provider loads", async () => {
    const {
      getServiceProviderDescription,
    } = require("@/library/modules/pluginService");
    getServiceProviderDescription.mockImplementationOnce(
      () => new Promise(() => {}),
    );

    const wrapper = await createWrapper();

    expect(wrapper.text()).toContain("loading.text");
  });

  // ─── Save and Cancel buttons ──────────────────────────────────────────────

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

  it("user edits the command field and clicks Save, edited value is emitted with caller-owned fields intact", async () => {
    const wrapper = await createWrapper({
      modelValue: {
        type: "exec",
        config: { exec: "echo hello" },
        keepgoingOnSuccess: true,
        nodeStep: true,
        id: "handler-123",
      },
      showButtons: true,
    });

    await wrapper
      .find('[data-testid="prop-field-exec"] input')
      .setValue("echo updated");
    await wrapper.find('[data-testid="save-button"]').trigger("click");

    const emitted = wrapper.emitted("update:modelValue") as any[];
    expect(emitted.length).toBeGreaterThan(0);
    const lastEmit = emitted[emitted.length - 1][0];
    expect(lastEmit.config.exec).toBe("echo updated");
    expect(lastEmit.keepgoingOnSuccess).toBe(true);
    expect(lastEmit.nodeStep).toBe(true);
    expect(lastEmit.id).toBe("handler-123");
  });

  it("user fills empty form for new error handler and saves", async () => {
    const wrapper = await createWrapper({
      modelValue: { type: "exec", config: {}, nodeStep: true },
      showButtons: true,
    });

    await wrapper
      .find('[data-testid="prop-field-exec"] input')
      .setValue("echo new");
    await wrapper.find('[data-testid="save-button"]').trigger("click");

    const emitted = wrapper.emitted("update:modelValue") as any[];
    const lastEmit = emitted[emitted.length - 1][0];
    expect(lastEmit.config.exec).toBe("echo new");
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
