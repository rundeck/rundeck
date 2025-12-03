import { RootStore } from "./RootStore";
import { JobWorkflow } from "../utilities/JobWorkflow";
import { api } from "../services/api";

export class WorkflowStore {
  workflows: Map<string, JobWorkflow> = new Map();

  constructor(
    readonly root: RootStore,
  ) {}

  async get(jobId: string) {
    if (!this.workflows.has(jobId)) {
      const workflow = await this.fetch(jobId);
      this.workflows.set(jobId, workflow);
    }
    return this.workflows.get(jobId)!;
  }

  private async fetch(jobId: string) {
    const resp = await api.get(`job/${jobId}/workflow`);
    return new JobWorkflow(resp.data.workflow);
  }
}

class WorkflowStep {
  jobref?: WorkflowStepJobref;
  jobId?: string;
  description?: string;
  exec?: string;
  script?: string;
  scriptfile?: string;
  scripturl?: string;
  type?: string;
  nodeStep?: string;
  workflow?: WorkflowStep[];
}

class WorkflowStepJobref {
  name?: string;
  group?: string;
  uuid?: string;
  nodeStep?: string;
  importOptions?: boolean;
}
