package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;


public interface JobEventStatus {

    default boolean isSuccessful() {
        return true;
    }

    default String getDescription() {
        return null;
    }

    default boolean useNewValues(){ return false; }

    default Map getOptionsValues(){ return null; }

    default SortedSet<JobOption> getOptions(){ return new TreeSet<JobOption>(); }

    /**
     * @return StepExecutionContext of the event.
     */
    StepExecutionContext getExecutionContext();

}
