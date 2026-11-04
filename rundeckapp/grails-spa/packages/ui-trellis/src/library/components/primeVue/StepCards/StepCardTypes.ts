import type { JobRefDefinition } from "../../../types/jobs/WorkflowTypes";
import type { PluginConfig } from "../../../interfaces/PluginConfig";

export interface StepCardConfig extends PluginConfig {
  id?: string;
  jobref?: JobRefDefinition;
  nodeStep?: boolean;
}

export interface StepCardHandler extends PluginConfig {
  jobref?: JobRefDefinition;
  title?: string;
}
