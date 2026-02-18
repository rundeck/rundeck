import {
  CommandEditData,
  CommandExecPluginConfig,
  EditStepData,
  ErrorHandlerDefinition,
  ScriptFilePluginConfig,
  ScriptInlinePluginConfig,
  StepData,
  StepsData,
  StepsEditData,
  BackendCondition,
} from "./workflowTypes";
import type { ConditionSet } from "./conditionalStepTypes";

/**
 * Convert input data to edit data format
 * @param stepsData
 */
export function commandsToEditData(stepsData: StepsData): StepsEditData {
  return {
    commands: (stepsData.commands || []).map((cmd) => commandToEditConfig(cmd)),
  };
}

/**
 * Convert edit data to export data format
 * @param editData
 */
export function editCommandsToStepsData(editData: StepsEditData): StepsData {
  return {
    commands: editData.commands.map((cmd) => editToCommandConfig(cmd)),
  };
}

export function commandToEditConfig(cmd: StepData): CommandEditData {
  let editData = {
    description: cmd.description,
    id: mkid(),
    filters: cmd.plugins?.LogFilter || [],
  } as CommandEditData;
  // Check for conditional logic step (identified by conditionSet and subSteps)
  if (cmd.conditionSet !== undefined && cmd.subSteps !== undefined) {
    editData.type = "conditional.logic";
    editData.nodeStep = cmd.nodeStep ?? true;
    editData.description = cmd.name; // Backend 'name' → Frontend 'description'
    editData.config = {
      conditionSet: transformConditionSetToUI(cmd.conditionSet),
      subSteps: (cmd.subSteps || []).map((s: StepData) => commandToEditConfig(s)),
    };
  } else if (cmd.type) {
    editData.type = cmd.type;
    editData.config = cmd.configuration || {};
    editData.nodeStep = cmd.nodeStep;
  } else {
    if (cmd.jobref) {
      editData.type = "job.reference";
      editData.nodeStep = cmd.nodeStep ?? cmd.jobref?.nodeStep ?? false;
      editData.jobref = cmd.jobref;
    } else if (cmd.script) {
      editData.nodeStep = true;
      editData.type = "script-inline";
      editData.config = {
        adhocLocalString: cmd.script,
        argString: cmd.args,
        scriptInterpreter: cmd.scriptInterpreter,
        interpreterArgsQuoted: cmd.interpreterArgsQuoted,
        fileExtension: cmd.fileExtension,
      } as ScriptInlinePluginConfig;
    } else if (cmd.scriptfile) {
      editData.nodeStep = true;
      editData.type = "script-file-url";
      editData.config = {
        adhocFilepath: cmd.scriptfile,
        expandTokenInScriptFile: cmd.expandTokenInScriptFile,
        argString: cmd.args,
        scriptInterpreter: cmd.scriptInterpreter,
        interpreterArgsQuoted: cmd.interpreterArgsQuoted,
        fileExtension: cmd.fileExtension,
      } as ScriptFilePluginConfig;
    } else if (cmd.exec) {
      editData.nodeStep = true;
      editData.type = "exec-command";
      editData.config = {
        adhocRemoteString: cmd.exec,
      } as CommandExecPluginConfig;
    }
  }
  if(cmd.errorhandler) {
    editData.errorhandler = {
      ...commandToEditConfig(cmd.errorhandler),
      keepgoingOnSuccess: cmd.errorhandler.keepgoingOnSuccess
    }
  }
  return editData;
}
export function mkid() {
  return Math.random().toString(36).substring(7);
}

/**
 * Map UI operator to backend operator
 */
function mapOperatorToBackend(uiOperator: string): string {
  const operatorMap: Record<string, string> = {
    equals: "==",
    notEquals: "!=",
    contains: "in",
    notContains: "!~",
    regex: "~",
    matches: "=~"
  };
  return operatorMap[uiOperator] || uiOperator;
}

/**
 * Map backend operator to UI operator
 */
function mapOperatorToUI(backendOperator: string): string {
  const operatorMap: Record<string, string> = {
    "==": "equals",
    "!=": "notEquals",
    "in": "contains",
    "!~": "notContains",
    "~": "regex",
    "=~": "matches"
  };
  return operatorMap[backendOperator] || backendOperator;
}

/**
 * Transform UI conditionSet to backend format
 * UI: [{ id, conditions: [{ id, field, operator, value }] }]
 * Backend: [[{ key, operator, value }]]
 */
function transformConditionSetToBackend(
  conditionSet: ConditionSet[],
): BackendCondition[][] {
  return conditionSet.map((set) =>
    set.conditions.map((cond) => ({
      key: cond.field || "",
      operator: mapOperatorToBackend(cond.operator),
      value: cond.value,
    })),
  );
}

/**
 * Transform backend conditionSet to UI format
 * Backend: [[{ key, operator, value }]]
 * UI: [{ id, conditions: [{ id, field, operator, value }] }]
 */
function transformConditionSetToUI(
  backendConditionSet: BackendCondition[][],
): ConditionSet[] {
  return backendConditionSet.map((backendSet) => ({
    id: crypto.randomUUID(),
    conditions: backendSet.map((backendCond) => ({
      id: crypto.randomUUID(),
      field: backendCond.key,
      operator: mapOperatorToUI(backendCond.operator),
      value: backendCond.value,
    })),
  }));
}
export function editToCommandConfig(plugin: EditStepData): StepData {
  let data = {
    description: plugin.description,
    nodeStep: plugin.nodeStep,
    jobref: plugin.jobref
  } as StepData;
  if (plugin.filters && plugin.filters.length > 0) {
    data.plugins = {
      LogFilter: plugin.filters,
    };
  }
  if (plugin.type === "script-inline") {
    let scriptInline = plugin.config as ScriptInlinePluginConfig;
    data.script = scriptInline.adhocLocalString;
    data.args = scriptInline.argString;
    data.scriptInterpreter = scriptInline.scriptInterpreter;
    data.interpreterArgsQuoted = scriptInline.interpreterArgsQuoted;
    data.fileExtension = scriptInline.fileExtension;
  } else if (plugin.type === "script-file-url") {
    let scriptFile = plugin.config as ScriptFilePluginConfig;
    data.scriptfile = scriptFile.adhocFilepath;
    data.expandTokenInScriptFile = scriptFile.expandTokenInScriptFile;
    data.args = scriptFile.argString;
    data.scriptInterpreter = scriptFile.scriptInterpreter;
    data.interpreterArgsQuoted = scriptFile.interpreterArgsQuoted;
    data.fileExtension = scriptFile.fileExtension;
  } else if (plugin.type === "exec-command") {
    let commandExec = plugin.config as CommandExecPluginConfig;
    data.exec = commandExec.adhocRemoteString;
  } else if (plugin.type === "conditional.logic") {
    // Conditional logic: no 'type' field in backend format
    data.name = plugin.description; // Frontend 'description' → Backend 'name'
    data.conditionSet = transformConditionSetToBackend(plugin.config?.conditionSet || []);
    data.subSteps = (plugin.config?.subSteps || []).map((s: EditStepData) =>
      editToCommandConfig(s),
    );
    delete data.description; // Remove description from backend format
  } else if (plugin.type) {
    data.type = plugin.type;
    data.configuration = plugin.config;
  } else if (plugin.jobref) {
    data.jobref = plugin.jobref;
  }
  if(plugin.errorhandler) {
    data.errorhandler = {
      ...editToCommandConfig(plugin.errorhandler),
      keepgoingOnSuccess: plugin.errorhandler.keepgoingOnSuccess
    } as ErrorHandlerDefinition;
  }
  return data;
}
