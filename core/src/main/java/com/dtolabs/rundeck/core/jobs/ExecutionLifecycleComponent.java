package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.ExecutionLifecycleComponentException;

public interface ExecutionLifecycleComponent {

    /**
     * It triggers before the job starts
     * @param event event execution data
     * @return JobEventStatus
     */
    ExecutionLifecycleStatus beforeJobStarts(JobExecutionEvent event) throws ExecutionLifecycleComponentException;

    /**
     * It triggers when job is aborting execution
     * @param event event execution data
     * @return JobEventStatus
     */
    ExecutionLifecycleStatus jobIsAborting(JobExecutionEvent event) throws ExecutionLifecycleComponentException;

    /**
     * It triggers when a job ends
     * @param event event execution data
     * @return JobEventStatus
     */
    ExecutionLifecycleStatus afterJobEnds(JobExecutionEvent event) throws ExecutionLifecycleComponentException;
}
