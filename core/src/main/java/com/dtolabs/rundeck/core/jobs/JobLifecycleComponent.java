package com.dtolabs.rundeck.core.jobs;

/**
 * A Job LifeCycle Components allow independent components and plugins to intervene and
 * modify a job behavior through various phases of its lifecycle.
 * 
 */
public interface JobLifecycleComponent {

    /**
     * It triggers before the job execution context exist
     * @param event event execution data
     * @return JobEventStatus
     */
    JobLifecycleStatus beforeJobExecution(JobPreExecutionEvent event) throws JobLifecycleComponentException;

    /**
     * It triggers when a job is persisted
     * @param event event saving data
     * @return JobEventStatus
     */
    JobLifecycleStatus beforeSaveJob(JobPersistEvent event) throws JobLifecycleComponentException;
    
}
