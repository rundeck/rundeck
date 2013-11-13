package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.Date;
import java.util.Map;

/**
 * $INTERFACE is ... User: greg Date: 10/15/13 Time: 4:13 PM
 */
public class StepStateImpl implements StepState {

    private ExecutionState executionState;
    private Map metadata;
    private String errorMessage;
    private Date startTime;
    private Date updateTime;
    private Date endTime;


    public Map getMetadata() {
        return metadata;
    }

    public void setMetadata(Map metadata) {
        this.metadata = metadata;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ExecutionState getExecutionState() {
        return executionState;
    }

    public void setExecutionState(ExecutionState executionState) {
        this.executionState = executionState;
    }

    @Override
    public String toString() {
        return "StepStateImpl{" +
                "executionState=" + executionState +
                ", metadata=" + metadata +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
