package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.execution.ExecutionLifecyclePluginException;
import com.dtolabs.rundeck.core.jobs.ExecutionLifecycleStatus;
import com.dtolabs.rundeck.core.jobs.JobExecutionEvent;
import com.dtolabs.rundeck.core.jobs.JobEventStatus;


/**
 * ExecutionLifecyclePlugin interface for a task to be executed during the job execution life cycle
 * Created by rnavarro
 * Date: 9/04/19
 * Time: 01:32 PM
 */
public interface ExecutionLifecyclePlugin {

    /**
     * It triggers before the job starts
     * @param event event execution data
     * @return JobEventStatus
     */
    default ExecutionLifecycleStatus beforeJobStarts(JobExecutionEvent event) throws ExecutionLifecyclePluginException {
        return null;
    }

    /**
     * It triggers when a job ends
     * @param event event execution data
     * @return JobEventStatus
     */
    default ExecutionLifecycleStatus afterJobEnds(JobExecutionEvent event) throws ExecutionLifecyclePluginException {
        return null;
    }
}
