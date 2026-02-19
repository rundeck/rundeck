import type { ConditionSet } from "../../types/conditionalStepTypes";

export const mockSingleConditionSet: ConditionSet[] = [
  {
    id: "set-1",
    conditions: [
      { id: "cond-1", field: "os-name", operator: "equals", value: "Linux" },
    ],
  },
];

export const mockTwoConditionSets: ConditionSet[] = [
  {
    id: "set-1",
    conditions: [
      { id: "cond-1", field: "os-name", operator: "equals", value: "Linux" },
    ],
  },
  {
    id: "set-2",
    conditions: [
      { id: "cond-2", field: "region", operator: "contains", value: "us-east" },
    ],
  },
];

export const mockSetWithTwoConditions: ConditionSet[] = [
  {
    id: "set-1",
    conditions: [
      { id: "cond-1", field: "os-name", operator: "equals", value: "Linux" },
      { id: "cond-2", field: "region", operator: "contains", value: "us-east" },
    ],
  },
];

/** A condition set at the MAX_CONDITIONS_PER_SET limit (5 conditions) */
export const mockSetAtMaxConditions: ConditionSet[] = [
  {
    id: "set-1",
    conditions: [
      { id: "cond-1", field: "f1", operator: "equals", value: "v1" },
      { id: "cond-2", field: "f2", operator: "equals", value: "v2" },
      { id: "cond-3", field: "f3", operator: "equals", value: "v3" },
      { id: "cond-4", field: "f4", operator: "equals", value: "v4" },
      { id: "cond-5", field: "f5", operator: "equals", value: "v5" },
    ],
  },
];

/** Five condition sets — at the MAX_CONDITION_SETS limit */
export const mockFiveConditionSets: ConditionSet[] = [
  { id: "set-1", conditions: [{ id: "c1", field: "f1", operator: "equals", value: "v1" }] },
  { id: "set-2", conditions: [{ id: "c2", field: "f2", operator: "equals", value: "v2" }] },
  { id: "set-3", conditions: [{ id: "c3", field: "f3", operator: "equals", value: "v3" }] },
  { id: "set-4", conditions: [{ id: "c4", field: "f4", operator: "equals", value: "v4" }] },
  { id: "set-5", conditions: [{ id: "c5", field: "f5", operator: "equals", value: "v5" }] },
];
