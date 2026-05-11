import pluginPropEdit from "@/library/components/plugins/pluginPropEdit.vue";
import AceEditorVue from "@/library/components/utils/AceEditorVue.vue";
import { describe, it, expect, jest, beforeEach } from "@jest/globals";
import { flushPromises, shallowMount, VueWrapper } from "@vue/test-utils";
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

import { getRundeckContext } from "../../../rundeckService";
const mockGetRundeckContext = getRundeckContext as jest.Mock;
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
describe("pluginPropEdit aceEditor computed props", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("aceEditorMinLines", () => {
    it("passes minLines of 12 to AceEditorVue when appMeta has no aceEditorMinLines", async () => {
      mockGetRundeckContext.mockReturnValueOnce({ appMeta: {} });
      const wrapper = await createCodeWrapper({ prop: codeProp });

      expect(wrapper.findComponent(AceEditorVue).props("minLines")).toBe(12);
    });

    it("passes the configured minLines to AceEditorVue when appMeta provides it", async () => {
      mockGetRundeckContext.mockReturnValueOnce({
        appMeta: { aceEditorMinLines: 20 },
      });
      const wrapper = await createCodeWrapper({ prop: codeProp });

      expect(wrapper.findComponent(AceEditorVue).props("minLines")).toBe(20);
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
