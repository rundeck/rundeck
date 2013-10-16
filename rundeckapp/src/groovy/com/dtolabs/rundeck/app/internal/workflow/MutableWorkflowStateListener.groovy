package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StepState
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
    void stepStateChanged(StepState stepState, Date timestamp) {
        mutableWorkflowState.updateStateForStep(stepState,timestamp)
    }

    @Override
    void workflowExecutionStateChanged(ExecutionState executionState, Date timestamp) {
        mutableWorkflowState.updateWorkflowState(executionState, timestamp)

    }
}
