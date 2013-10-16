package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.Map;

/**
 * $INTERFACE is ... User: greg Date: 10/15/13 Time: 3:00 PM
 */
public class WorkflowStepStateImpl implements WorkflowStepState {
    private StepIdentifier stepIdentifier;
    private StepState stepState;
    private Map<String, StepState> nodeStateMap;
    private boolean subWorkflow;
    private WorkflowState subWorkflowState;


    public StepIdentifier getStepIdentifier() {
        return stepIdentifier;
    }

    public boolean hasSubWorkflow() {
        return subWorkflow;
    }

    public WorkflowState getSubWorkflowState() {
        return subWorkflowState;
    }

    public StepState getStepState() {
        return stepState;
    }

    public Map<String, StepState> getNodeStateMap() {
        return nodeStateMap;
    }

    public void setStepIdentifier(StepIdentifier stepIdentifier) {
        this.stepIdentifier = stepIdentifier;
    }

    public void setStepState(StepState stepState) {
        this.stepState = stepState;
    }

    public void setNodeStateMap(Map<String, StepState> nodeStateMap) {
        this.nodeStateMap = nodeStateMap;
    }

    public void setSubWorkflow(boolean subWorkflow) {
        this.subWorkflow = subWorkflow;
    }

    public void setSubWorkflowState(WorkflowState subWorkflowState) {
        this.subWorkflowState = subWorkflowState;
    }
}
