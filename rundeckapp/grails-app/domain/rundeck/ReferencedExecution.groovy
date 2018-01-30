package rundeck

class ReferencedExecution {
    ScheduledExecution scheduledExecution
    String status
    Execution execution

    static belongsTo=[Execution]

    static constraints = {
        scheduledExecution(nullable:true)
        status(nullable:true)
        execution(nullable: false)

    }
}
