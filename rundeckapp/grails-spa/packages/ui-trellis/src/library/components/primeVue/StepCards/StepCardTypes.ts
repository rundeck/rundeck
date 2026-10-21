import type { JobRefDefinition } from "../../../../app/components/job/workflow/types/workflowTypes";
import type { PluginConfig } from "../../../interfaces/PluginConfig";

export interface StepCardConfig extends PluginConfig {
  id?: string;
  jobref?: JobRefDefinition;
}

export interface StepCardHandler extends PluginConfig {
  jobref?: JobRefDefinition;
  title?: string;
}
