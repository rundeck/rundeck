import { mount } from "@vue/test-utils";
import PtStepper from "../PtStepper.vue";

interface MountOptions {
  props?: Record<string, any>;
  slots?: Record<string, string>;
}

const createWrapper = async (options: MountOptions = {}) => {
  const wrapper = mount(PtStepper, {
    props: {
      items: [{ label: "Step Plugin" }, { label: "Configure" }],
      ...options.props,
    },
    slots: options.slots,
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

const findByTestId = (wrapper: ReturnType<typeof mount>, testId: string) =>
  wrapper.find(`[data-testid="${testId}"]`);

describe("PtStepper", () => {
  describe("horizontal (default)", () => {
    it("renders the step labels in a horizontal stepper", async () => {
      const wrapper = await createWrapper();

      expect(findByTestId(wrapper, "pt-stepper-horizontal").exists()).toBe(
        true,
      );
      expect(findByTestId(wrapper, "pt-stepper-vertical").exists()).toBe(false);
      expect(findByTestId(wrapper, "pt-stepper-label-1").text()).toBe(
        "Step Plugin",
      );
      expect(findByTestId(wrapper, "pt-stepper-label-2").text()).toBe(
        "Configure",
      );
    });
  });

  describe("vertical", () => {
    it("renders the step labels in a vertical stepper", async () => {
      const wrapper = await createWrapper({
        props: { orientation: "vertical" },
      });

      expect(findByTestId(wrapper, "pt-stepper-vertical").exists()).toBe(true);
      expect(findByTestId(wrapper, "pt-stepper-horizontal").exists()).toBe(
        false,
      );
      expect(findByTestId(wrapper, "pt-stepper-label-1").text()).toBe(
        "Step Plugin",
      );
      expect(findByTestId(wrapper, "pt-stepper-label-2").text()).toBe(
        "Configure",
      );
    });

    it("renders the content slot under every step", async () => {
      const wrapper = await createWrapper({
        props: { orientation: "vertical" },
        slots: {
          content: `<template #content="{ item, index }">Body {{ index }} {{ item.label }}</template>`,
        },
      });

      expect(findByTestId(wrapper, "pt-stepper-content-1").text()).toBe(
        "Body 0 Step Plugin",
      );
      expect(findByTestId(wrapper, "pt-stepper-content-2").text()).toBe(
        "Body 1 Configure",
      );
    });

    it("renders an empty content area when no content slot is given", async () => {
      const wrapper = await createWrapper({
        props: { orientation: "vertical" },
      });

      expect(findByTestId(wrapper, "pt-stepper-content-1").text()).toBe("");
    });
  });
});
