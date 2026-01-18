export interface Condition {
  id: string;
  field: string | null;
  operator: string;
  value: string;
}

export interface ConditionSet {
  id: string;
  conditions: Condition[];
}

export interface OperatorOption {
  label: string;
  value: string;
}

export interface FieldOption {
  label: string;
  value: string;
  isNote?: boolean;
}

export const MAX_CONDITIONS_PER_SET = 5;
export const MAX_CONDITION_SETS = 5;

export function createEmptyCondition(): Condition {
  return {
    id: crypto.randomUUID(),
    field: null,
    operator: "equals",
    value: "",
  };
}

export function createEmptyConditionSet(): ConditionSet {
  return {
    id: crypto.randomUUID(),
    conditions: [createEmptyCondition()],
  };
}
