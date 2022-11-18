package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Status result returned from job lifecycle event handlers
 */
public interface JobLifecycleStatus
    extends LifecycleStatus {
    
    /**
     * @return option values to use when isUseNewValues is true
     */
    default Map getOptionsValues() {
        return null;
    }
    
    /**
     * @return options to use when isUseNewValues is true
     */
    default SortedSet<JobOption> getOptions() {
        return new TreeSet<>();
    }
    
    
    /**
     * @return true indicates metadata returned by this status result should be used.
     */
    default boolean isUseNewMetadata() {
        return false;
    }
    
    default Map getNewExecutionMetadata() {
        return null;
    }
    
}
