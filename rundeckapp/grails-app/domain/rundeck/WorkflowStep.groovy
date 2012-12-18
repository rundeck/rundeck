package rundeck

class WorkflowStep {
    WorkflowStep errorHandler
    Boolean keepgoingOnSuccess
    static belongsTo = [Workflow, WorkflowStep]
    static constraints = {
        errorHandler(nullable: true)
        keepgoingOnSuccess(nullable: true)
    }

    public String summarize() {
        return this.toString()
    }
}
