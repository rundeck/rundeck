package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.rules.StateObj;
import com.dtolabs.rundeck.core.rules.WorkflowSystem;

/**
 * Successful result of a workflow step operation
 */
class EngineWorkflowStepOperationSuccess implements WorkflowSystem.OperationSuccess {
    int stepNum;
    StepExecutionResult result;
    private StateObj newState;
    ControlBehavior controlBehavior;
    String statusString;

    EngineWorkflowStepOperationSuccess(
            final int stepNum,
            final StepExecutionResult result,
            final StateObj newState, final ControlBehavior controlBehavior, final String statusString
    )
    {
        this.stepNum = stepNum;
        this.result = result;
        this.newState = newState;
        this.controlBehavior = controlBehavior;
        this.statusString = statusString;
    }

    @Override
    public StateObj getNewState() {
        return newState;
    }

    @Override
    public String toString() {
        return "StepSuccess{" +
               "stepNum=" + stepNum +
               ", result=" + result +
               ", newState=" + newState +
               '}';
    }
}
