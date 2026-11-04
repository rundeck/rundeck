import { PluginConfig } from "../../interfaces/PluginConfig";

export interface ScriptGeneralPluginConfig {
  argString?: string;
  scriptInterpreter?: string;
  interpreterArgsQuoted?: boolean;
  fileExtension?: string;
}

export interface ScriptInlinePluginConfig extends ScriptGeneralPluginConfig {
  adhocLocalString: string;
}

export interface ScriptFilePluginConfig extends ScriptGeneralPluginConfig {
  adhocFilepath: string;
  expandTokenInScriptFile?: boolean;
}

export interface CommandExecPluginConfig {
  adhocRemoteString: string;
}

/**
 * Extension point for bundled consumers that add workflow-step config fields.
 */
export interface StepPluginConfigExtension {
  readonly __stepPluginConfigExtension?: never;
}

export type StepPluginConfig = (
  | ScriptInlinePluginConfig
  | ScriptFilePluginConfig
  | CommandExecPluginConfig
  | Record<string, unknown>
) &
  StepPluginConfigExtension;

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
  nodeStep?: boolean;
  useName?: boolean;
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

export interface ErrorHandlerDefinition {
  config: StepPluginConfig;
  keepgoingOnSuccess?: boolean;
  nodeStep: boolean;
  jobref?: JobRefDefinition;
  type: string;
  id: string;
}

export interface CommandEditData extends PluginConfig<StepPluginConfig> {
  nodeStep: boolean;
  description?: string;
  jobref?: JobRefDefinition;
  errorhandler?: ErrorHandlerDefinition;
  id: string;
  filters?: PluginConfig[];
}

export type EditStepData = CommandEditData;

export interface StepsEditData {
  commands: EditStepData[];
}
