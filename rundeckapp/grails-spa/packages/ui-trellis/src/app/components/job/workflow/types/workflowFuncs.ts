import {
  CommandEditData,
  CommandExecPluginConfig,
  EditStepData,
  ScriptFilePluginConfig,
  ScriptInlinePluginConfig,
  StepData,
  StepsData,
  StepsEditData,
} from "./workflowTypes";

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
  let editData = { description: cmd.description } as CommandEditData;
  if (cmd.type) {
    editData.type = cmd.type;
    editData.config = cmd.configuration;
    editData.nodeStep = cmd.nodeStep;
  } else {
    if (cmd.jobref) {
      editData.jobref = cmd.jobref;
      editData.nodeStep = cmd.nodeStep;
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
  return editData;
}

export function editToCommandConfig(plugin: EditStepData): StepData {
  let data = {
    description: plugin.description,
    nodeStep: plugin.nodeStep,
  } as StepData;
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
  } else if (plugin.type) {
    data.type = plugin.type;
    data.configuration = plugin.config;
  } else if (plugin.jobref) {
    data.jobref = plugin.jobref;
  }
  return data;
}
