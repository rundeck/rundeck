package rundeck.data.execution.reference

import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.jobs.JobReference

class ExecutionReferenceImpl implements ExecutionReference {
    String project
    String options
    String filter
    String id
    String uuid
    String retryOriginalId
    String retryPrevId
    String retryNextId
    JobReference job
    Date dateStarted
    Date dateCompleted
    String status
    String succeededNodeList
    String failedNodeList
    String targetNodes
    String adhocCommand
    Map metadata
    boolean scheduled
    String executionType

    @Override
    String toString() {
        return "ExecutionReference{" +
                "id='" + id + '\'' +
                "uuid='" + uuid + '\'' +
                ", status='" + status + '\'' +
                ", filter='" + filter + '\'' +
                ", options='" + options + '\'' +
                ", job='" + job + '\'' +
                '}';
    }
}
