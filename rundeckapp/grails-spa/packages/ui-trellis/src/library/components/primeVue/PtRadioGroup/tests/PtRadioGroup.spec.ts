import { mount } from "@vue/test-utils";
import PtRadioGroup from "../PtRadioGroup.vue";

const OPTIONS = [
  { label: "All nodes", value: "all" },
  { label: "Specific nodes", value: "specific" },
];

const createWrapper = (props = {}) =>
  mount(PtRadioGroup, {
    props: {
      modelValue: "all",
      options: OPTIONS,
      legend: "Select Target Nodes:",
      ...props,
    },
  });

const findByTestId = (wrapper: ReturnType<typeof mount>, testId: string) =>
  wrapper.find(`[data-testid="${testId}"]`);

const findRadio = (wrapper: ReturnType<typeof mount>, index: number) =>
  findByTestId(wrapper, `pt-radio-group-radio-${index}`).find("input");

describe("PtRadioGroup", () => {
  describe("rendering", () => {
    it("renders a label for each option", () => {
      const wrapper = createWrapper();

      const labels = wrapper
        .findAll('[data-testid="pt-radio-group-label"]')
        .map((label) => label.text());
      expect(labels).toEqual(["All nodes", "Specific nodes"]);
    });

    it("checks only the radio matching the model value", () => {
      const wrapper = createWrapper({ modelValue: "specific" });

      expect(findRadio(wrapper, 0).element.checked).toBe(false);
      expect(findRadio(wrapper, 1).element.checked).toBe(true);
    });

    it("supports plain string options", () => {
      const wrapper = createWrapper({
        modelValue: "b",
        options: ["a", "b"],
      });

      expect(findRadio(wrapper, 1).element.checked).toBe(true);
      expect(wrapper.text()).toContain("a");
    });

    it("renders every option when values stringify to the same text", () => {
      const wrapper = createWrapper({
        modelValue: "true",
        options: [
          { label: "Boolean", value: true },
          { label: "String", value: "true" },
        ],
      });

      const labels = wrapper
        .findAll('[data-testid="pt-radio-group-label"]')
        .map((label) => label.text());
      expect(labels).toEqual(["Boolean", "String"]);
      expect(findRadio(wrapper, 1).element.checked).toBe(true);
    });

    it("supports boolean option values", () => {
      const wrapper = createWrapper({
        modelValue: false,
        options: [
          { label: "Continue", value: true },
          { label: "Stop", value: false },
        ],
      });

      expect(findRadio(wrapper, 0).element.checked).toBe(false);
      expect(findRadio(wrapper, 1).element.checked).toBe(true);
    });

    it("honours custom optionLabel and optionValue", () => {
      const wrapper = createWrapper({
        modelValue: 2,
        options: [
          { id: 1, name: "One" },
          { id: 2, name: "Two" },
        ],
        optionLabel: "name",
        optionValue: "id",
      });

      expect(wrapper.text()).toContain("Two");
      expect(findRadio(wrapper, 1).element.checked).toBe(true);
    });
  });

  describe("accessibility", () => {
    it("names the group by its legend", () => {
      const wrapper = createWrapper({ legend: "Select Target Nodes:" });

      const legend = findByTestId(wrapper, "pt-radio-group-legend");
      expect(legend.text()).toBe("Select Target Nodes:");
      expect(
        findByTestId(wrapper, "pt-radio-group").attributes(),
      ).toMatchObject({
        role: "radiogroup",
        "aria-labelledby": legend.attributes("id"),
      });
    });

    it("keeps a hidden legend as the group's accessible name", () => {
      const wrapper = createWrapper({ hideLegend: true });

      const legend = findByTestId(wrapper, "pt-radio-group-legend");
      expect(legend.classes()).toContain("pt-radio-group__legend--hidden");
      expect(
        findByTestId(wrapper, "pt-radio-group").attributes("aria-labelledby"),
      ).toBe(legend.attributes("id"));
    });

    it("shows the legend by default", () => {
      const wrapper = createWrapper();

      expect(
        findByTestId(wrapper, "pt-radio-group-legend").classes(),
      ).not.toContain("pt-radio-group__legend--hidden");
    });

    it("links each label to its radio", () => {
      const wrapper = createWrapper();

      const label = wrapper.findAll('[data-testid="pt-radio-group-label"]')[1];
      expect(label.attributes("for")).toBe(
        findRadio(wrapper, 1).attributes("id"),
      );
    });

    it("shares one name across the radios", () => {
      const wrapper = createWrapper({ name: "target" });

      expect(findRadio(wrapper, 0).attributes("name")).toBe("target");
      expect(findRadio(wrapper, 1).attributes("name")).toBe("target");
    });
  });

  describe("selection", () => {
    it("emits update:modelValue and change with the chosen value", async () => {
      const wrapper = createWrapper();

      await findRadio(wrapper, 1).setValue(true);

      expect(wrapper.emitted("update:modelValue")![0]).toEqual(["specific"]);
      expect(wrapper.emitted("change")![0]).toEqual(["specific"]);
    });
  });

  describe("disabled", () => {
    it("disables every radio when the group is disabled", () => {
      const wrapper = createWrapper({ disabled: true });

      expect(findRadio(wrapper, 0).element.disabled).toBe(true);
      expect(findRadio(wrapper, 1).element.disabled).toBe(true);
    });

    it("disables only the options flagged disabled", () => {
      const wrapper = createWrapper({
        options: [
          { label: "A", value: "a" },
          { label: "B", value: "b", disabled: true },
        ],
      });

      expect(findRadio(wrapper, 0).element.disabled).toBe(false);
      expect(findRadio(wrapper, 1).element.disabled).toBe(true);
    });
  });
});
