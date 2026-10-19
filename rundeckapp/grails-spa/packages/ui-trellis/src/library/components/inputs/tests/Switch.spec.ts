import { mount } from "@vue/test-utils";
import RdSwitch from "../Switch.vue";

const createWrapper = async (props = {}) => {
  const wrapper = mount(RdSwitch, {
    props: {
      modelValue: false,
      ...props,
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("RdSwitch", () => {
  it.each([
    [false, true],
    [true, false],
  ])(
    "emits update:modelValue with %s toggled to %s when clicked",
    async (current, expected) => {
      const wrapper = await createWrapper({ modelValue: current });

      await wrapper.find('[data-testid="switch-toggle"]').trigger("click");

      expect(wrapper.emitted("update:modelValue")).toHaveLength(1);
      expect(wrapper.emitted("update:modelValue")?.[0][0]).toBe(expected);
    },
  );

  it("emits update:modelValue when space is pressed", async () => {
    const wrapper = await createWrapper();

    await wrapper
      .find('[data-testid="switch-toggle"]')
      .trigger("keypress", { key: " " });

    expect(wrapper.emitted("update:modelValue")?.[0][0]).toBe(true);
  });

  it("emits a boolean when the native checkbox changes", async () => {
    const wrapper = await createWrapper();

    await wrapper.find('[data-testid="switch-input"]').setValue(true);

    expect(wrapper.emitted("update:modelValue")?.[0][0]).toBe(true);
  });

  it("reflects the modelValue prop in the checkbox and aria state", async () => {
    const wrapper = await createWrapper();
    const toggle = wrapper.find('[data-testid="switch-toggle"]');
    const input = wrapper.find('[data-testid="switch-input"]');

    expect((input.element as HTMLInputElement).checked).toBe(false);
    expect(toggle.attributes("aria-checked")).toBe("false");

    await wrapper.setProps({ modelValue: true });
    await wrapper.vm.$nextTick();

    expect((input.element as HTMLInputElement).checked).toBe(true);
    expect(toggle.attributes("aria-checked")).toBe("true");
    expect(toggle.classes()).toContain("switch--checked");
  });

  it("does not emit on its own when the parent changes modelValue", async () => {
    const wrapper = await createWrapper();

    await wrapper.setProps({ modelValue: true });
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("update:modelValue")).toBeUndefined();
  });
});
