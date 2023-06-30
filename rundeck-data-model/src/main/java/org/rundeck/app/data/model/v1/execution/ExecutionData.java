package org.rundeck.app.data.model.v1.execution;

import org.rundeck.app.data.model.v1.job.config.NodeConfig;
import org.rundeck.app.data.model.v1.job.orchestrator.OrchestratorData;
import org.rundeck.app.data.model.v1.job.workflow.WorkflowData;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public interface ExecutionData {
    String getUuid();
    String getJobUuid();
    String getProject();
    String getArgString();
    String getUser();
    List<String> getUserRoles();
    String getLoglevel();
    String getTimeout();
    String getRetry();
    String getRetryDelay();
    String getStatus();
    Date getDateStarted();
    Date getDateCompleted();
    String getOutputfilepath();
    String getFailedNodeList();
    String getSucceededNodeList();
    String getAbortedby();
    String getExecutionType();
    String getServerNodeUUID();
    String getExtraMetadata();
    Integer getRetryAttempt();
    Integer getNodeThreadcount();
    Serializable getRetryExecutionId();
    Serializable getRetryOriginalId();
    Serializable getRetryPrevId();
    Serializable getLogFileStorageRequestId();
    boolean isCancelled();
    Boolean getTimedOut();
    Boolean getWillRetry();
    NodeConfig getNodeConfig();
    WorkflowData getWorkflow();
    OrchestratorData getOrchestrator();

    //transient methods
    String getExecutionState();
    boolean isServerNodeUUIDChanged();
    boolean statusSucceeded();
}
