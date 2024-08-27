import { mount, flushPromises, VueWrapper } from "@vue/test-utils";
import PluginSearch from "../PluginSearch.vue";
import { Popover } from "uiv";

const createWrapper = async (propsData = {}): Promise<VueWrapper<any>> => {
  const wrapper = mount(PluginSearch, {
    props: {
      ...propsData,
    },
    global: {
      mocks: {
        $t: (msg: string) => msg,
      },
      stubs: {
        btn: {
          template: `<button><slot></slot></button>`,
        },
        popover: true,
      },
    },
  });
  await wrapper.vm.$nextTick();
  await flushPromises();
  return wrapper;
};

describe("PluginSearch", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("emits search event with filter value on button click", async () => {
    const wrapper = await createWrapper();

    const input = wrapper.find("input[type='search']");
    await input.setValue("test filter");
    const searchButton = wrapper.find("[data-test-id='search']");
    await searchButton.trigger("click");

    expect(wrapper.emitted("search")[0]).toEqual(["test filter"]);
  });

  it("prevents form submission on enter key press", async () => {
    const wrapper = await createWrapper();

    const input = wrapper.find("input[type='search']");
    await input.setValue("test filter2");

    await input.trigger("keydown.enter");

    expect(wrapper.emitted("search")[0]).toEqual(["test filter2"]);
  });
});
