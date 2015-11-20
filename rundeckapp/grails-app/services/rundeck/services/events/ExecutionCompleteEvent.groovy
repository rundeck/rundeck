package rundeck.services.events

import rundeck.Execution
import rundeck.ScheduledExecution

/**
 * Created by greg on 11/2/15.
 */
class ExecutionCompleteEvent {
    String state
    Execution execution
    ScheduledExecution job
    Map nodeStatus
    Map context


    @Override
    public String toString() {
        return "rundeck.services.events.ExecutionCompleteEvent{" +
                "state='" + state + '\'' +
                ", execution=" + execution +
                ", job=" + job +
                ", nodeStatus=" + nodeStatus +
                ", context=" + context +
                '}';
    }
}
