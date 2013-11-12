package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.*;

/**
 * $INTERFACE is ... User: greg Date: 10/15/13 Time: 3:35 PM
 */
public class WorkflowStateImpl implements WorkflowState {
    private ArrayList<String> nodeSet;
    private long stepCount;
    private ExecutionState executionState;
    private Date timestamp;
    private ArrayList<WorkflowStepState> stepStates;

    public WorkflowStateImpl(List<String> nodeSet, long stepCount, ExecutionState executionState, Date timestamp,
            ArrayList<WorkflowStepState> stepStates) {
        this.setNodeSet(nodeSet);
        this.setStepCount(stepCount);
        this.setExecutionState(executionState);
        this.setTimestamp(timestamp);
        this.setStepStates(stepStates);
    }

    public List<String> getNodeSet() {
        return nodeSet;
    }

    public long getStepCount() {
        return stepCount;
    }

    public ExecutionState getExecutionState() {
        return executionState;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public List<WorkflowStepState> getStepStates() {
        return stepStates;
    }

    public void setNodeSet(List<String> nodeSet) {
        this.nodeSet = new ArrayList<String>(nodeSet);
    }

    public void setStepCount(long stepCount) {
        this.stepCount = stepCount;
    }

    public void setExecutionState(ExecutionState executionState) {
        this.executionState = executionState;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setStepStates(ArrayList<WorkflowStepState> stepStates) {
        this.stepStates = stepStates;
    }
}
