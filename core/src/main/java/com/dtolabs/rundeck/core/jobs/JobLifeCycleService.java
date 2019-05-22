package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.JobLifeCycleException;

public interface JobLifeCycleService {

    JobLifeCycleStatus beforeJobStarts(JobLifeCycleEvent event) throws JobLifeCycleException;

    JobLifeCycleStatus afterJobEnds(JobLifeCycleEvent event) throws JobLifeCycleException;

}
