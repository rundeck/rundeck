package com.dtolabs.rundeck.core.execution.workflow.state;

/**
 * $INTERFACE is ... User: greg Date: 10/15/13 Time: 4:59 PM
 */
public class StepStateChangeImpl implements StepStateChange {
    private StepState stepState;
    private String nodeName;
    private boolean nodeState;

    public StepState getStepState() {
        return stepState;
    }

    public void setStepState(StepState stepState) {
        this.stepState = stepState;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public boolean isNodeState() {
        return nodeState;
    }

    public void setNodeState(boolean nodeState) {
        this.nodeState = nodeState;
    }

    @Override
    public String toString() {
        return "StepStateChangeImpl{" +
                "stepState=" + stepState +
                ", nodeName='" + nodeName + '\'' +
                ", nodeState=" + nodeState +
                '}';
    }
}
