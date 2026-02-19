import {
  createEmptyCondition,
  createEmptyConditionSet,
  MAX_CONDITIONS_PER_SET,
  MAX_CONDITION_SETS,
} from "../conditionalStepTypes";

describe("conditionalStepTypes", () => {
  describe("createEmptyCondition", () => {
    it("returns a condition with a generated id", () => {
      const condition = createEmptyCondition();

      expect(condition.id).toBeDefined();
      expect(typeof condition.id).toBe("string");
      expect(condition.id.length).toBeGreaterThan(0);
    });

    it("returns a condition with null field", () => {
      const condition = createEmptyCondition();

      expect(condition.field).toBeNull();
    });

    it("returns a condition with default operator equals", () => {
      const condition = createEmptyCondition();

      expect(condition.operator).toBe("equals");
    });

    it("returns a condition with empty value", () => {
      const condition = createEmptyCondition();

      expect(condition.value).toBe("");
    });

    it("returns a new object on each call", () => {
      const c1 = createEmptyCondition();
      const c2 = createEmptyCondition();

      expect(c1).not.toBe(c2);
    });

    it("generates a unique id on each call", () => {
      const c1 = createEmptyCondition();
      const c2 = createEmptyCondition();

      expect(c1.id).not.toBe(c2.id);
    });
  });

  describe("createEmptyConditionSet", () => {
    it("returns a condition set with a generated id", () => {
      const set = createEmptyConditionSet();

      expect(set.id).toBeDefined();
      expect(typeof set.id).toBe("string");
      expect(set.id.length).toBeGreaterThan(0);
    });

    it("returns a condition set with exactly one condition", () => {
      const set = createEmptyConditionSet();

      expect(set.conditions).toHaveLength(1);
    });

    it("initializes the single condition via createEmptyCondition", () => {
      const set = createEmptyConditionSet();
      const condition = set.conditions[0];

      expect(condition.field).toBeNull();
      expect(condition.operator).toBe("equals");
      expect(condition.value).toBe("");
      expect(typeof condition.id).toBe("string");
    });

    it("returns a new object on each call", () => {
      const s1 = createEmptyConditionSet();
      const s2 = createEmptyConditionSet();

      expect(s1).not.toBe(s2);
    });

    it("generates a unique id on each call", () => {
      const s1 = createEmptyConditionSet();
      const s2 = createEmptyConditionSet();

      expect(s1.id).not.toBe(s2.id);
    });
  });

  describe("constants", () => {
    it("MAX_CONDITIONS_PER_SET is 5", () => {
      expect(MAX_CONDITIONS_PER_SET).toBe(5);
    });

    it("MAX_CONDITION_SETS is 5", () => {
      expect(MAX_CONDITION_SETS).toBe(5);
    });
  });
});
