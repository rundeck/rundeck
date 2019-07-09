package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.JobPluginException;

public interface CoreJobPluginService {

    JobEventStatus beforeJobStarts(JobEvent event) throws JobPluginException;

    JobEventStatus afterJobEnds(JobEvent event) throws JobPluginException;

}
