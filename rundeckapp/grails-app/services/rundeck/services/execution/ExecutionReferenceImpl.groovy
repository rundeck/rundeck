package rundeck.services.execution

import com.dtolabs.rundeck.core.execution.ExecutionReference
import rundeck.services.JobReferenceImpl


class ExecutionReferenceImpl implements ExecutionReference {
    String options
    String filter
    String id
    JobReferenceImpl job
    Date dateStarted
    Date dateCompleted
    String status
    String succeededNodeList
    String failedNodeList
    String targetNodes
    String adhocCommand

    @Override
    String toString() {
        return "ExecutionReference{" +
                "id='" + id + '\'' +
                ", status='" + status + '\'' +
                ", filter='" + filter + '\'' +
                ", options='" + options + '\'' +
                ", job='" + job + '\'' +
                '}';
    }
}
