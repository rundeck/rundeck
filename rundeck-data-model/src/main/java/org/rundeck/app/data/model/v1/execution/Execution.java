package org.rundeck.app.data.model.v1.execution;

import org.rundeck.app.data.model.v1.job.JobData;
import org.rundeck.app.data.model.v1.logstorage.LogFileStorageRequest;

import java.io.Serializable;

public interface Execution {
    Serializable getId();
    Serializable getJobId();
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
    Serializable getRetryOriginalId();
    Serializable getRetryPrevId();
    Serializable getRetryExecutionId();
    Serializable getLogFileStorageRequestId();
    Boolean isCancelled();
    Boolean isTimedOut();
    Boolean isWillRetry();
    Boolean isServerNodeUUIDChanged();
}
