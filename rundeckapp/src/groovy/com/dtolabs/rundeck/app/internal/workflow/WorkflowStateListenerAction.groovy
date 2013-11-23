package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier
import com.dtolabs.rundeck.core.execution.workflow.state.StepStateChange
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowStateListener

/**
 * Listener implementation which invokes a closure when an event is received.
 */
class WorkflowStateListenerAction implements WorkflowStateListener{
    Closure onStepStateChanged
    Closure onWorkflowExecutionStateChanged
    Closure onSubWorkflowExecutionStateChanged
    @Override
    void stepStateChanged(StepIdentifier identifier, StepStateChange stepStateChange, Date timestamp) {
        if(null!=onStepStateChanged){
            onStepStateChanged(identifier,stepStateChange,timestamp)
        }
    }

    @Override
    void workflowExecutionStateChanged(ExecutionState executionState, Date timestamp, List<String> nodeSet) {

        if (null != onWorkflowExecutionStateChanged) {
            onWorkflowExecutionStateChanged(executionState, timestamp, nodeSet)
        }
    }

    @Override
    void subWorkflowExecutionStateChanged(StepIdentifier identifier, ExecutionState executionState, Date timestamp, List<String> nodeSet) {
        if(null!=onSubWorkflowExecutionStateChanged){
            onSubWorkflowExecutionStateChanged(identifier,executionState,timestamp,nodeSet)
        }
    }
}
