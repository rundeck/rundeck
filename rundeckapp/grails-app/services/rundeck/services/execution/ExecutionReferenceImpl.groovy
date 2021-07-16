package rundeck.services.execution

import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.jobs.JobReference
import org.rundeck.core.executions.provenance.Provenance

class ExecutionReferenceImpl implements ExecutionReference {
    String project
    String options
    String filter
    String id
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
    Provenance provenance

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
