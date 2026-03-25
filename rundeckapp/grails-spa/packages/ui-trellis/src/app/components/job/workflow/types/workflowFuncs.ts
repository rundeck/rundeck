import { cloneDeep } from "lodash";
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
} from "./workflowTypes";

/**
 * When saving a workflow step, plugin edit UIs may omit `filters` or emit `[]` while the step
 * still had log filters configured. Merge from the original step so filters are not dropped.
 */
export function mergePreservedLogFiltersIntoSaveData(
  saveData: Pick<EditStepData, "filters">,
  originalFilters: EditStepData["filters"] | undefined | null,
): void {
  if (saveData.filters == null) {
    saveData.filters =
      originalFilters != null ? cloneDeep(originalFilters) : [];
  } else if (
    Array.isArray(saveData.filters) &&
    saveData.filters.length === 0 &&
    Array.isArray(originalFilters) &&
    originalFilters.length > 0
  ) {
    saveData.filters = cloneDeep(originalFilters);
  }
}

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
  if (cmd.type) {
    editData.type = cmd.type;
    editData.config = cmd.configuration || {};
    editData.nodeStep = cmd.nodeStep ?? false;
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
    } else if (cmd.scriptfile != null || cmd.scripturl != null) {
      editData.nodeStep = true;
      editData.type = "script-file-url";
      editData.config = {
        adhocFilepath: cmd.scriptfile ?? cmd.scripturl,
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
    if (scriptFile.adhocFilepath != null) {
      const isUrl = /^(https?|file):.*$/i.test(scriptFile.adhocFilepath);
      if (isUrl) {
        data.scripturl = scriptFile.adhocFilepath;
      } else {
        data.scriptfile = scriptFile.adhocFilepath;
      }
    }
    data.expandTokenInScriptFile = scriptFile.expandTokenInScriptFile;
    data.args = scriptFile.argString;
    data.scriptInterpreter = scriptFile.scriptInterpreter;
    data.interpreterArgsQuoted = scriptFile.interpreterArgsQuoted;
    data.fileExtension = scriptFile.fileExtension;
  } else if (plugin.type === "exec-command") {
    let commandExec = plugin.config as CommandExecPluginConfig;
    data.exec = commandExec.adhocRemoteString;
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
