package com.dtolabs.rundeck.core.execution.workflow.state;

/**
 * $INTERFACE is ... User: greg Date: 10/15/13 Time: 2:52 PM
 */
public enum ExecutionState {
    WAITING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    ABORTED;

    public boolean isCompletedState() {
        return this == ExecutionState.ABORTED || this == ExecutionState.SUCCEEDED || this == ExecutionState.FAILED;
    }
}
