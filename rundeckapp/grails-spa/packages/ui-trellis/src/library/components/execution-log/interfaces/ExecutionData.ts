export default interface IWorkflowJson {
    workflow: Array<IWorkflowStep>
}

interface IWorkflowStep {
    jobref?: {}
    exec?: {}
    script?: {}
    scriptfile?: {}
    scripturl?: {}
    workflow?: Array<IWorkflowStep>
}