package com.dtolabs.rundeck.core.execution.workflow.state;

import java.util.Map;

/**
 * Describes the state of a step, which includes the execution status, and possible metadata
 */
public interface StepState {
    /**
     * The execution state
     * @return
     */
    public ExecutionState getExecutionState();

    /**
     * Any metadata
     * @return
     */
    public Map getMetadata();

    /**
     * Potential error message if state is failed
     * @return
     */
    public String getErrorMessage();
}
