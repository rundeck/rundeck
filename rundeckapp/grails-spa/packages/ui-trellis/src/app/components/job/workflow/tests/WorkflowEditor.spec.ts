import { flushPromises, VueWrapper, shallowMount } from "@vue/test-utils";
import WorkflowEditor from "../WorkflowEditor.vue";
import WorkflowBasic from "@/app/components/job/workflow/WorkflowBasic.vue";
import WorkflowStrategy from "@/app/components/job/workflow/WorkflowStrategy.vue";
import WorkflowGlobalLogFilters from "@/app/components/job/workflow/WorkflowGlobalLogFilters.vue";
import UiSocket from "@/library/components/utils/UiSocket.vue";
import { createTestingPinia } from "@pinia/testing";

jest.mock("@/library/modules/rundeckClient", () => ({
  client: jest.fn(),
}));

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: { on: jest.fn(), emit: jest.fn() },
    rdBase: "http://localhost:4440",
    projectName: "testProject",
    apiVersion: "44",
  })),
}));
jest.mock("@/library/services/projects");

jest.mock("@/library/stores/NodesStorePinia", () => ({
  useNodesStore: jest.fn().mockImplementation(() => ({})),
}));

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
      plugins: [createTestingPinia({})],
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

    const workflowBasicComponent = wrapper.findComponent(WorkflowBasic);
    expect(workflowBasicComponent.exists()).toBe(true);
    expect(workflowBasicComponent.props("modelValue")).toEqual({
      keepgoing: true,
    });

    const workflowStrategyComponent = wrapper.findComponent(WorkflowStrategy);
    expect(workflowStrategyComponent.props("modelValue")).toEqual({
      type: "default",
      config: {},
    });

    const workflowGlobalLogFiltersComponent = wrapper.findComponent(
      WorkflowGlobalLogFilters,
    );
    expect(workflowGlobalLogFiltersComponent.props("modelValue")).toEqual({
      LogFilter: [{ type: "filter1" }],
    });

    const uiSocketComponent = wrapper.findComponent(UiSocket);
    expect(uiSocketComponent.props("modelValue")).toEqual({
      commands: [{ description: "step1" }],
    });
  });

  it("emits update:modelValue when data changes", async () => {
    const wrapper = await createWrapper();

    const workflowBasicComponent = wrapper.findComponent(WorkflowBasic);
    await workflowBasicComponent.vm.$emit("update:modelValue", {
      keepgoing: false,
    });

    await wrapper.vm.$nextTick();

    expect(
      wrapper.emitted("update:modelValue")![
        wrapper.emitted("update:modelValue")!.length - 1
      ][0],
    ).toMatchObject(
      expect.objectContaining({
        keepgoing: false,
        pluginConfig: {
          LogFilter: [{ type: "filter1" }],
          WorkflowStrategy: { default: {} },
        },
        commands: [{ description: "step1" }],
        strategy: "default",
      }),
    );
  });
});
