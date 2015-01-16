package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.Date;
import java.util.Map;

/**
 * Describes the state of a step, which includes the execution status, and possible metadata
 */
public interface StepState extends HasExecutionState {
    /**
     * @return the execution state
     */
    public ExecutionState getExecutionState();

    /**
     * @return Any metadata
     */
    public Map getMetadata();

    /**
     * @return Potential error message if state is failed
     */
    public String getErrorMessage();

    /**
     * @return Timestamp that the executionState left WAITING
     */
    public Date getStartTime();
    /**
     * @return Last timestamp that the executionState changed
     */
    public Date getUpdateTime();

    /**
     * @return Timestamp that the executionState was completed
     */
    public Date getEndTime();
}
