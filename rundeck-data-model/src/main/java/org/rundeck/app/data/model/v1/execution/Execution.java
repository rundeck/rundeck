package org.rundeck.app.data.model.v1.execution;

import org.rundeck.app.data.model.v1.job.Job;
import org.rundeck.app.data.model.v1.logstorage.LogFileStorageRequest;

import java.io.Serializable;

public interface Execution {
    Serializable getId();
    Job getJob();
    String getUuid();
    String getProject();
    String getStatus();
    String getOutputfilepath();
    String getFailedNodeList();
    String getSucceededNodeList();
    String getAbortedby();
    String getExecutionType();
    String getUserRoleList();
    String getServerNodeUUID();
    String getExtraMetadata();
    Integer getRetryAttempt();
    Integer getNodeThreadcount();
    Long getRetryOriginalId();
    Long getRetryPrevId();
    Execution getRetryExecution();
    LogFileStorageRequest getLogFileStorageRequest();
    Boolean isCancelled();
    Boolean isTimedOut();
    Boolean isWillRetry();
    Boolean isServerNodeUUIDChanged();
}
