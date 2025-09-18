/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rundeck.app.data.model.v1.execution;

import org.rundeck.app.data.model.v1.job.config.NodeConfig;
import org.rundeck.app.data.model.v1.job.orchestrator.OrchestratorData;
import org.rundeck.app.data.model.v1.job.workflow.WorkflowData;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ExecutionData {
    Serializable getInternalId();
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
    Map<String,Object> getExtraMetadataMap();

    //transient methods
    String getExecutionState();
    boolean isServerNodeUUIDChanged();
    boolean statusSucceeded();

}
