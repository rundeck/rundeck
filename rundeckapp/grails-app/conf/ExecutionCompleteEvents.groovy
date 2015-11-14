import rundeck.services.events.ExecutionCompleteEvent

events = {
    executionComplete filter: ExecutionCompleteEvent, fork: true
}