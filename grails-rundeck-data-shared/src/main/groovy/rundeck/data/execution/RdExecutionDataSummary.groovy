package rundeck.data.execution

import org.rundeck.app.data.model.v1.execution.ExecutionDataSummary

class RdExecutionDataSummary implements ExecutionDataSummary {
    String uuid
    String jobUuid
    String project
    String status
    String executionType
    String executionState
    Date dateStarted
    Date dateCompleted
    String serverNodeUUID
}
