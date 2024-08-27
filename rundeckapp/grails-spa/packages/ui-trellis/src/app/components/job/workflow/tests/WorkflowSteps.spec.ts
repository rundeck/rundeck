import { flushPromises, mount, VueWrapper } from "@vue/test-utils";
import WorkflowSteps from "../WorkflowSteps.vue";
import ChoosePluginModal from "@/library/components/plugins/ChoosePluginModal.vue";
import EditPluginModal from "@/library/components/plugins/EditPluginModal.vue";

jest.mock("@/library/modules/pluginService", () => ({
  getServiceProviderDescription: jest.fn(),
  getPluginProvidersForService: jest.fn().mockReturnValue({
    service: true,
    descriptions: ["descrip1", "descrip2"],
    labels: ["label1", "label2"],
  }),
  validatePluginConfig: jest.fn().mockResolvedValue({
    valid: true,
    errors: {},
  }),
}));

jest.mock("@/library/modules/rundeckClient", () => ({
  client: jest.fn(),
}));

jest.mock("@/library", () => {
  return {
    getRundeckContext: jest.fn().mockImplementation(() => ({
      client: {},
      eventBus: { on: jest.fn(), off: jest.fn(), emit: jest.fn() },
      rootStore: {
        plugins: {
          load: jest.fn(),
          getServicePlugins: jest.fn(),
        },
      },
    })),
  };
});

const createWrapper = async (props = {}): Promise<VueWrapper<any>> => {
  const wrapper = mount(WorkflowSteps, {
    props: {
      modelValue: {
        commands: [],
      },
      ...props,
    },
    global: {
      stubs: {
        // LogFilters: false,
        popover: true,
        tabs: true,
        tab: true,
      },
    },
  });
  await flushPromises();
  return wrapper;
};

describe("WorkflowSteps", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders the component with no steps", async () => {
    const wrapper = await createWrapper();
    expect(wrapper.find('[data-testid="no-steps"]').text()).toContain(
      "Workflow.noSteps",
    );
  });

  it("emits update:modelValue when a step is added", async () => {
    const wrapper = await createWrapper();
    const addButton = wrapper.find('[data-testid="add-button"]');

    await addButton.trigger("click");
    const chooseModal = wrapper.findComponent(ChoosePluginModal);
    chooseModal.vm.$emit("selected", { service: "a", provider: "b" });
    await wrapper.vm.$nextTick();
    const editModal = wrapper.findComponent(EditPluginModal);
    editModal.vm.$emit("update:modelValue", {
      type: "c",
      config: { test: true },
    });
    editModal.vm.$emit("save");

    await flushPromises();
    expect(wrapper.emitted("update:modelValue")[1][0]).toEqual({
      commands: [
        {
          configuration: {
            test: true,
          },
          description: undefined,
          nodeStep: false,
          type: "c",
        },
      ],
    });
  });

  it("emits update:modelValue when a step is removed", async () => {
    const wrapper = await createWrapper({
      modelValue: {
        commands: [{ type: "stepType", configuration: {} }],
      },
    });
    const removeButton = wrapper.find('[data-test="remove-step"]');
    await removeButton.trigger("click");
    expect(wrapper.emitted("update:modelValue")[1][0]).toEqual({
      commands: [],
    });
  });

  it("saves edited step and emits update:modelValue", async () => {
    const wrapper = await createWrapper({
      modelValue: {
        commands: [
          {
            type: "exec-command",
            nodeStep: true,
            configuration: {
              adhocRemoteString: "abc",
            },
          },
        ],
      },
    });
    const editStep = wrapper.find('[data-test="edit-step-item"]');
    await editStep.trigger("click");
    await wrapper.vm.$nextTick();
    const editModal = wrapper.findComponent(EditPluginModal);
    editModal.vm.$emit("update:modelValue", {
      type: "exec-command",
      nodeStep: true,
      config: {
        adhocRemoteString: "abcdef123",
      },
    });
    editModal.vm.$emit("save");
    await flushPromises();

    expect(wrapper.emitted("update:modelValue")[1][0]).toEqual({
      commands: [
        {
          description: undefined,
          exec: "abc",
          nodeStep: true,
          plugins: {
            LogFilter: [
              {
                config: {
                  adhocRemoteString: "abcdef123",
                },
                nodeStep: true,
                type: "exec-command",
              },
            ],
          },
        },
      ],
    });
  });
});
