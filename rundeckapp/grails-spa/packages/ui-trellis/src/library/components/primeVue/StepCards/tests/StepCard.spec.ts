import { mount } from "@vue/test-utils";
import BaseStepCard from "../BaseStepCard.vue";
import StepCardHeader from "../StepCardHeader.vue";
import StepCardContent from "../StepCardContent.vue";
import ConfigSection from "../ConfigSection.vue";
import PluginInfo from "../../../plugins/PluginInfo.vue";
import PtButton from "../../PtButton/PtButton.vue";
import StepCard from "../StepCard.vue";

jest.mock("@/library", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    eventBus: { on: jest.fn(), off: jest.fn(), emit: jest.fn() },
  }),
}));

jest.mock("@/library/modules/rundeckClient", () => ({ client: {} }));
jest.mock("@/library/components/plugins/pluginConfig.vue", () => ({ default: { name: "PluginConfig", template: "<div />" } }));
jest.mock("@/library/components/plugins/pluginPropView.vue", () => ({ default: { name: "PluginPropView", template: "<div />", props: ["prop", "value", "allowCopy"] } }));

const createWrapper = async (props = {}): Promise<any> => {
  const wrapper = mount(StepCard, {
    props: {
      pluginDetails: { title: "My Plugin", description: "Does things" },
      config: {},
      ...props,
    },
    global: {
      stubs: {
        Menu: { template: "<div />", methods: { toggle: jest.fn() } },
      },
      components: { BaseStepCard, StepCardHeader, StepCardContent, ConfigSection, PluginInfo, PtButton },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("StepCard", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("service name", () => {
    it("shows the node step tag when serviceName is WorkflowNodeStep", async () => {
      const wrapper = await createWrapper({ serviceName: "WorkflowNodeStep" });
      expect(wrapper.find('[data-testid="step-card-header-step-type-tag"]').classes()).toContain("tag-node");
    });

    it("shows the workflow step tag when serviceName is WorkflowStep", async () => {
      const wrapper = await createWrapper({ serviceName: "WorkflowStep" });
      expect(wrapper.find('[data-testid="step-card-header-step-type-tag"]').classes()).toContain("tag-workflow");
    });

    it("derives node step from config.nodeStep when serviceName is an empty string", async () => {
      const wrapper = await createWrapper({ config: { nodeStep: true }, serviceName: "" });
      expect(wrapper.find('[data-testid="step-card-header-step-type-tag"]').classes()).toContain("tag-node");
    });
  });

  describe("error handler computed props", () => {
    it("passes WorkflowNodeStep service name to content when the error handler is a node step", async () => {
      const wrapper = await createWrapper({
        errorHandler: [{ type: "exec", nodeStep: true, config: {} }],
      });
      expect(wrapper.findComponent(StepCardContent).props("errorHandlerServiceName")).toBe("WorkflowNodeStep");
    });

    it("passes WorkflowStep service name to content when the error handler is not a node step", async () => {
      const wrapper = await createWrapper({
        errorHandler: [{ type: "exec", nodeStep: false, config: {} }],
      });
      expect(wrapper.findComponent(StepCardContent).props("errorHandlerServiceName")).toBe("WorkflowStep");
    });

    it("passes the error handler provider type to content", async () => {
      const wrapper = await createWrapper({
        errorHandler: [{ type: "my-error-handler", config: {} }],
      });
      expect(wrapper.findComponent(StepCardContent).props("errorHandlerProvider")).toBe("my-error-handler");
    });

    it("passes empty string as provider when there is no error handler", async () => {
      const wrapper = await createWrapper({ errorHandler: [] });
      expect(wrapper.findComponent(StepCardContent).props("errorHandlerProvider")).toBe("");
    });
  });

  describe("events", () => {
    it("emits delete when the user clicks the delete button", async () => {
      const wrapper = await createWrapper();
      await wrapper.find('[data-testid="step-card-header-delete-btn"]').trigger("click");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("delete")).toHaveLength(1);
    });

    it("emits edit when the user clicks the plugin info area", async () => {
      const wrapper = await createWrapper();
      await wrapper.find('[data-testid="step-card-header-plugin-info"]').trigger("click");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("edit")).toHaveLength(1);
    });
  });
});
