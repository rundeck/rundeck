package org.rundeck.app.data.model.v1.execution;

import org.rundeck.app.data.model.v1.job.JobData;
import org.rundeck.app.data.model.v1.logstorage.LogFileStorageRequest;

import java.io.Serializable;

public interface Execution {
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
    boolean isCancelled();
    Boolean getTimedOut();
    Boolean getWillRetry();
    boolean isServerNodeUUIDChanged();
    String getNodeInclude();
}
