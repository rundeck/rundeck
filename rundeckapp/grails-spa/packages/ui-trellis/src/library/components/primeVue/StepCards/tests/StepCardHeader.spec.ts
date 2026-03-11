import { mount } from "@vue/test-utils";
import PtButton from "../../PtButton/PtButton.vue";
import StepCardHeader from "../StepCardHeader.vue";

const createWrapper = async (props = {}): Promise<any> => {
  const wrapper = mount(StepCardHeader, {
    props: {
      pluginDetails: { title: "My Plugin", description: "Plugin description" },
      config: {},
      ...props,
    },
    global: {
      stubs: {
        Menu: { template: "<div />", methods: { toggle: jest.fn() } },
      },
      components: { PtButton },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("StepCardHeader", () => {
  describe("step type tag", () => {
    it("shows the node step tag when showAsNodeStep is explicitly true", async () => {
      const wrapper = await createWrapper({ showAsNodeStep: true });
      expect(wrapper.find('[data-testid="step-card-header-step-type-tag"]').classes()).toContain("tag-node");
    });

    it("shows the workflow step tag when showAsNodeStep is explicitly false", async () => {
      const wrapper = await createWrapper({ showAsNodeStep: false });
      expect(wrapper.find('[data-testid="step-card-header-step-type-tag"]').classes()).toContain("tag-workflow");
    });

    it("shows the node step tag when config.nodeStep is true and showAsNodeStep is not provided", async () => {
      const wrapper = await createWrapper({ config: { nodeStep: true } });
      expect(wrapper.find('[data-testid="step-card-header-step-type-tag"]').classes()).toContain("tag-node");
    });

    it("shows the workflow step tag by default when neither showAsNodeStep nor config.nodeStep is set", async () => {
      const wrapper = await createWrapper({ config: {} });
      expect(wrapper.find('[data-testid="step-card-header-step-type-tag"]').classes()).toContain("tag-workflow");
    });
  });

  describe("delete button", () => {
    it("emits delete when the user clicks the delete button", async () => {
      const wrapper = await createWrapper();
      await wrapper.find('[data-testid="step-card-header-delete-btn"]').trigger("click");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("delete")).toHaveLength(1);
    });
  });

  describe("edit action", () => {
    it("emits edit when the user clicks the plugin info area", async () => {
      const wrapper = await createWrapper();
      await wrapper.find('[data-testid="step-card-header-plugin-info"]').trigger("click");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("edit")).toHaveLength(1);
    });

    it("does not emit edit when the component is disabled", async () => {
      const wrapper = await createWrapper({ disabled: true });
      await wrapper.find('[data-testid="step-card-header-plugin-info"]').trigger("click");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("edit")).toBeFalsy();
    });
  });

  describe("toggle vs more actions button", () => {
    it("shows the toggle button when showToggle is true", async () => {
      const wrapper = await createWrapper({ showToggle: true });
      expect(wrapper.find('[data-testid="step-card-header-toggle-btn"]').exists()).toBe(true);
    });

    it("hides the toggle button when showToggle is false", async () => {
      const wrapper = await createWrapper({ showToggle: false });
      expect(wrapper.find('[data-testid="step-card-header-toggle-btn"]').exists()).toBe(false);
    });

    it("shows the more actions button when showToggle is false", async () => {
      const wrapper = await createWrapper({ showToggle: false });
      expect(wrapper.find('[data-testid="step-card-header-more-btn"]').exists()).toBe(true);
    });

    it("hides the more actions button when showToggle is true", async () => {
      const wrapper = await createWrapper({ showToggle: true });
      expect(wrapper.find('[data-testid="step-card-header-more-btn"]').exists()).toBe(false);
    });

    it("emits toggle when the user clicks the toggle button", async () => {
      const wrapper = await createWrapper({ showToggle: true });
      await wrapper.find('[data-testid="step-card-header-toggle-btn"]').trigger("click");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("toggle")).toHaveLength(1);
    });
  });

  describe("validation error tag", () => {
    it("shows the error tag when editing and there are validation errors", async () => {
      const wrapper = await createWrapper({
        editing: true,
        validationErrors: { valid: false, errors: { name: "Required" } },
      });
      expect(wrapper.find('[data-testid="step-card-header-error-tag"]').exists()).toBe(true);
    });

    it("does not show the error tag when validation passes", async () => {
      const wrapper = await createWrapper({
        editing: true,
        validationErrors: { valid: true, errors: {} },
      });
      expect(wrapper.find('[data-testid="step-card-header-error-tag"]').exists()).toBe(false);
    });

    it("does not show the error tag when not editing even if there are errors", async () => {
      const wrapper = await createWrapper({
        editing: false,
        validationErrors: { valid: false, errors: { name: "Required" } },
      });
      expect(wrapper.find('[data-testid="step-card-header-error-tag"]').exists()).toBe(false);
    });
  });

  describe("plugin description", () => {
    it("shows the plugin description when in editing mode", async () => {
      const wrapper = await createWrapper({
        editing: true,
        pluginDetails: { title: "My Plugin", description: "Does useful things" },
      });
      const desc = wrapper.find('[data-testid="step-card-header-plugin-desc"]');
      expect(desc.exists()).toBe(true);
      expect(desc.text()).toBe("Does useful things");
    });

    it("does not show the plugin description text when not in editing mode", async () => {
      const wrapper = await createWrapper({ editing: false });
      expect(wrapper.find('[data-testid="step-card-header-plugin-desc"]').exists()).toBe(false);
    });
  });
});
