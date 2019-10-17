package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Status result returned from job lifecycle event handlers
 */
public interface JobEventStatus extends LifecycleStatus{

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
}
