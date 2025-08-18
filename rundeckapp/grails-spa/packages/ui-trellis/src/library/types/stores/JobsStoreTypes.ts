import { JobDefinition } from "../jobs/JobDefinition";
export interface ContextVariable {
  name: string;
  title: string;
  desc?: string;
}

export type JobStore = {
  activeId?: string;
  jobs: {
    [jobId: string]: JobDefinition;
  };
  contextVariables?: Record<string, ContextVariable[]>;
};
