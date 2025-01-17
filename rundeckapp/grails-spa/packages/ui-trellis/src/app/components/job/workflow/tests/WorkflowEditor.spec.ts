import { flushPromises, VueWrapper, shallowMount } from "@vue/test-utils";
import WorkflowEditor from "../WorkflowEditor.vue";
import WorkflowBasic from "@/app/components/job/workflow/WorkflowBasic.vue";
import { createTestingPinia } from "@pinia/testing";

jest.mock("@/library/modules/rundeckClient", () => ({
  client: jest.fn(),
}));

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: { on: jest.fn(), emit: jest.fn() },
    rdBase: "http://localhost:4440/",
    projectName: "testProject",
    apiVersion: "44",
  })),
}));
jest.mock("../../../../../library/services/projects");

const createWrapper = async (propsData = {}): Promise<VueWrapper<any>> => {
  const wrapper = shallowMount(WorkflowEditor, {
    props: {
      modelValue: {
        keepgoing: true,
        strategy: "default",
        pluginConfig: { LogFilter: [{ type: "filter1" }] },
        commands: [{ description: "step1" }],
      },
      ...propsData,
    },
    global: {
      stubs: {
        WorkflowBasic: false,
      },
      plugins: [createTestingPinia()],
    },
  });
  await wrapper.vm.$nextTick();
  await flushPromises();
  return wrapper;
};

describe("WorkflowEditor", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("initializes data correctly on mount", async () => {
    const wrapper = await createWrapper();

    expect(wrapper.vm.basicData).toEqual({ keepgoing: true });
    expect(wrapper.vm.strategyData).toEqual({ strategy: "default" });
    expect(wrapper.vm.logFiltersData).toEqual({
      LogFilter: [{ type: "filter1" }],
    });
    expect(wrapper.vm.stepsData).toEqual({
      commands: [{ description: "step1" }],
    });
    expect(wrapper.vm.loaded).toBe(true);
  });

  it("emits update:modelValue when data changes", async () => {
    const wrapper = await createWrapper();

    const workflowBasicComponent = wrapper.findComponent(WorkflowBasic);
    await workflowBasicComponent.vm.$emit("update:modelValue", {
      keepgoing: false,
    });

    await wrapper.vm.$nextTick();

    expect(
      wrapper.emitted("update:modelValue")[
        wrapper.emitted("update:modelValue").length - 1
      ][0],
    ).toMatchObject(
      expect.objectContaining({
        keepgoing: false,
        pluginConfig: {
          LogFilter: [{ type: "filter1" }],
        },
        commands: [{ description: "step1" }],
        strategy: "default",
      }),
    );
  });
});
