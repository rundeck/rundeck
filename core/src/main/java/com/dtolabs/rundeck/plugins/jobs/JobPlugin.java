package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.execution.JobPluginException;
import com.dtolabs.rundeck.core.jobs.JobExecutionEvent;
import com.dtolabs.rundeck.core.jobs.JobEventStatus;
import com.dtolabs.rundeck.core.jobs.JobPersistEvent;
import com.dtolabs.rundeck.core.jobs.JobPreExecutionEvent;


/**
 * JobPlugin interface for a task to be executed during the job life cycle
 * Created by rnavarro
 * Date: 5/20/19
 * Time: 10:22 AM
 */
public interface JobPlugin {


    /**
     * It triggers before the job execution context exist
     * @param event event execution data
     * @return JobEventStatus
     */
    default public JobEventStatus beforeJobExecution(JobPreExecutionEvent event) throws JobPluginException{
        return null;
    }

    /**
     * It triggers before the job starts
     * @param event event execution data
     * @return JobEventStatus
     */
    default public JobEventStatus beforeJobStarts(JobExecutionEvent event) throws JobPluginException{
        return null;
    }

    /**
     * It triggers when a job ends
     * @param event event execution data
     * @return JobEventStatus
     */
    default public JobEventStatus afterJobEnds(JobExecutionEvent event) throws JobPluginException{
        return null;
    }

    /**
     * It triggers when a job is persisted
     * @param event event saving data
     * @return JobEventStatus
     */
    default public JobEventStatus beforeSaveJob(JobPersistEvent event) throws JobPluginException{
        return null;
    }

}