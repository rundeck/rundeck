import { PluginConfig } from "../../../../../library/interfaces/PluginConfig";

/**
 * Input config for script steps
 */
export interface ScriptArgsData {
  args?: string;
  scriptInterpreter?: string;
  fileExtension?: string;
  interpreterArgsQuoted?: boolean;
}

/**
 * Input config for script steps with inline script
 */
export interface ScriptInlineData extends ScriptArgsData {
  script?: string;
}

/**
 * Output Plugin common fields for script plugin types
 */
export interface ScriptGeneralPluginConfig {
  argString?: string;
  scriptInterpreter?: string;
  interpreterArgsQuoted?: boolean;
  fileExtension?: string;
}
/**
 * Ouput Plugin config for script-inline plugin type
 */
export interface ScriptInlinePluginConfig extends ScriptGeneralPluginConfig {
  adhocLocalString: string;
}

/**
 * Input config for script steps with script file
 */
export interface ScriptFileData extends ScriptArgsData {
  scriptfile?: string;
  expandTokenInScriptFile?: boolean;
}
/**
 * Ouput Plugin config for script-inline plugin type
 */
export interface ScriptFilePluginConfig extends ScriptGeneralPluginConfig {
  adhocFilepath: string;
  expandTokenInScriptFile?: boolean;
}
export interface CommandExecData {
  exec?: string;
}
/**
 * Ouput Plugin config for exec-command plugin type
 */
export interface CommandExecPluginConfig {
  adhocRemoteString: string;
}
export interface JobRefDefinition {
  group: string;

  name: string;
  project?: string;
  uuid?: string;
  args?: string;
  failOnDisable?: boolean;
  childNodes?: boolean;
  importOptions?: boolean;
  ignoreNotifications?: boolean;
  nodefilters?: {
    filter: string;
    dispatch?: {
      threadcount?: number;
      keepgoing?: boolean;
      rankAttribute?: string;
      rankOrder?: string;
      nodeIntersect?: boolean;
    };
  };
}
export interface JobRefData {
  jobref?: JobRefDefinition;
  nodeStep?: boolean;
}
export interface PluginStepData {
  type?: string;
  configuration?: any;
  nodeStep?: boolean;
}
export interface CommandData {
  description?: string;
  plugins?: { [key: string]: any };
}

export interface BasicData {
  keepgoing: boolean;
}

export interface StrategyData {
  strategy?: string;
  strategyConfig?: any;
}

export interface GlobalLogFiltersData {
  filters?: PluginConfig[];
}

export type StepData = CommandData &
  JobRefData &
  PluginStepData &
  ScriptFileData &
  ScriptInlineData &
  CommandExecData;
export interface StepsData {
  commands: StepData[];
}
export interface CommandEditData extends PluginConfig {
  nodeStep: boolean;
  description?: string;
  jobref?: JobRefDefinition;
  //simply to provide a unique id for each step on client side
  id: string;
  filters: PluginConfig[];
}
export type EditStepData = CommandEditData & JobRefData;
export interface StepsEditData {
  commands: EditStepData[];
}

export interface WorkflowData
  extends BasicData,
    StrategyData,
    GlobalLogFiltersData,
    StepsData {}

export function createBasicData({ keepgoing }): BasicData {
  return { keepgoing: !!keepgoing };
}

export function createStrategyData(
  { strategy },
  evalFunc: (val: string) => string = undefined,
): StrategyData {
  return { strategy: evalFunc ? evalFunc(strategy) : strategy };
}
export function createLogFiltersData({ logFilters }): GlobalLogFiltersData {
  return { filters: logFilters || [] };
}
export function createStepsData({ commands }): StepsData {
  return { commands: commands || [] };
}
