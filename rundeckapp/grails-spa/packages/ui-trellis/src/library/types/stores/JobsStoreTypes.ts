import { JobDefinition } from "../jobs/JobDefinition";

export type JobStore = {
  activeId?: string;
  jobs: {
    [jobId: string]: JobDefinition;
  };
};
