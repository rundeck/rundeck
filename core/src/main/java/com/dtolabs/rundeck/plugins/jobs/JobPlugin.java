package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.execution.JobPluginException;
import com.dtolabs.rundeck.core.jobs.JobEvent;
import com.dtolabs.rundeck.core.jobs.JobEventStatus;


/**
 * JobPlugin interface for a task to be executed during the job life cycle
 * Created by rnavarro
 * Date: 5/20/19
 * Time: 10:22 AM
 */
public interface JobPlugin {

    /**
     * It triggers before the job starts
     * @param event event execution data
     * @return JobEventStatus
     */
    default public JobEventStatus beforeJobStarts(JobEvent event) throws JobPluginException{
        return null;
    }

    /**
     * It triggers when a job ends
     * @param event event execution data
     * @return JobEventStatus
     */
    default public JobEventStatus afterJobEnds(JobEvent event) throws JobPluginException{
        return null;
    }

}