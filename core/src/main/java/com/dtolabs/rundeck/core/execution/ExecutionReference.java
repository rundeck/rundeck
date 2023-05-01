package com.dtolabs.rundeck.core.execution;


import com.dtolabs.rundeck.core.jobs.JobReference;

import java.util.Date;
import java.util.Map;

public interface ExecutionReference extends PreparedExecutionReference{

    /**
     * If this execution is a retry, return the ID of the original execution
     * @return
     */
    default String getRetryOriginalId() {
        return null;
    }

    /**
     * If this execution is a retry, return the ID of the previous execution
     * @return
     */
    default String getRetryPrevId() {
        return null;
    }
    /**
     * If this execution was retried, return the ID of the retry execution
     * @return
     */
    default String getRetryNextId() {
        return null;
    }
    String getFilter();
    String getOptions();
    Date getDateStarted();
    Date getDateCompleted();
    String getStatus();
    String getSucceededNodeList();
    String getFailedNodeList();
    String getTargetNodes();
    String getAdhocCommand();

    Map getMetadata();
    boolean isDoNodedispatch();

}
