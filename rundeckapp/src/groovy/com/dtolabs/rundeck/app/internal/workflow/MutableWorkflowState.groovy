package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier
import com.dtolabs.rundeck.core.execution.workflow.state.StepState
import com.dtolabs.rundeck.core.execution.workflow.state.StepStateChange
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowState

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 10/15/13
 * Time: 3:42 PM
 */
public interface MutableWorkflowState extends WorkflowState {
    /**
     * Update the state for a step
     * @param identifier
     * @param stepStateChange
     * @param timestamp
     */
    void updateStateForStep(StepIdentifier identifier, StepStateChange stepStateChange, Date timestamp);
    /**
     * Update the state for this workflow
     * @param executionState
     * @param timestamp
     */
    void updateWorkflowState(ExecutionState executionState, Date timestamp, List<String> nodeNames);

    /**
     * Update state for a sub workflow
     * @param identifier
     * @param executionState
     * @param timestamp
     * @param nodeNames
     */
    void updateSubWorkflowState(StepIdentifier identifier, ExecutionState executionState, Date timestamp, List<String> nodeNames);

}
