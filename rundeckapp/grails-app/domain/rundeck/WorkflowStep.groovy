package rundeck

class WorkflowStep {
    WorkflowStep errorHandler
    Boolean keepgoingOnSuccess
    String description
    static belongsTo = [Workflow, WorkflowStep]
    static constraints = {
        errorHandler(nullable: true)
        keepgoingOnSuccess(nullable: true)
        description(nullable: true, maxSize: 1024)
    }

    public String summarize() {
        return this.toString()
    }
}
