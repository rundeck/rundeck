package rundeck.services.execution

import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.jobs.JobReference
import rundeck.services.JobReferenceImpl


class ExecutionReferenceImpl implements ExecutionReference {
    String project
    String options
    String filter
    String id
    JobReference job
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
