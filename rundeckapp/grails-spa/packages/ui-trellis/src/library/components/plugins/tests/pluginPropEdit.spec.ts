import pluginPropEdit from "@/library/components/plugins/pluginPropEdit.vue";
import { describe, it, jest } from "@jest/globals";
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
    })),
  };
});
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
