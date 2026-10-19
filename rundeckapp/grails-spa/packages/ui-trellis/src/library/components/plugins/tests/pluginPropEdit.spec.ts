import pluginPropEdit from "@/library/components/plugins/pluginPropEdit.vue";
import AceEditorVue from "@/library/components/utils/AceEditorVue.vue";
import { describe, it, expect, jest, beforeEach } from "@jest/globals";
import {
  config,
  flushPromises,
  mount,
  shallowMount,
  VueWrapper,
} from "@vue/test-utils";
jest.mock("../../../modules/rundeckClient", () => ({}));

jest.mock("@/library/rundeckService", () => {
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
      appMeta: {},
    })),
  };
});

const { getRundeckContext } = jest.requireMock("@/library/rundeckService") as {
  getRundeckContext: jest.Mock;
};
const mockGetRundeckContext = getRundeckContext;
const createWrapper = async (propsData = {}): Promise<VueWrapper<any>> => {
  const wrapper = shallowMount(pluginPropEdit, {
    props: {
      ...propsData,
    },
    global: {},
  });
  await flushPromises();
  return wrapper;
};

const codeProp = {
  type: "String",
  title: "Script",
  name: "script",
  required: false,
  desc: "",
  options: { displayType: "CODE", codeSyntaxMode: "sh" },
};

const createCodeWrapper = async (propsData = {}): Promise<VueWrapper<any>> => {
  const wrapper = shallowMount(pluginPropEdit, {
    props: { modelValue: "", selectorData: {}, ...propsData },
    global: {
      stubs: { UiSocket: { template: "<div><slot /></div>" } },
    },
  });
  await flushPromises();
  return wrapper;
};
const numberProp = (type: string) => ({
  type,
  title: "Maximum resources allowed to retrieve",
  name: "maximumResources",
});

const createNumberWrapper = async (
  propsData = {},
): Promise<VueWrapper<any>> => {
  const wrapper = mount(pluginPropEdit, {
    props: {
      modelValue: "100",
      rkey: "test_",
      validation: null,
      readOnly: false,
      selectorData: {},
      ...propsData,
    },
    global: {
      stubs: { UiSocket: true },
    },
  });
  await flushPromises();
  return wrapper;
};

describe("pluginPropEdit aceEditor computed props", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("aceEditorMinLines", () => {
    it("passes minLines of 0 to AceEditorVue when appMeta has no aceEditorMinLines", async () => {
      mockGetRundeckContext.mockReturnValueOnce({ appMeta: {} });
      const wrapper = await createCodeWrapper({ prop: codeProp });

      expect(wrapper.findComponent(AceEditorVue).props("minLines")).toBe(0);
    });

    it("passes the configured minLines to AceEditorVue when appMeta provides it", async () => {
      mockGetRundeckContext.mockReturnValueOnce({
        appMeta: { aceEditorMinLines: 30 },
      });
      const wrapper = await createCodeWrapper({ prop: codeProp });

      expect(wrapper.findComponent(AceEditorVue).props("minLines")).toBe(30);
    });
  });

  describe("aceEditorMaxLines", () => {
    it("passes Infinity to AceEditorVue when appMeta has no aceEditorMaxLines", async () => {
      mockGetRundeckContext.mockReturnValueOnce({ appMeta: {} });
      const wrapper = await createCodeWrapper({ prop: codeProp });

      expect(wrapper.findComponent(AceEditorVue).props("maxLines")).toBe(
        Infinity,
      );
    });

    it("passes Infinity to AceEditorVue when aceEditorMaxLines is 0", async () => {
      mockGetRundeckContext.mockReturnValueOnce({
        appMeta: { aceEditorMaxLines: 0 },
      });
      const wrapper = await createCodeWrapper({ prop: codeProp });

      expect(wrapper.findComponent(AceEditorVue).props("maxLines")).toBe(
        Infinity,
      );
    });

    it("passes the configured maxLines to AceEditorVue when set to a positive integer", async () => {
      mockGetRundeckContext.mockReturnValueOnce({
        appMeta: { aceEditorMaxLines: 50 },
      });
      const wrapper = await createCodeWrapper({ prop: codeProp });

      expect(wrapper.findComponent(AceEditorVue).props("maxLines")).toBe(50);
    });
  });
});

describe("pluginPropEdit", () => {
  it.each([true, false])(
    "hides the label when labelHidden option is %p for text property",
    async (hidden: boolean) => {
      const wrapper = await createWrapper({
        modelValue: "test model value",
        prop: {
          type: "String",
          title: "Property Name",
          name: "prop1",
          options: {
            labelHidden: hidden.toString(),
          },
        },
        rkey: "test_",
        validation: null,
        readOnly: false,
        selectorData: {},
      });

      const input = await wrapper.get("input#test_prop_0");
      expect((input.element as HTMLInputElement).value).toBe(
        "test model value",
      );
      const label = wrapper.find("[data-testid='plugin-prop-label']");
      expect(label.exists()).toBe(!hidden);
    },
  );
  it.each([
    [12, true],
    [10, false],
  ])(
    "inputColSize value is %p when labelHidden option is %p for text property",
    async (size: number, hidden: boolean) => {
      const prop = {
        type: "String",
        title: "Property Name",
        name: "prop1",
        options: {
          labelHidden: hidden.toString(),
        },
      };
      const wrapper = await createWrapper({
        modelValue: "test model value",
        prop,
        rkey: "test_",
        validation: null,
        readOnly: false,
        selectorData: {},
      });

      expect(wrapper.vm.inputColSize(prop)).toBe("col-sm-" + size);
    },
  );
});

describe("pluginPropEdit numeric property", () => {
  it.each(["Integer", "Long"])(
    "renders an editable input for a %s property and emits the typed value",
    async (type: string) => {
      const wrapper = await createNumberWrapper({ prop: numberProp(type) });

      const input = wrapper.get(
        "[data-testid='plugin-prop-number-input'] input",
      );
      expect((input.element as HTMLInputElement).value).toBe("100");

      await input.setValue("500");

      expect(wrapper.emitted("update:modelValue")).toContainEqual(["500"]);
    },
  );

  it("cannot render the numeric input when the app does not install PrimeVue", async () => {
    const plugins = config.global.plugins;
    config.global.plugins = [];
    try {
      await expect(
        createNumberWrapper({ prop: numberProp("Integer") }),
      ).rejects.toThrow();
    } finally {
      config.global.plugins = plugins;
    }
  });
});
