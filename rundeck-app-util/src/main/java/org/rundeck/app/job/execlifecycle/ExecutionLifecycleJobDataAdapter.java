package org.rundeck.app.job.execlifecycle;

import com.dtolabs.rundeck.core.plugins.PluginConfigSet;
import org.rundeck.app.data.model.v1.job.JobData;

public interface ExecutionLifecycleJobDataAdapter {
    /**
     * Read the config set for the job
     * @param job
     * @return PluginConfigSet for the ExecutionLifecyclePlugin service for the job, or null if not defined or not enabled
     */
    PluginConfigSet getExecutionLifecyclePluginConfigSetForJob(JobData job);
}
