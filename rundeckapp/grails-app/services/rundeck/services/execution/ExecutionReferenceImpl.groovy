package rundeck.services.execution

import com.dtolabs.rundeck.core.execution.ExecutionReference
import rundeck.services.JobReferenceImpl


class ExecutionReferenceImpl implements ExecutionReference {
    String options
    String filter
    String id
    JobReferenceImpl job

    @Override
    String toString() {
        return "ExecutionReference{" +
                "id='" + id + '\'' +
                ", filter='" + filter + '\'' +
                ", options='" + options + '\'' +
                ", job='" + job + '\'' +
                '}';
    }
}
