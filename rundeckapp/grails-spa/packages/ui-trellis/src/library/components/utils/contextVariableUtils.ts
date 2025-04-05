import {
  contextVariables,
  ContextVariable,
} from "../../stores/context_variables";

type WorkflowStepTypes = "WorkflowNodeStep" | "WorkflowStep" | "Notification";

export const getContextVariables = (
  stepType: WorkflowStepTypes,
): Array<ContextVariable> => {
  if (stepType === "WorkflowNodeStep") return getNodeContextVariables();
  if (stepType === "WorkflowStep") return getWorkflowContextVariables();
  if (stepType === "Notification") return getNotificationContextVariables();
  return [];
};

export const getWorkflowContextVariables = (): Array<ContextVariable> => {
  const vars = getContextVariablesByTypes(["job"]);
  return delimitContextVariables(vars);
};

const getNotificationContextVariables = (): Array<ContextVariable> => {
  const vars = getContextVariablesByTypes(["job", "execution"]);
  return delimitContextVariables(vars);
};

export const getScriptContextVariables = (
  stepType: WorkflowStepTypes,
): Array<ContextVariable> => {
  if (stepType === "WorkflowNodeStep") return getScriptNodeContextVariables();
  if (stepType === "WorkflowStep") return getScriptWorkflowContextVariables();
  return [];
};

export const getScriptNodeContextVariables = (): Array<ContextVariable> => {
  const vars = getContextVariablesByTypes(["job", "node"]);
  const thing = [
    delimitContextVariables(vars),
    delimitContextVariables(vars, "@"),
    transformVariablesToEnvironmentVariables(vars),
    transformVariablesToEnvironmentVariables(vars, "@"),
  ].flat();
  return thing;
};

export const getScriptWorkflowContextVariables = (): Array<ContextVariable> => {
  const vars = getContextVariablesByTypes(["job"]);
  const thing = [
    delimitContextVariables(vars),
    delimitContextVariables(vars, "@"),
    transformVariablesToEnvironmentVariables(vars),
    transformVariablesToEnvironmentVariables(vars, "@"),
  ].flat();
  return thing;
};

export const getNodeContextVariables = (): Array<ContextVariable> => {
  const vars = getContextVariablesByTypes(["job", "node"]);
  return delimitContextVariables(vars);
};

const getContextVariablesByTypes = (
  types: ContextVariable["type"][],
): Array<ContextVariable> => {
  return types.flatMap((type) => contextVariables[type]);
};

const transformVariablesToEnvironmentVariables = (
  contextVariables: Array<ContextVariable>,
  delimiter: "$" | "@" = "$",
): Array<ContextVariable> => {
  return contextVariables.map(({ name, type, ...rest }) => ({
    name:
      delimiter === "$"
        ? `$RD_${type}_${name}`.toUpperCase()
        : `@RD_${type}_${name}@`.toUpperCase(),
    type,
    ...rest,
  }));
};

const delimitContextVariables = (
  contextVariables: Array<ContextVariable>,
  delimiter: "$" | "@" = "$",
): Array<ContextVariable> => {
  return contextVariables.map(({ name, type, ...rest }) => ({
    name: delimiter === "$" ? `$\{${type}.${name}}` : `@${type}.${name}@`,
    type,
    ...rest,
  }));
};
