import { vi } from "vitest";
import { flushPromises, VueWrapper, shallowMount } from "@vue/test-utils";
import { createTestingPinia } from "@pinia/testing";

vi.mock("../WorkflowBasic.vue", () => ({
  default: { name: "WorkflowBasic", template: "<div />", props: ["modelValue"] },
}));
vi.mock("@/app/components/job/workflow/WorkflowStrategy.vue", () => ({
  default: {
    name: "WorkflowStrategy",
    template: "<div />",
    props: ["modelValue"],
  },
}));
vi.mock("@/app/components/job/workflow/WorkflowGlobalLogFilters.vue", () => ({
  default: {
    name: "WorkflowGlobalLogFilters",
    template: "<div />",
    props: ["modelValue"],
  },
}));
vi.mock("@/library/components/utils/UiSocket.vue", () => ({
  default: { name: "UiSocket", template: "<div />", props: ["modelValue"] },
}));
vi.mock("@/app/pages/job/editor/OptionsEditorSection.vue", () => ({
  default: { name: "OptionsEditorSection", template: "<div />" },
}));
vi.mock("@/library/components/plugins/pluginPropEdit.vue", () => ({
  default: { name: "PluginPropEdit", template: "<div />" },
}));
vi.mock("lodash", () => ({
  cloneDeep: <T>(value: T): T => JSON.parse(JSON.stringify(value)),
}));
vi.mock("../types/workflowTypes", () => ({
  createBasicData: ({ keepgoing }: { keepgoing?: boolean }) => ({
    keepgoing: !!keepgoing,
  }),
  createStrategyData: (workflow: {
    strategy?: string;
    pluginConfig?: Record<string, Record<string, unknown>>;
  }) => ({
    type: workflow.strategy || "",
    config: workflow.pluginConfig?.WorkflowStrategy?.[workflow.strategy || ""] || {},
  }),
  createLogFiltersData: (workflow: {
    pluginConfig?: { LogFilter?: unknown[] };
  }) => ({
    LogFilter: workflow.pluginConfig?.LogFilter || [],
  }),
  createStepsData: ({ commands }: { commands?: unknown[] }) => ({
    commands: commands || [],
  }),
  exportPluginData: (
    strategy: { type: string; config: unknown },
    logFilters: { LogFilter?: unknown[] },
  ) => ({
    strategy: strategy.type,
    pluginConfig: {
      WorkflowStrategy: { [strategy.type]: strategy.config },
      ...(logFilters.LogFilter?.length ? { LogFilter: logFilters.LogFilter } : {}),
    },
  }),
}));

vi.mock("@/library/modules/rundeckClient", () => ({
  client: vi.fn(),
}));

vi.mock("@/library/rundeckService", () => ({
  getRundeckContext: vi.fn().mockImplementation(() => ({
    eventBus: { on: vi.fn(), emit: vi.fn() },
    rdBase: "http://localhost:4440",
    projectName: "testProject",
    apiVersion: "44",
  })),
}));
vi.mock("@/library/services/projects", () => ({}));

vi.mock("@/library/stores/NodesStorePinia", () => ({
  useNodesStore: vi.fn().mockImplementation(() => ({})),
}));

const { default: WorkflowEditor } = await import("../WorkflowEditor.vue");
const { default: WorkflowBasic } = await import(
  "@/app/components/job/workflow/WorkflowBasic.vue"
);
const { default: WorkflowStrategy } = await import(
  "@/app/components/job/workflow/WorkflowStrategy.vue"
);
const { default: WorkflowGlobalLogFilters } = await import(
  "@/app/components/job/workflow/WorkflowGlobalLogFilters.vue"
);
const { default: UiSocket } = await import(
  "@/library/components/utils/UiSocket.vue"
);

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
    vi.clearAllMocks();
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
