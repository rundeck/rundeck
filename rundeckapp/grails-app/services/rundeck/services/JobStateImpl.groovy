package rundeck.services

import com.dtolabs.rundeck.core.dispatcher.ExecutionState
import com.dtolabs.rundeck.core.jobs.JobState

/**
 * Created by greg on 2/3/15.
 */
class JobStateImpl implements JobState {
    boolean running
    Set<String> runningExecutionIds
    ExecutionState previousExecutionState
    String previousExecutionStatusString
}
