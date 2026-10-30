import { mount } from "@vue/test-utils";
import PtCheckbox from "../PtCheckbox.vue";

interface MountOptions {
  modelValue?: boolean;
  inputId?: string;
  name?: string;
  disabled?: boolean;
  readonly?: boolean;
  invalid?: boolean;
  indeterminate?: boolean;
  ariaLabel?: string;
}

const createWrapper = (props: MountOptions = {}) =>
  mount(PtCheckbox, {
    props: {
      modelValue: false,
      ...props,
    },
  });

const findByTestId = (wrapper: ReturnType<typeof mount>, testId: string) =>
  wrapper.find(`[data-testid="${testId}"]`);

describe("PtCheckbox", () => {
  it("emits the checked value when a user toggles the checkbox", async () => {
    const wrapper = createWrapper();

    await findByTestId(wrapper, "pt-checkbox-input").setValue(true);

    expect(wrapper.emitted("update:modelValue")).toEqual([[true]]);
  });

  it("renders the checked state supplied by v-model", () => {
    const wrapper = createWrapper({ modelValue: true });

    expect(
      (findByTestId(wrapper, "pt-checkbox-input").element as HTMLInputElement)
        .checked,
    ).toBe(true);
  });

  it("forwards native input identity and accessible naming", () => {
    const wrapper = createWrapper({
      inputId: "node-api-01",
      name: "nodes",
      ariaLabel: "Select api-01",
    });
    const input = findByTestId(wrapper, "pt-checkbox-input");

    expect(input.attributes()).toMatchObject({
      id: "node-api-01",
      name: "nodes",
      "aria-label": "Select api-01",
    });
  });

  it("prevents interaction when disabled", async () => {
    const wrapper = createWrapper({ disabled: true });
    const input = findByTestId(wrapper, "pt-checkbox-input");

    expect((input.element as HTMLInputElement).disabled).toBe(true);
    await input.setValue(true);
    expect(wrapper.emitted("update:modelValue")).toBeUndefined();
  });

  it("marks the native control readonly", () => {
    const wrapper = createWrapper({ readonly: true });

    expect(
      findByTestId(wrapper, "pt-checkbox-input").attributes("readonly"),
    ).toBeDefined();
  });

  it("exposes invalid and indeterminate states to PrimeVue", () => {
    const wrapper = createWrapper({
      invalid: true,
      indeterminate: true,
    });

    expect(findByTestId(wrapper, "pt-checkbox").classes()).toContain(
      "p-invalid",
    );
    expect(
      (findByTestId(wrapper, "pt-checkbox-input").element as HTMLInputElement)
        .indeterminate,
    ).toBe(true);
  });
});
