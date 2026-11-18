import { mount, VueWrapper } from "@vue/test-utils";
import PtProgressBar from "../PtProgressBar.vue";

const createWrapper = (
  props: Record<string, unknown> = {},
  slots: Record<string, string> = {},
): VueWrapper => {
  return mount(PtProgressBar, {
    props,
    slots,
    global: {
      mocks: {
        $t: (key: string, args?: unknown[]) =>
          args ? `${key}:${args.join(",")}` : key,
      },
    },
  });
};

const progressRoot = (wrapper: VueWrapper) =>
  wrapper.find('[role="progressbar"]');

describe("PtProgressBar", () => {
  it("shows the percent label and sets aria-valuenow for a determinate value", () => {
    const wrapper = createWrapper({ value: 42 });

    expect(progressRoot(wrapper).attributes("aria-valuenow")).toBe("42");
    expect(progressRoot(wrapper).text()).toContain("42%");
  });

  it("clamps values above 100 to a full bar", () => {
    const wrapper = createWrapper({ value: 150 });

    expect(progressRoot(wrapper).attributes("aria-valuenow")).toBe("100");
    expect(progressRoot(wrapper).text()).toContain("100%");
  });

  it("clamps negative values to zero and hides the label", () => {
    const wrapper = createWrapper({ value: -5 });

    expect(progressRoot(wrapper).attributes("aria-valuenow")).toBe("0");
    expect(progressRoot(wrapper).text()).not.toContain("%");
  });

  it("hides the value label when showValue is false", () => {
    const wrapper = createWrapper({ value: 60, showValue: false });

    expect(progressRoot(wrapper).text()).not.toContain("60%");
  });

  it("renders default slot content in place of the percent label", () => {
    const wrapper = createWrapper(
      { value: 40 },
      { default: "<span>40 of 100</span>" },
    );

    expect(progressRoot(wrapper).text()).toContain("40 of 100");
    expect(progressRoot(wrapper).text()).not.toContain("40%");
  });

  it("renders no label or screen-reader text in indeterminate mode", () => {
    const wrapper = createWrapper({ mode: "indeterminate" });

    expect(progressRoot(wrapper).text()).toBe("");
    expect(wrapper.find('[data-testid="pt-progress-bar-sr"]').exists()).toBe(
      false,
    );
  });

  it("passes aria-label and aria-labelledby to the progressbar element", () => {
    const wrapper = createWrapper({
      value: 10,
      ariaLabel: "Export progress",
      ariaLabelledby: "export-heading",
    });

    expect(progressRoot(wrapper).attributes("aria-label")).toBe(
      "Export progress",
    );
    expect(progressRoot(wrapper).attributes("aria-labelledby")).toBe(
      "export-heading",
    );
  });

  it("renders the screen-reader percent complete text with the clamped value", () => {
    const wrapper = createWrapper({ value: 120 });

    expect(wrapper.find('[data-testid="pt-progress-bar-sr"]').text()).toBe(
      "progress.bar.percent.complete:100",
    );
  });
});
