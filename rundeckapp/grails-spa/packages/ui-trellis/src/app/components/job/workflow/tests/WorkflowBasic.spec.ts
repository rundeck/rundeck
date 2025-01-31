import { mount } from "@vue/test-utils";
import WorkflowBasic from "../WorkflowBasic.vue";

const createWrapper = async (props = {}) => {
  return mount(WorkflowBasic, {
    props: {
      modelValue: { keepgoing: false },
      ...props,
    },
  });
};

describe("WorkflowBasic", () => {
  it("starts with the second radio button checked when keepgoing is true", async () => {
    const wrapper = await createWrapper({ modelValue: { keepgoing: true } });
    const radioButtons = wrapper.findAll("input[type='radio']");

    expect((radioButtons[0].element as HTMLInputElement).checked).toBe(false);
    expect((radioButtons[1].element as HTMLInputElement).checked).toBe(true);
  });

  it("emits update:modelValue when data.keepgoing changes", async () => {
    const wrapper = await createWrapper({ modelValue: { keepgoing: false } });
    const radioButtons = wrapper.findAll("input[type='radio']");

    await radioButtons[1].setValue();
    expect(wrapper.emitted()["update:modelValue"][0][0]).toEqual({
      keepgoing: true,
    });

    await radioButtons[0].setValue();
    expect(wrapper.emitted()["update:modelValue"][1][0]).toEqual({
      keepgoing: false,
    });
  });
});
