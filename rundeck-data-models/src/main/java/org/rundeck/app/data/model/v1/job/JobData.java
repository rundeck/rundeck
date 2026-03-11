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
package org.rundeck.app.data.model.v1.job;

import org.rundeck.app.data.model.v1.job.component.JobComponentData;
import org.rundeck.app.data.model.v1.job.config.LogConfig;
import org.rundeck.app.data.model.v1.job.config.NodeConfig;
import org.rundeck.app.data.model.v1.job.notification.NotificationData;
import org.rundeck.app.data.model.v1.job.option.OptionData;
import org.rundeck.app.data.model.v1.job.orchestrator.OrchestratorData;
import org.rundeck.app.data.model.v1.job.schedule.ScheduleData;
import org.rundeck.app.data.model.v1.job.workflow.WorkflowData;

import java.io.Serializable;
import java.util.*;

public interface JobData extends JobDataSummary {
    String getArgString();
    String getUser();
    String getTimeout();
    String getRetry();
    String getRetryDelay();
    List<String> getUserRoles();
    Boolean getMultipleExecutions();
    String getNotifyAvgDurationThreshold();
    String getTimeZone();
    String getDefaultTab();
    String getMaxMultipleExecutions();
    Date getDateCreated();
    Date getLastUpdated();

    /**
     * Configuration for plugins that are attached to this job, such as execution lifecycle plugins
     * @return a map containing the plugin name as the key and the configuration data as the value
     */
    Map<String, Object> getPluginConfigMap();

    LogConfig getLogConfig();
    NodeConfig getNodeConfig();
    SortedSet<OptionData> getOptionSet();
    Set<NotificationData> getNotificationSet();
    WorkflowData getWorkflow();
    ScheduleData getSchedule();
    OrchestratorData getOrchestrator();

    /**
     * Configuration for job components attached to this job
     * @return a map containing the component name as the key and the configuration data as the value
     */
    Map<String, JobComponentData> getComponents();
}
