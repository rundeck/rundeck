package com.dtolabs.rundeck.core.execution;


import com.dtolabs.rundeck.core.jobs.JobReference;

import java.util.Date;

public interface ExecutionReference {
    public String getId();
    public String getFilter();
    public String getOptions();
    public JobReference getJob();
    public Date getDateStarted();
    public String getStatus();

}
