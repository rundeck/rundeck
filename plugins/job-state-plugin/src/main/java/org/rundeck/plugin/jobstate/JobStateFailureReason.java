package org.rundeck.plugin.jobstate;

import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;

public enum JobStateFailureReason implements FailureReason {
    /**
     * State not match and failed marked true
     */
    ExecutionStateNotMatch
}
