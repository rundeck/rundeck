// @ts-nocheck
import { mount } from "@vue/test-utils";
import ProjectPluginGroups from "../ProjectPluginGroups.vue";
import { getRundeckContext } from "@/library/rundeckService";

// eventBus is created once inside the factory closure (not referencing an
// outer variable, which jest.mock hoisting would otherwise break) so every
// call to getRundeckContext() — including the one ProjectPluginGroups.vue
// makes at its own module top-level — shares the same emit mock.
jest.mock("@/library/rundeckService", () => {
  const eventBus = { emit: jest.fn() };
  return {
    getRundeckContext: () => ({
      rundeckClient: {},
      rdBase: "",
      eventBus,
    }),
  };
});

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

// The real pluginConfig.vue drags in rundeckClient.ts, which reads a token
// out of the DOM at module-load time and isn't relevant to this behavior.
jest.mock("@/library/components/plugins/pluginConfig.vue", () => ({
  name: "PluginConfig",
  props: ["mode", "serviceName", "provider", "showDescription", "showTitle", "config", "validation", "validationWarningText"],
  template: "<div><slot name=\"extra\"></slot></div>",
}));

import pluginService from "../../../../library/modules/pluginService";

const mockEmit = getRundeckContext().eventBus.emit;

const mountWidget = async (props: Record<string, any> = {}) => {
  const wrapper = mount(ProjectPluginGroups, {
    props: {
      serviceName: "PluginGroup",
      configPrefix: "pluginValues.PluginGroup.",
      editMode: true,
      ...props,
    },
    global: {
      stubs: {
        PluginInfo: true,
        Expandable: true,
        modal: true,
        btn: true,
      },
    },
  });
  await wrapper.vm.$nextTick();
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("ProjectPluginGroups editing-state event", () => {
  beforeEach(() => {
    (window as any)._rundeck = {
      projectName: "testProject",
      data: { pluginGroups: { config: [] } },
    };
  });

  afterEach(() => {
    mockEmit.mockClear();
  });

  it("does not emit an editing signal on mount when nothing is being edited", async () => {
    await mountWidget();

    expect(mockEmit).not.toHaveBeenCalledWith(
      "project-plugin-group-editing",
      expect.anything(),
    );
  });

  it("emits editing=true as soon as a plugin is opened for editing", async () => {
    const wrapper = await mountWidget();

    // Opening a new plugin group entry (or clicking Edit on an existing
    // one) puts it in edit mode without yet committing any field values —
    // this is the state that used to let the page-level Save silently
    // discard the in-progress edit.
    wrapper.vm.addPlugin("test-plugin");
    await wrapper.vm.$nextTick();

    expect(wrapper.vm.editFocus).toBe(0);
    expect(mockEmit).toHaveBeenLastCalledWith(
      "project-plugin-group-editing",
      true,
    );
  });

  it("emits editing=false once the plugin's own Save has been clicked", async () => {
    const wrapper = await mountWidget();

    wrapper.vm.addPlugin("test-plugin");
    wrapper.vm.workingData[0].entry.config = { host: "example.com" };
    await wrapper.vm.savePlugin(wrapper.vm.workingData[0], 0);
    await wrapper.vm.$nextTick();

    expect(pluginService.validatePluginConfig).toHaveBeenCalled();
    expect(wrapper.vm.editFocus).toBe(-1);
    expect(mockEmit).toHaveBeenLastCalledWith(
      "project-plugin-group-editing",
      false,
    );
  });

  it("emits editing=false when the edit is cancelled instead of saved", async () => {
    const wrapper = await mountWidget();

    wrapper.vm.addPlugin("test-plugin");
    await wrapper.vm.$nextTick();
    wrapper.vm.didCancel(wrapper.vm.workingData[0], 0);
    await wrapper.vm.$nextTick();

    expect(wrapper.vm.editFocus).toBe(-1);
    expect(mockEmit).toHaveBeenLastCalledWith(
      "project-plugin-group-editing",
      false,
    );
  });

  it("emits editing=false on unmount as a safety net against a stuck-hidden Save button", async () => {
    const wrapper = await mountWidget();

    wrapper.vm.addPlugin("test-plugin");
    await wrapper.vm.$nextTick();
    mockEmit.mockClear();

    wrapper.unmount();

    expect(mockEmit).toHaveBeenCalledWith("project-plugin-group-editing", false);
  });
});
