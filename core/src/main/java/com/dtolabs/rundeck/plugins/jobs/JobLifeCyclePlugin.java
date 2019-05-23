package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.execution.JobLifeCycleException;
import com.dtolabs.rundeck.core.jobs.JobLifeCycleEvent;
import com.dtolabs.rundeck.core.jobs.JobLifeCycleStatus;


/**
 * JobLifeCyclePlugin interface for a task to be executed during the job life cycle
 * Created by rnavarro
 * Date: 5/20/19
 * Time: 10:22 AM
 */
public interface JobLifeCyclePlugin {

    /**
     * It triggers before the job starts
     * @param event event execution data
     * @return JobLifeCycleStatus
     */
    public JobLifeCycleStatus beforeJobStarts(JobLifeCycleEvent event) throws JobLifeCycleException;

    /**
     * It triggers when a job ends
     * @param event event execution data
     * @return JobLifeCycleStatus
     */
    public JobLifeCycleStatus afterJobEnds(JobLifeCycleEvent event) throws JobLifeCycleException;

}