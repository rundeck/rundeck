import { mount } from "@vue/test-utils";
import Chip from "primevue/chip";
import ConfigSection from "../ConfigSection.vue";
import PluginInfo from "../../../plugins/PluginInfo.vue";
import StepCardContent from "../StepCardContent.vue";

jest.mock("@/library", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    eventBus: { on: jest.fn(), off: jest.fn(), emit: jest.fn() },
  }),
}));

jest.mock("@/library/modules/rundeckClient", () => ({ client: {} }));
jest.mock("@/library/components/plugins/pluginConfig.vue", () => ({ default: { name: "PluginConfig", inheritAttrs: false, template: '<div v-bind="$attrs" />' } }));

const createWrapper = async (props = {}): Promise<any> => {
  const wrapper = mount(StepCardContent, {
    props: {
      config: { type: "exec", config: {} },
      elementId: "step-1",
      ...props,
    },
    global: {
      components: { ConfigSection, PluginInfo, Chip },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("StepCardContent", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("job reference vs plugin config display", () => {
    it("shows the job reference section when config has a jobref", async () => {
      const wrapper = await createWrapper({
        config: { jobref: { name: "my-job", uuid: "abc-123" } },
      });
      expect(wrapper.find('[data-testid="step-card-content-jobref-section"]').exists()).toBe(true);
    });

    it("does not show the job reference section when config has no jobref", async () => {
      const wrapper = await createWrapper({ config: { type: "exec", config: {} } });
      expect(wrapper.find('[data-testid="step-card-content-jobref-section"]').exists()).toBe(false);
    });

    it("shows the plugin config section when there is no jobref", async () => {
      const wrapper = await createWrapper({ config: { type: "exec", config: {} } });
      expect(wrapper.find('[data-testid="step-card-content-plugin-config"]').exists()).toBe(true);
    });

    it("does not show the plugin config section when config has a jobref", async () => {
      const wrapper = await createWrapper({
        config: { jobref: { name: "my-job" } },
      });
      expect(wrapper.find('[data-testid="step-card-content-plugin-config"]').exists()).toBe(false);
    });
  });

  describe("log filters config section", () => {
    it("shows the log filters section when hideConfigSection is false and there is no jobref", async () => {
      const wrapper = await createWrapper({ hideConfigSection: false });
      expect(wrapper.find('[data-testid="step-card-content-log-filters"]').exists()).toBe(true);
    });

    it("hides the log filters section when hideConfigSection is true", async () => {
      const wrapper = await createWrapper({ hideConfigSection: true });
      expect(wrapper.find('[data-testid="step-card-content-log-filters"]').exists()).toBe(false);
    });

    it("hides the log filters section when config has a jobref even if hideConfigSection is false", async () => {
      const wrapper = await createWrapper({
        config: { jobref: { name: "my-job" } },
        hideConfigSection: false,
      });
      expect(wrapper.find('[data-testid="step-card-content-log-filters"]').exists()).toBe(false);
    });
  });

  describe("error handler config section", () => {
    it("shows the error handler section when hideConfigSection is false", async () => {
      const wrapper = await createWrapper({ hideConfigSection: false });
      expect(wrapper.find('[data-testid="step-card-content-error-handler"]').exists()).toBe(true);
    });

    it("hides the error handler section when hideConfigSection is true", async () => {
      const wrapper = await createWrapper({ hideConfigSection: true });
      expect(wrapper.find('[data-testid="step-card-content-error-handler"]').exists()).toBe(false);
    });
  });

  describe("adding items", () => {
    it("emits add-log-filter with the elementId when the log filters add button is clicked", async () => {
      // errorHandler non-empty so its ConfigSection shows a chip, not an add button
      const wrapper = await createWrapper({
        logFilters: [],
        errorHandler: [{ type: "exec", title: "Handler" }],
        elementId: "my-step",
      });
      await wrapper.find('[data-testid="config-section-add-btn"]').trigger("click");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("add-log-filter")).toHaveLength(1);
      expect(wrapper.emitted("add-log-filter")![0]).toEqual(["my-step"]);
    });

    it("emits add-error-handler with the elementId when the error handler add button is clicked", async () => {
      // logFilters non-empty so its ConfigSection shows a chip, not an add button
      const wrapper = await createWrapper({
        logFilters: [{ type: "logging/mask-passwords", title: "Filter" }],
        errorHandler: [],
        elementId: "my-step",
      });
      await wrapper.find('[data-testid="config-section-add-btn"]').trigger("click");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("add-error-handler")).toHaveLength(1);
      expect(wrapper.emitted("add-error-handler")![0]).toEqual(["my-step"]);
    });
  });

  describe("editing items", () => {
    it("emits edit-log-filter with the filter and its index when a log filter chip is clicked", async () => {
      const filter = { type: "logging/mask-passwords", title: "Mask Passwords" };
      const wrapper = await createWrapper({
        logFilters: [filter],
        errorHandler: [],
        elementId: "step-1",
      });
      await wrapper.findAllComponents(Chip)[0].trigger("click");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("edit-log-filter")).toHaveLength(1);
      expect(wrapper.emitted("edit-log-filter")![0]).toEqual([{ filter, index: 0 }]);
    });

    it("does not emit edit-error-handler when there is no error handler configured", async () => {
      const wrapper = await createWrapper({ errorHandler: [] });
      // handleEditErrorHandler has a guard: returns early if errorHandlerData is null
      // Simulate the ConfigSection emitting editElement with no data
      await wrapper.findComponent({ name: "ConfigSection" }).vm.$emit("editElement", null, 0);
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("edit-error-handler")).toBeFalsy();
    });
  });

  describe("removing items", () => {
    it("emits remove-error-handler with the elementId when an error handler chip is removed", async () => {
      const wrapper = await createWrapper({
        logFilters: [],
        errorHandler: [{ type: "exec", title: "Error Handler" }],
        elementId: "step-1",
      });
      await wrapper.findAllComponents(Chip)[0].vm.$emit("remove");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("remove-error-handler")).toHaveLength(1);
      expect(wrapper.emitted("remove-error-handler")![0]).toEqual(["step-1"]);
    });
  });

  describe("job reference full name", () => {
    it("builds the full name as group/name when both are present", async () => {
      const wrapper = await createWrapper({
        config: { jobref: { group: "ops", name: "deploy", uuid: "abc" } },
      });
      expect(wrapper.find('[data-testid="step-card-content-jobref-section"]').text()).toContain("ops/deploy");
    });

    it("uses just the name when there is no group", async () => {
      const wrapper = await createWrapper({
        config: { jobref: { name: "deploy", uuid: "abc" } },
      });
      expect(wrapper.find('[data-testid="step-card-content-jobref-section"]').text()).toContain("deploy");
    });

    it("falls back to uuid when there is no name", async () => {
      const wrapper = await createWrapper({
        config: { jobref: { uuid: "abc-123" } },
      });
      expect(wrapper.find('[data-testid="step-card-content-jobref-section"]').text()).toContain("abc-123");
    });
  });
});
