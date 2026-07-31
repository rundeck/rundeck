import { mount } from "@vue/test-utils";
import ProjectPluginGroups from "../ProjectPluginGroups.vue";

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: () => ({
    rundeckClient: {},
    rdBase: "",
  }),
}));

jest.mock("@/library/modules/pluginService", () => ({
  __esModule: true,
  default: {
    getPluginProvidersForService: jest.fn().mockResolvedValue({
      service: true,
      descriptions: [{ name: "test-plugin", title: "Test Plugin" }],
      labels: [],
    }),
    validatePluginConfig: jest.fn().mockResolvedValue({ valid: true }),
  },
}));

import pluginService from "@/library/modules/pluginService";

const mountInForm = async (props: Record<string, any> = {}) => {
  const formEl = document.createElement("form");
  document.body.appendChild(formEl);
  const wrapper = mount(ProjectPluginGroups, {
    attachTo: formEl,
    props: {
      serviceName: "PluginGroup",
      configPrefix: "pluginValues.PluginGroup.",
      editMode: true,
      ...props,
    },
    global: {
      stubs: {
        PluginInfo: true,
        PluginConfig: true,
        Expandable: true,
        modal: true,
        btn: true,
      },
    },
  });
  await wrapper.vm.$nextTick();
  await wrapper.vm.$nextTick();
  return { wrapper, formEl };
};

const submitForm = (formEl: HTMLFormElement) => {
  const event = new Event("submit", { cancelable: true, bubbles: true });
  formEl.dispatchEvent(event);
  return event;
};

describe("ProjectPluginGroups unsaved-edit submit guard", () => {
  beforeEach(() => {
    (window as any)._rundeck = {
      projectName: "testProject",
      data: { pluginGroups: { config: [] } },
    };
  });

  afterEach(() => {
    document.body.innerHTML = "";
    jest.clearAllMocks();
  });

  it("does not block the page-level submit when no plugin is being edited", async () => {
    const { wrapper, formEl } = await mountInForm();

    const event = submitForm(formEl);

    expect(event.defaultPrevented).toBe(false);
    expect(wrapper.vm.errors).toHaveLength(0);
  });

  it("blocks the page-level submit and shows an error while a plugin edit is unsaved", async () => {
    const { wrapper, formEl } = await mountInForm();

    // Simulate a user opening a new plugin group for editing without
    // clicking that plugin's own inline Save button.
    wrapper.vm.addPlugin("test-plugin");
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.editFocus).toBe(0);

    const event = submitForm(formEl);

    expect(event.defaultPrevented).toBe(true);
    expect(wrapper.vm.errors).toHaveLength(1);
    expect(wrapper.vm.errors[0]).toContain("PluginGroup");
  });

  it("allows the page-level submit again once the plugin's own Save has been clicked", async () => {
    const { wrapper, formEl } = await mountInForm();

    wrapper.vm.addPlugin("test-plugin");
    wrapper.vm.workingData[0].entry.config = { host: "example.com" };
    await wrapper.vm.savePlugin(wrapper.vm.workingData[0], 0);
    await wrapper.vm.$nextTick();

    expect(pluginService.validatePluginConfig).toHaveBeenCalled();
    expect(wrapper.vm.editFocus).toBe(-1);

    const event = submitForm(formEl);

    expect(event.defaultPrevented).toBe(false);
  });
});
