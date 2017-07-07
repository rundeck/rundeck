package com.dtolabs.rundeck.core.execution;


import com.dtolabs.rundeck.core.jobs.JobReference;

import java.util.Date;

public interface ExecutionReference {
    String getId();
    String getFilter();
    String getOptions();
    JobReference getJob();
    Date getDateStarted();
    String getStatus();
    String getSucceededNodeList();
    String getFailedNodeList();
    String getTargetNodes();


}
