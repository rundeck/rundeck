import { PluginConfig } from "../../../../../library/interfaces/PluginConfig";
import {
  ErrorHandlerDefinition,
  JobRefDefinition,
  StepPluginConfig,
} from "../../../../../library/types/jobs/WorkflowTypes";

export {
  CommandEditData,
  CommandExecPluginConfig,
  EditStepData,
  ErrorHandlerDefinition,
  JobRefDefinition,
  ScriptFilePluginConfig,
  ScriptGeneralPluginConfig,
  ScriptInlinePluginConfig,
  StepPluginConfig,
  StepPluginConfigExtension,
  StepsEditData,
} from "../../../../../library/types/jobs/WorkflowTypes";

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
 * Input config for script steps with script file
 */
export interface ScriptFileData extends ScriptArgsData {
  scriptfile?: string;
  scripturl?: string;
  expandTokenInScriptFile?: boolean;
}

export interface CommandExecData {
  exec?: string;
}
export interface JobRefData {
  description?: string;
  keepgoingOnSuccess?: boolean;
  jobref?: JobRefDefinition;
}
export interface PluginStepData {
  type?: string;
  configuration?: StepPluginConfig;
  nodeStep?: boolean;
}
export interface CommandData {
  description?: string;
  plugins?: Record<string, unknown>;
}
export interface BasicData {
  keepgoing: boolean;
}

export interface WorkflowStrategyPluginConfig {
  WorkflowStrategy?: Record<string, Record<string, unknown>>;
}

export interface WorkflowLogFilterConfig {
  LogFilter?: PluginConfig[];
}

export interface WorkflowPluginConfig {
  pluginConfig?: WorkflowStrategyPluginConfig &
    WorkflowLogFilterConfig &
    Record<string, unknown>;
}

export interface StrategyConfig {
  strategy?: string;
}

export interface ErrorHandlerData {
  errorhandler?: ErrorHandlerDefinition;
}
export interface StepErrorHandlerData {
  errorhandler?: StepData & {
    keepgoingOnSuccess?: boolean;
  };
}

export interface GlobalLogFiltersData {
  filters?: PluginConfig[];
  LogFilter?: PluginConfig[];
}

export type StepData = CommandData &
  JobRefData &
  PluginStepData &
  ScriptFileData &
  ScriptInlineData &
  CommandExecData &
  StepErrorHandlerData;

export interface StepsData {
  commands: StepData[];
}

export interface WorkflowData
  extends
    Partial<BasicData>,
    StrategyConfig,
    WorkflowPluginConfig,
    Partial<StepsData> {}

export function createBasicData({ keepgoing }: Partial<BasicData>): BasicData {
  return { keepgoing: !!keepgoing };
}

export function createStrategyData(workflowData: WorkflowData): PluginConfig {
  const type = workflowData.strategy || "";
  const strategyObj = workflowData.pluginConfig?.WorkflowStrategy;
  const config = strategyObj && type ? strategyObj[type] : {};
  return { type, config };
}

export function exportPluginData(
  strategy: PluginConfig,
  logFilters: GlobalLogFiltersData,
): WorkflowPluginConfig & StrategyConfig {
  const obj: {
    pluginConfig: {
      WorkflowStrategy: Record<string, Record<string, unknown>>;
      LogFilter?: PluginConfig[];
    };
    strategy: string;
  } = {
    pluginConfig: {
      WorkflowStrategy: { [strategy.type]: strategy.config },
    },
    strategy: strategy.type,
  };
  if (logFilters && logFilters.LogFilter && logFilters.LogFilter.length > 0) {
    obj.pluginConfig.LogFilter = logFilters.LogFilter;
  }
  return obj;
}
export function createLogFiltersData({
  pluginConfig,
}: WorkflowPluginConfig): GlobalLogFiltersData {
  return { LogFilter: pluginConfig?.LogFilter || [] };
}

export function createStepsData({ commands }: Partial<StepsData>): StepsData {
  return { commands: commands || [] };
}
