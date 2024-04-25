package rundeck.events

class RdExecutionCompleteEvent {
    String state
    String executionUuid
    Map nodeStatus
    Map context
}
