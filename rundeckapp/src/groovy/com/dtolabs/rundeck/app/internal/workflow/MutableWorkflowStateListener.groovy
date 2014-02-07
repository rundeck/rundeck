package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier
import com.dtolabs.rundeck.core.execution.workflow.state.StepState
import com.dtolabs.rundeck.core.execution.workflow.state.StepStateChange
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowStateListener

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 10/15/13
 * Time: 3:43 PM
 */
class MutableWorkflowStateListener implements WorkflowStateListener {
    private MutableWorkflowState mutableWorkflowState

    MutableWorkflowStateListener(MutableWorkflowState mutableWorkflowState) {
        this.mutableWorkflowState = mutableWorkflowState
    }

    @Override
    void stepStateChanged(StepIdentifier identifier, StepStateChange stepStateChange, Date timestamp) {
        mutableWorkflowState.updateStateForStep(identifier,0,stepStateChange, timestamp)
    }

    @Override
    void workflowExecutionStateChanged(ExecutionState executionState, Date timestamp, List<String> nodenames) {
        mutableWorkflowState.updateWorkflowState(executionState, timestamp, nodenames)
    }

    @Override
    void subWorkflowExecutionStateChanged(StepIdentifier identifier, ExecutionState executionState, Date timestamp, List<String> nodeSet) {
        mutableWorkflowState.updateSubWorkflowState(identifier,0, false, executionState, timestamp, nodeSet, mutableWorkflowState)
    }
}
