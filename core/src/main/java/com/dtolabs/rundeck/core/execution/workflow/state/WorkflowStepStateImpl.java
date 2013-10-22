package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.Map;
import java.util.Set;

/**
 * $INTERFACE is ... User: greg Date: 10/15/13 Time: 3:00 PM
 */
public class WorkflowStepStateImpl implements WorkflowStepState {
    private StepIdentifier stepIdentifier;
    private StepState stepState;
    private Map<String, StepState> nodeStateMap;
    private boolean subWorkflow;
    private WorkflowState subWorkflowState;
    private Set<String> nodeStepTargets;


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

    public Map<String, ? extends StepState> getNodeStateMap() {
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

    public Set<String> getNodeStepTargets() {
        return nodeStepTargets;
    }

    public void setNodeStepTargets(Set<String> nodeStepTargets) {
        this.nodeStepTargets = nodeStepTargets;
    }
}
