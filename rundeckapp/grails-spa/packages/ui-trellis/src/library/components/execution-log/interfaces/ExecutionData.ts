export default interface IWorkflowJson {
  workflow: Array<IWorkflowStep>;
}

interface IWorkflowStep {
  jobref?: Record<string, unknown>;
  exec?: Record<string, unknown>;
  script?: Record<string, unknown>;
  scriptfile?: Record<string, unknown>;
  scripturl?: Record<string, unknown>;
  workflow?: Array<IWorkflowStep>;
}
