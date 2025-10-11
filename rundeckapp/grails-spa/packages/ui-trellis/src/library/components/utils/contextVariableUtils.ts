import { ContextVariable } from "../../stores/contextVariables";
import { useJobStore } from "../../stores/JobsStore";

export type WorkflowStepType = "WorkflowNodeStep" | "WorkflowStep" | "Notification";
const workflowStepTypes = ["WorkflowNodeStep", "WorkflowStep", "Notification"] as const;

type DelimiterType = "$" | "@";
type FieldType = "input" | "script";

const stepTypeContextVarMap: Record<WorkflowStepType, ContextVariable["type"][]> = {
  WorkflowNodeStep: ["job", "node"],
  WorkflowStep: ["job"],
  Notification: ["job", "execution"],
};

export const isAutoCompleteField = (step: unknown): step is WorkflowStepType =>
  workflowStepTypes.includes(step as WorkflowStepType);

export const getContextVariables = (
  fieldType: FieldType,
  stepType?: WorkflowStepType,
  pluginType?: string,
): ContextVariable[] => {
  if (!stepType) {
    return [];
  }
  try {
    const variableTypes = stepTypeContextVarMap[stepType];
    const variablesByType = getVariablesByTypes(variableTypes);
    return transformVariables(fieldType, variablesByType, pluginType);
  } catch {
    console.error("couldn't get variables");
    return [];
  }
};

export const transformVariables = (
  fieldType: FieldType,
  variables: ContextVariable[],
  pluginType?: string,
): ContextVariable[] => {
  const include_at_symbol_vars =
    pluginType === "script-inline" && fieldType === "script";
  const include_env_vars = fieldType === "script";
  return formatVars(variables, include_at_symbol_vars, include_env_vars);
};

const getVariablesByTypes = (types: ContextVariable["type"][]): Array<ContextVariable> => {
  const contextVariables =
    (useJobStore().contextVariables as Record<string, ContextVariable[]>) || [];
  return [...types].flatMap((type) => contextVariables[type]);
};

const formatVars = (
  variables: ContextVariable[],
  include_at_symbol_vars: boolean,
  include_env_vars: boolean,
): ContextVariable[] => {
  return [
    ...formatWithDelimiters(variables),
    ...(include_at_symbol_vars ? formatWithDelimiters(variables, "@") : []),
    ...(include_env_vars ? formatAsEnvVars(variables) : []),
    ...(include_at_symbol_vars ? formatAsEnvVars(variables, "@") : []),
  ];
};

type VariableFormatter = (
  variables: ContextVariable[],
  delimiter?: DelimiterType,
) => ContextVariable[];

const formatAsEnvVars: VariableFormatter = (variables, delimiter = "$") => {
  return variables.map(({ name, type, ...rest }) => ({
    name:
      delimiter === "$"
        ? `$RD_${type}_${name}`.toUpperCase()
        : `@RD_${type}_${name}@`.toUpperCase(),
    type,
    ...rest,
  }));
};

const formatWithDelimiters: VariableFormatter = (variables, delimiter = "$") => {
  return variables.flatMap(({ name, type, ...rest }) => {
    const baseName = delimiter === "$" ? `\${${type}.${name}}` : `@${type}.${name}@`;

    const unquotedName =
      delimiter === "$" ? `\${unquoted${type}.${name}}` : `@unquoted${type}.${name}@`;

    return [
      { name: baseName, type, ...rest },
      ...(type === "option" ? [{ name: unquotedName, type, ...rest }] : []),
    ];
  });
};
