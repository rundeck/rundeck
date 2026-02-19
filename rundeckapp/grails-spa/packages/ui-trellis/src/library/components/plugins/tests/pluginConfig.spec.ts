import { shallowMount } from "@vue/test-utils";
import PluginConfig from "../pluginConfig.vue";
import PluginPropView from "../pluginPropView.vue";
import PluginPropEdit from "../pluginPropEdit.vue";

jest.mock("@/library/modules/rundeckClient", () => ({
  client: jest.fn(),
}));

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rootStore: {
      plugins: {
        getPluginDetail: jest.fn().mockResolvedValue({ props: [] }),
      },
    },
  }),
}));

jest.mock("@/library/modules/pluginService", () => ({
  getPluginProvidersForService: jest.fn().mockResolvedValue({
    service: true,
    descriptions: [],
    labels: [],
  }),
}));

jest.mock("@/library/modules/InputUtils", () => ({
  cleanConfigInput: jest.fn((v) => v),
  convertArrayInput: jest.fn((v) => v),
}));

interface MountOptions {
  props?: Record<string, any>;
}

const createWrapper = async (options: MountOptions = {}) => {
  const wrapper = shallowMount(PluginConfig, {
    props: {
      mode: "create",
      modelValue: { type: "test-plugin", config: {} },
      pluginConfig: { props: [] },
      ...options.props,
    },
    global: {
      stubs: {
        PluginInfo: true,
        PluginPropView: true,
        PluginPropEdit: true,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("PluginConfig", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("mode-based rendering", () => {
    it("renders PluginPropView for each prop that has a value in config in show mode", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "show",
          config: { host: "localhost" },
          pluginConfig: {
            props: [{ name: "host", type: "String", options: {} }],
          },
        },
      });

      expect(wrapper.findAllComponents(PluginPropView)).toHaveLength(1);
    });

    it("renders a form field for each prop in edit mode", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          modelValue: { type: "test", config: {} },
          pluginConfig: {
            props: [
              { name: "host", type: "String", options: {} },
              { name: "port", type: "Integer", options: {} },
            ],
          },
        },
      });

      expect(wrapper.find('[data-testid="prop-field-host"]').exists()).toBe(true);
      expect(wrapper.find('[data-testid="prop-field-port"]').exists()).toBe(true);
    });

    it("does not render form fields in show mode", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "show",
          config: {},
          pluginConfig: { props: [{ name: "host", type: "String", options: {} }] },
        },
      });

      expect(wrapper.find('[data-testid="prop-field-host"]').exists()).toBe(false);
    });
  });

  describe("scope-based field visibility", () => {
    it("renders props that have no scope constraint regardless of the component scope", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          scope: "Instance",
          modelValue: { type: "test", config: {} },
          pluginConfig: {
            props: [{ name: "host", type: "String", options: {} }],
          },
        },
      });

      expect(wrapper.find('[data-testid="prop-field-host"]').exists()).toBe(true);
    });

    it("hides props scoped to Framework when the component scope is Instance", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          scope: "Instance",
          modelValue: { type: "test", config: {} },
          pluginConfig: {
            props: [{ name: "host", type: "String", scope: "Framework", options: {} }],
          },
        },
      });

      expect(wrapper.find('[data-testid="prop-field-host"]').exists()).toBe(false);
    });

    it("shows props scoped to InstanceOnly when the component scope is Instance", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          scope: "Instance",
          modelValue: { type: "test", config: {} },
          pluginConfig: {
            props: [{ name: "host", type: "String", scope: "InstanceOnly", options: {} }],
          },
        },
      });

      expect(wrapper.find('[data-testid="prop-field-host"]').exists()).toBe(true);
    });

    it("shows Project-scoped props when the component scope is Framework", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          scope: "Framework",
          modelValue: { type: "test", config: {} },
          pluginConfig: {
            props: [{ name: "host", type: "String", scope: "Project", options: {} }],
          },
        },
      });

      expect(wrapper.find('[data-testid="prop-field-host"]').exists()).toBe(true);
    });

    it("hides Instance-scoped props when the component scope is Framework", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          scope: "Framework",
          modelValue: { type: "test", config: {} },
          pluginConfig: {
            props: [{ name: "host", type: "String", scope: "InstanceOnly", options: {} }],
          },
        },
      });

      expect(wrapper.find('[data-testid="prop-field-host"]').exists()).toBe(false);
    });

    it("shows props with Unspecified scope regardless of the component scope", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          scope: "Instance",
          modelValue: { type: "test", config: {} },
          pluginConfig: {
            props: [{ name: "host", type: "String", scope: "Unspecified", options: {} }],
          },
        },
      });

      expect(wrapper.find('[data-testid="prop-field-host"]').exists()).toBe(true);
    });
  });

  describe("property grouping", () => {
    it("renders ungrouped props directly in the form without a details section", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          modelValue: { type: "test", config: {} },
          pluginConfig: {
            props: [{ name: "host", type: "String", options: {} }],
          },
        },
      });

      expect(wrapper.find('[data-testid="prop-section-Auth"]').exists()).toBe(false);
      expect(wrapper.find('[data-testid="prop-field-host"]').exists()).toBe(true);
    });

    it("renders named group props inside a collapsible details section", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          modelValue: { type: "test", config: {} },
          pluginConfig: {
            props: [
              { name: "user", type: "String", options: { groupName: "Auth" } },
              { name: "pass", type: "String", options: { groupName: "Auth" } },
            ],
          },
        },
      });

      expect(wrapper.find('[data-testid="prop-section-Auth"]').exists()).toBe(true);
      expect(wrapper.find('[data-testid="prop-field-user"]').exists()).toBe(true);
      expect(wrapper.find('[data-testid="prop-field-pass"]').exists()).toBe(true);
    });

    it("renders a primary named group as an open details section", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          modelValue: { type: "test", config: {} },
          pluginConfig: {
            props: [{ name: "user", type: "String", options: { groupName: "Auth" } }],
          },
        },
      });

      // open=!group.secondary=!false → open="" (HTML boolean attribute)
      expect(wrapper.find('[data-testid="prop-section-Auth"]').attributes("open")).toBe("");
    });

    it("renders a secondary group as a collapsed details section by default", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          modelValue: { type: "test", config: {} },
          pluginConfig: {
            props: [{ name: "extra", type: "String", options: { grouping: true } }],
          },
        },
      });

      // open=!group.secondary=!true → open not set (collapsed)
      expect(wrapper.find('[data-testid="prop-section--"]').attributes("open")).toBeUndefined();
    });
  });

  describe("hidden props", () => {
    it("renders a hidden input instead of a visible form field for HIDDEN display type props", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          modelValue: { type: "test", config: {} },
          pluginConfig: {
            props: [{ name: "secret", type: "String", options: { displayType: "HIDDEN" } }],
          },
        },
      });

      expect(wrapper.find('[data-testid="prop-hidden-secret"]').exists()).toBe(true);
      expect(wrapper.find('[data-testid="prop-field-secret"]').exists()).toBe(false);
    });
  });

  describe("show mode prop visibility", () => {
    it("renders PluginPropView for a String prop that has a value in config", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "show",
          config: { host: "localhost" },
          pluginConfig: { props: [{ name: "host", type: "String", options: {} }] },
        },
      });

      expect(wrapper.findAllComponents(PluginPropView)).toHaveLength(1);
    });

    it("does not render PluginPropView for a String prop with no value in config", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "show",
          config: {},
          pluginConfig: { props: [{ name: "host", type: "String", options: {} }] },
        },
      });

      expect(wrapper.findAllComponents(PluginPropView)).toHaveLength(0);
    });

    it("always renders PluginPropView for a Boolean prop even when not present in config", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "show",
          config: {},
          pluginConfig: { props: [{ name: "enabled", type: "Boolean", options: {} }] },
        },
      });

      expect(wrapper.findAllComponents(PluginPropView)).toHaveLength(1);
    });
  });

  describe("input initialization", () => {
    it("passes an array to PluginPropEdit for an Options prop with a comma-separated string value", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          modelValue: { type: "test", config: { tags: "red, blue, green" } },
          pluginConfig: { props: [{ name: "tags", type: "Options", options: {} }] },
        },
      });

      expect(wrapper.findComponent(PluginPropEdit).props("modelValue")).toEqual(["red", "blue", "green"]);
    });

    it("passes an empty array to PluginPropEdit for an Options prop with no value in config", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          modelValue: { type: "test", config: {} },
          pluginConfig: { props: [{ name: "tags", type: "Options", options: {} }] },
        },
      });

      expect(wrapper.findComponent(PluginPropEdit).props("modelValue")).toEqual([]);
    });

    it("passes an empty string to PluginPropEdit for an undefined Select prop", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          modelValue: { type: "test", config: {} },
          pluginConfig: { props: [{ name: "strategy", type: "Select", options: {} }] },
        },
      });

      expect(wrapper.findComponent(PluginPropEdit).props("modelValue")).toBe("");
    });

    it("converts a string 'true' Boolean config value to a real boolean for the form", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          modelValue: { type: "test", config: { enabled: "true" } },
          pluginConfig: { props: [{ name: "enabled", type: "Boolean", options: {} }] },
        },
      });

      expect(wrapper.findComponent(PluginPropEdit).props("modelValue")).toBe(true);
    });

    it("applies the prop default value in create mode when no config value exists", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "create",
          modelValue: { type: "test", config: {} },
          pluginConfig: {
            props: [{ name: "timeout", type: "String", defaultValue: "30", options: {} }],
          },
        },
      });

      expect(wrapper.findComponent(PluginPropEdit).props("modelValue")).toBe("30");
    });

    it("does not apply the prop default in edit mode when no config value exists", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          modelValue: { type: "test", config: {} },
          pluginConfig: {
            props: [{ name: "timeout", type: "String", defaultValue: "30", options: {} }],
          },
        },
      });

      // Default must not be applied — value should be absent/empty, not "30"
      expect(wrapper.findComponent(PluginPropEdit).props("modelValue")).not.toBe("30");
    });
  });

  describe("boolean export", () => {
    it("emits the string 'true' in the config payload when a Boolean field is set to true", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          modelValue: { type: "test", config: {} },
          pluginConfig: {
            props: [{ name: "enabled", type: "Boolean", options: {} }],
          },
        },
      });

      await wrapper.findComponent(PluginPropEdit).vm.$emit("update:modelValue", true);
      await wrapper.vm.$nextTick();

      const emitted = wrapper.emitted("update:modelValue");
      expect(emitted).toHaveLength(1);
      expect((emitted[0][0] as any).config.enabled).toBe("true");
    });

    it("emits the string 'false' in the config payload when a Boolean with a true default is set to false", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          modelValue: { type: "test", config: {} },
          pluginConfig: {
            props: [{ name: "enabled", type: "Boolean", defaultValue: "true", options: {} }],
          },
        },
      });

      await wrapper.findComponent(PluginPropEdit).vm.$emit("update:modelValue", false);
      await wrapper.vm.$nextTick();

      const emitted = wrapper.emitted("update:modelValue");
      expect((emitted[emitted.length - 1][0] as any).config.enabled).toBe("false");
    });

    it("omits the boolean key from the config payload when the value is false with no true default", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          modelValue: { type: "test", config: {} },
          pluginConfig: {
            props: [{ name: "enabled", type: "Boolean", options: {} }],
          },
        },
      });

      await wrapper.findComponent(PluginPropEdit).vm.$emit("update:modelValue", false);
      await wrapper.vm.$nextTick();

      const emitted = wrapper.emitted("update:modelValue");
      expect((emitted[emitted.length - 1][0] as any).config.enabled).toBeUndefined();
    });
  });

  describe("validation state", () => {
    it("applies the required class to the form group when the prop is marked required", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          modelValue: { type: "test", config: {} },
          pluginConfig: {
            props: [{ name: "host", type: "String", required: true, options: {} }],
          },
        },
      });

      expect(wrapper.find('[data-testid="prop-field-host"]').classes()).toContain("required");
    });

    it("applies the has-error class when validation errors exist for a prop", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          modelValue: { type: "test", config: {} },
          validation: { valid: false, errors: { host: "is required" } },
          pluginConfig: { props: [{ name: "host", type: "String", options: {} }] },
        },
      });

      expect(wrapper.find('[data-testid="prop-field-host"]').classes()).toContain("has-error");
    });

    it("does not apply the has-error class when there are no validation errors", async () => {
      const wrapper = await createWrapper({
        props: {
          mode: "edit",
          modelValue: { type: "test", config: {} },
          validation: { valid: true, errors: {} },
          pluginConfig: { props: [{ name: "host", type: "String", options: {} }] },
        },
      });

      expect(wrapper.find('[data-testid="prop-field-host"]').classes()).not.toContain("has-error");
    });
  });
});
