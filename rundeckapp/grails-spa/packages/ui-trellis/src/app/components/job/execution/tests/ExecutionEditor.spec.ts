import { flushPromises, shallowMount, VueWrapper } from "@vue/test-utils";
import ExecutionEditor from "../ExecutionEditor.vue";
import { executionLifecycle, pluginsInitialData } from "./mocks";
import PluginConfig from "../../../../../library/components/plugins/pluginConfig.vue";

jest.mock("@/library/modules/rundeckClient", () => ({
  client: {},
}));

const createWrapper = async (propsData = {}): Promise<VueWrapper<any>> => {
  const wrapper = shallowMount(ExecutionEditor, {
    props: {
      initialData: null,
      modelValue: {
        ExecutionLifecycle: {},
      },
      ...propsData,
    },
    global: {
      mocks: {
        $t: (msg: string) => msg,
      },
      stubs: {
        pluginConfig: {
          props: ["showIcon", "showTitle", "showDescription", "provider"],
          data() {
            return {
              inputShowTitle: this.showTitle !== null ? this.showTitle : true,
              inputShowIcon: this.showIcon !== null ? this.showIcon : true,
              inputShowDescription:
                this.showDescription !== null ? this.showDescription : true,
              detail: {
                desc: this.provider || "", // bypassing to avoid mocking up all api calls
              },
            };
          },
          template: `<div><slot name="header" :header="{
            inputShowTitle,
            inputShowIcon,
            inputShowDescription,
            detail,
          }"></slot></div>`,
        },
      },
    },
  });
  await wrapper.vm.$nextTick();
  await flushPromises();
  return wrapper;
};

describe("ExecutionEditor", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("emits update:modelValue with formatted data", async () => {
    const wrapper = await createWrapper();

    // as the initial data is coming from gsp, the component is initially mounted without the initialData
    // therefore using setProps to simulate this behaviour
    await wrapper.setProps({
      initialData: {
        pluginsInitialData,
        ExecutionLifecycle: {},
      },
    });
    await wrapper.vm.$nextTick();

    // triggering the click in all checkboxes
    const checkboxes = wrapper.findAll("input[type=checkbox]");
    for (const checkbox of checkboxes) {
      await checkbox.setValue();
    }

    // emitting data from pluginConfig that differs from the original data, to simulate editing behaviour
    const pluginConfigs = wrapper.findAllComponents(PluginConfig);
    pluginConfigs.forEach((pluginComponent) => {
      const configToEmit = executionLifecycle[pluginComponent.vm.provider];
      pluginComponent.vm.$emit("update:modelValue", {
        config: configToEmit,
        type: undefined,
      });
    });

    await wrapper.vm.$nextTick();

    // as checkboxes were clicked first and then the plugin data updated, update:modelValue is triggered at first with empty objects
    // therefore for will check that the last event emitted has all the plugin data
    const numberOfEventsEmitted = wrapper.emitted("update:modelValue").length;

    expect(
      wrapper.emitted("update:modelValue")[numberOfEventsEmitted - 1],
    ).toEqual([
      expect.objectContaining({
        ExecutionLifecycle: executionLifecycle,
      }),
    ]);
  });
});
