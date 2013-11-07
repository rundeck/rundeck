package com.dtolabs.rundeck.core.execution.workflow.state;

/**
 * $INTERFACE is ... User: greg Date: 10/15/13 Time: 2:52 PM
 */
public enum ExecutionState {
    /**
     * Waiting to start running
     */
    WAITING,
    /**
     * Currently running
     */
    RUNNING,
    /**
     * Running error handler
     */
    RUNNING_HANDLER,
    /**
     * Finished running successfully
     */
    SUCCEEDED,
    /**
     * Finished with a failure
     */
    FAILED,
    /**
     * Execution was aborted
     */
    ABORTED,
    /**
     * Partial success for some nodes
     */
    NODE_PARTIAL_SUCCEEDED,
    /**
     * Mixed states among nodes
     */
    NODE_MIXED,
    /**
     * After waiting the execution did not start
     */
    NOT_STARTED,
    ;

    public boolean isCompletedState() {
        return this == ExecutionState.ABORTED || this == ExecutionState.SUCCEEDED || this == ExecutionState.FAILED
                || this == ExecutionState.NODE_MIXED || this == ExecutionState.NODE_PARTIAL_SUCCEEDED
                || this == NOT_STARTED
                ;
    }
}
