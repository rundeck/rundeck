import type { ContextVariable } from "../../../stores/contextVariables";

export interface TabConfig {
  label: string;
  filter: (suggestion: ContextVariable) => boolean;
  getCount: (suggestions: ContextVariable[]) => number;
}
