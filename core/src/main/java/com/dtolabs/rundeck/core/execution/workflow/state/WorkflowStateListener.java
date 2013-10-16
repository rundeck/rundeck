package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.Date;

/**
 * Listens to state changes for a workflow
 */
public interface WorkflowStateListener {
    /**
     * A step changed state, as identified in the {@link StepState}
     *
     * @param identifier      the new state identifier
     * @param stepStateChange the change to the state
     * @param timestamp       the time of the change
     */
    public void stepStateChanged(StepIdentifier identifier, StepStateChange stepStateChange, Date timestamp);

    /**
     * The workflow execution state changed
     *
     * @param executionState the new execution state
     * @param timestamp      the time of the change
     */
    public void workflowExecutionStateChanged(ExecutionState executionState, Date timestamp);
}
