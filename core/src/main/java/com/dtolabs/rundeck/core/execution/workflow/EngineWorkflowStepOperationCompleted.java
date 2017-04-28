package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.dispatcher.DataContext;
import com.dtolabs.rundeck.core.dispatcher.MultiDataContext;
import com.dtolabs.rundeck.core.rules.StateObj;
import com.dtolabs.rundeck.core.rules.WorkflowSystem;

/**
 * Successful result of a workflow step operation
 */
class EngineWorkflowStepOperationCompleted implements WorkflowSystem.OperationCompleted<WFSharedContext> {
    int stepNum;
    BaseWorkflowExecutor.StepResultCapture stepResultCapture;
    private StateObj newState;

    EngineWorkflowStepOperationCompleted(
            final int stepNum,
            final StateObj newState, BaseWorkflowExecutor.StepResultCapture stepResultCapture
    )
    {

        this.stepNum = stepNum;
        this.newState = newState;
        this.stepResultCapture = stepResultCapture;
    }

    @Override
    public WFSharedContext getResult() {
        return stepResultCapture.getResultData();
    }
    public BaseWorkflowExecutor.StepResultCapture getStepResultCapture() {
        return stepResultCapture;
    }

    @Override
    public StateObj getNewState() {
        return newState;
    }

    @Override
    public String toString() {
        return "StepSuccess{" +
               "stepNum=" + stepNum +
               ", stepResultCapture=" + stepResultCapture +
               ", newState=" + newState +
               '}';
    }
}
