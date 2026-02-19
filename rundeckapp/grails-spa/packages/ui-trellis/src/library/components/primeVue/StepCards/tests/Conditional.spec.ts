import { mount } from "@vue/test-utils";
import Conditional from "../Conditional.vue";

const createWrapper = async (props = {}) => {
  const wrapper = mount(Conditional, { props: { ...props } });
  await wrapper.vm.$nextTick();
  return wrapper;
};

const singleConditionSet = [
  {
    id: "cs-1",
    conditions: [
      { id: "c-1", field: "${job.execid}", operator: "equals", value: "123" },
    ],
  },
];

const multiConditionSet = [
  {
    id: "cs-1",
    conditions: [
      { id: "c-1", field: "${job.execid}", operator: "equals", value: "123" },
    ],
  },
  {
    id: "cs-2",
    conditions: [
      { id: "c-2", field: "${job.status}", operator: "contains", value: "success" },
    ],
  },
];

describe("Conditional", () => {
  describe("when there are no conditions", () => {
    it("shows nothing when conditionSet is empty", async () => {
      const wrapper = await createWrapper({ conditionSet: [] });
      expect(wrapper.find('[data-testid="conditional-if-statement"]').exists()).toBe(false);
    });
  });

  describe("displaying a condition to the user", () => {
    it("shows the condition field name so users know what variable is being checked", async () => {
      const wrapper = await createWrapper({ conditionSet: singleConditionSet });
      expect(wrapper.find('[data-testid="conditional-if-statement"]').text()).toContain(
        "${job.execid}",
      );
    });

    it("shows the condition value so users know what it is being compared to", async () => {
      const wrapper = await createWrapper({ conditionSet: singleConditionSet });
      expect(wrapper.find('[data-testid="conditional-if-statement"]').text()).toContain("123");
    });

    it("shows the operator label so users understand the comparison type", async () => {
      const wrapper = await createWrapper({ conditionSet: singleConditionSet });
      // $t is mocked globally to return the translation key as-is
      expect(wrapper.find('[data-testid="conditional-if-statement"]').text()).toContain(
        "Workflow.conditional.operator.equals",
      );
    });
  });

  describe("the 'do' section", () => {
    it("shows the 'do' section by default so the nested steps are visible", async () => {
      const wrapper = await createWrapper({
        conditionSet: singleConditionSet,
        headerOnly: false,
      });
      expect(wrapper.find('[data-testid="conditional-do-section"]').exists()).toBe(true);
    });

    it("hides the 'do' section when headerOnly is true so only the condition summary shows", async () => {
      const wrapper = await createWrapper({
        conditionSet: singleConditionSet,
        headerOnly: true,
      });
      expect(wrapper.find('[data-testid="conditional-do-section"]').exists()).toBe(false);
    });
  });

  describe("complex mode (multiple OR groups)", () => {
    it("shows the complex OR layout when there are multiple condition sets", async () => {
      const wrapper = await createWrapper({
        conditionSet: multiConditionSet,
        complex: true,
        headerOnly: false,
      });
      expect(wrapper.find('[data-testid="conditional-complex-section"]').exists()).toBe(true);
    });

    it("does not show the complex OR layout when complex is false even with multiple condition sets", async () => {
      const wrapper = await createWrapper({
        conditionSet: multiConditionSet,
        complex: false,
        headerOnly: false,
      });
      expect(wrapper.find('[data-testid="conditional-complex-section"]').exists()).toBe(false);
    });
  });
});
