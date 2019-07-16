package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;

public interface JobEventStatus {

    public default boolean isSuccessful() {
        return true;
    }

    public default String getDescription() {
        return null;
    }

    public default boolean useNewValues(){ return false; }

    /**
     *
     * @return StepExecutionContext of the event.
     */
    StepExecutionContext getExecutionContext();

}
