package com.dtolabs.rundeck.core.execution;


import com.dtolabs.rundeck.core.jobs.JobReference;

public interface ExecutionReference {
    public String getId();
    public String getFilter();
    public String getOptions();
    public JobReference getJob();

}
