import {
  flushPromises,
  mount,
  shallowMount,
  VueWrapper,
} from "@vue/test-utils";
import ErrorHandlerStep from "../ErrorHandlerStep.vue";
import PluginConfig from "@/library/components/plugins/pluginConfig.vue";
import JobRefStep from "@/app/components/job/workflow/JobRefStep.vue";

jest.mock("@/library/modules/pluginService", () => ({
  getPluginProvidersForService: jest.fn().mockResolvedValue({
    service: "OptionValues",
    descriptions: [],
    labels: [],
  }),
}));
jest.mock("@/library/modules/rundeckClient", () => ({
  client: {},
}));
jest.mock("@/library/rundeckService.ts", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: { on: jest.fn(), emit: jest.fn() },
    rdBase: "http://localhost:4440",
  })),
}));

const step = {
  errorhandler: {
    type: "exec-command",
    config: {},
    nodeStep: false,
    keepgoingOnSuccess: false,
    jobref: undefined,
  },
  jobref: undefined,
};

const createWrapper = async (props = {}): Promise<VueWrapper<any>> => {
  const wrapper = shallowMount(ErrorHandlerStep, {
    props: {
      step: {
        ...step,
        errorhandler: {
          ...step.errorhandler,
          ...props,
        },
      },
    },
    global: {
      stubs: {
        PluginConfig: {
          template: `<div><slot /><slot name="iconSuffix" /></div>`,
        },
        JobRefStep: true,
      },
    },
  });
  await wrapper.vm.$nextTick();
  await flushPromises();
  return wrapper;
};

describe("ErrorHandlerStep", () => {
  it("renders the keep going on success if error handler has this option enabled", async () => {
    const wrapper = await createWrapper({
      keepgoingOnSuccess: true,
    });

    const keepGoingText = wrapper.find("[data-testid='keepgoingOnSuccess']");
    expect(keepGoingText.text()).toBe(
      "Workflow.stepErrorHandler.label.keep.going.on.success",
    );
  });

  it("does not render the keep going on success if error handler has this option disabled", async () => {
    const wrapper = await createWrapper({
      keepgoingOnSuccess: false,
    });

    const keepGoingText = wrapper.find("[data-testid='keepgoingOnSuccess']");
    expect(keepGoingText.exists()).toBe(false);
  });

  it("emits removeHandler event upon clicking to remove it", async () => {
    const wrapper = await createWrapper();
    const removeButton = wrapper.find("[data-testid='remove-handler-button']");

    await removeButton.trigger("click");

    expect(wrapper.emitted("removeHandler")).toBeTruthy();
    expect(wrapper.emitted("removeHandler")[0][0]).toEqual(step);
  });

  it("renders plugin config for non-job-ref error handlers", async () => {
    const wrapper = await createWrapper({
      config: { command: "test" },
    });

    expect(wrapper.findComponent(PluginConfig).exists()).toBe(true);
    expect(wrapper.findComponent(JobRefStep).exists()).toBe(false);
  });

  it("renders job ref step for job reference error handlers", async () => {
    const wrapper = await createWrapper({
      jobref: {
        name: "test-job",
        group: "test-group",
      },
    });

    expect(wrapper.findComponent(JobRefStep).exists()).toBe(true);
    expect(wrapper.findComponent(PluginConfig).exists()).toBe(false);
  });

  it("shows node step icon when error handler is a node step", async () => {
    const wrapper = await createWrapper({
      nodeStep: true,
    });

    const nodeIcon = wrapper.find(".fa-hdd");
    expect(nodeIcon.exists()).toBe(true);
  });

  it("emits edit event when clicking the configuration area", async () => {
    const wrapper = await createWrapper();
    const configArea = wrapper.find(".configuration");

    await configArea.trigger("click");

    expect(wrapper.emitted("edit")).toBeTruthy();
  });
});
