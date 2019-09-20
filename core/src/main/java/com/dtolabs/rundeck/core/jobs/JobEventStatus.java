package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Status result returned from lifecycle event handlers
 */
public interface JobEventStatus {

    /**
     * @return true if event handler was successful
     */
    default boolean isSuccessful() {
        return true;
    }

    /**
     * @return descriptive error message when result is not successful
     */
    default String getErrorMessage() {
        return null;
    }

    default boolean useNewValues(){ return false; }

    /**
     * @return option values to use when isUseNewValues is true
     */
    default Map getOptionsValues(){ return null; }

    /**
     *
     * @return options to use when isUseNewValues is true
     */
    default SortedSet<JobOption> getOptions() {
        return new TreeSet<>();
    }

    /**
     * @return StepExecutionContext of the event to use if isUseNewValues is true
     */
    default StepExecutionContext getExecutionContext(){ return null; }

}
