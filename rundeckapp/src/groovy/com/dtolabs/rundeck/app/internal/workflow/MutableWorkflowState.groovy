package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.MutableExecutionState
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
public interface MutableWorkflowState extends WorkflowState, MutableExecutionState {
    /**
     * Update the state for a step
     * @param identifier
     * @param index current index into identifier context
     * @param stepStateChange
     * @param timestamp
     */
    void updateStateForStep(StepIdentifier identifier, int index,StepStateChange stepStateChange, Date timestamp);
    /**
     * Touch the state for a step, ensuring intermediate steps are touched as well,
     * updating the timestamp and transitioning from Waiting to Running if necessary
     * @param identifier
     * @param index
     * @param timestamp
     */
    void touchStateForStep(StepIdentifier identifier, int index, StepStateChange stepStateChange,Date timestamp);
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
    void updateSubWorkflowState(StepIdentifier identifier, int index, boolean quellFinalState, ExecutionState executionState, Date timestamp, List<String> nodeNames, MutableWorkflowState parent);


    public Map<String,? extends MutableWorkflowNodeState> getMutableNodeStates();
}
