package com.dtolabs.rundeck.plugins.project;

import com.dtolabs.rundeck.core.jobs.JobLifecycleComponent;
import com.dtolabs.rundeck.core.jobs.JobLifecycleStatus;
import com.dtolabs.rundeck.core.jobs.JobPersistEvent;
import com.dtolabs.rundeck.core.jobs.JobPreExecutionEvent;
import com.dtolabs.rundeck.core.plugins.JobLifecyclePluginException;

/**
 * Interface for creating JobLifecyclePlugins
 * Created by rnavarro
 * Date: 8/23/19
 * Time: 10:45 AM
 */
public interface JobLifecyclePlugin extends JobLifecycleComponent {
    
    /**
     * It triggers before the job execution context exist
     * @param event event execution data
     * @return JobEventStatus
     */
    default JobLifecycleStatus beforeJobExecution(JobPreExecutionEvent event) throws JobLifecyclePluginException {
        return null;
    }
    
    /**
     * It triggers when a job is persisted
     * @param event event saving data
     * @return JobEventStatus
     */
    default JobLifecycleStatus beforeSaveJob(JobPersistEvent event) throws JobLifecyclePluginException {
        return null;
    }
    
}
