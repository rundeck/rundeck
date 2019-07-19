package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.JobPluginException;

public interface IJobPluginService {

    JobEventStatus beforeJobStarts(JobExecutionEvent event) throws JobPluginException;

    JobEventStatus afterJobEnds(JobExecutionEvent event) throws JobPluginException;

}
