package com.dtolabs.rundeck.core.jobs;

import java.util.*;


/**
 * Status result returned from job lifecycle event handlers
 */
public interface JobLifecycleStatus
    extends LifecycleStatus {

    /**
     * Get the Event Type that triggered this lifecycle status result.
     * The event type is the full class name of the event.
     * @return The Class of the event object or JobEvent by default
     */
    default Class<? extends JobEvent> getJobEventType() {
        return JobEvent.class;
    }

    default boolean isTriggeredByJobEvent(Class<? extends JobEvent> eventType) {
        Class triggerEventType = getJobEventType();
        return triggerEventType != null && eventType != null && eventType.isAssignableFrom(triggerEventType);
    }

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
