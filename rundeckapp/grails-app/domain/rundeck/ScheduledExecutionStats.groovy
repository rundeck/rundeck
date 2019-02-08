package rundeck

class ScheduledExecutionStats {

    Long totalTime = 0
    Long execCount = 0
    Long refExecCount = 0
    Long scheduledExecutionId = 0

    static belongsTo=[ScheduledExecution]

    static constraints = {
        scheduledExecutionId(unique: true)
    }
}
