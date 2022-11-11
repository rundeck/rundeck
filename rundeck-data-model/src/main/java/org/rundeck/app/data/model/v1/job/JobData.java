package org.rundeck.app.data.model.v1.job;

import org.rundeck.app.data.model.v1.job.config.LogConfig;
import org.rundeck.app.data.model.v1.job.config.NodeConfig;
import org.rundeck.app.data.model.v1.job.notification.NotificationData;
import org.rundeck.app.data.model.v1.job.option.OptionData;
import org.rundeck.app.data.model.v1.job.orchestrator.OrchestratorData;
import org.rundeck.app.data.model.v1.job.schedule.ScheduleData;
import org.rundeck.app.data.model.v1.job.workflow.WorkflowData;

import java.io.Serializable;
import java.util.*;

public interface JobData {
    Serializable getId();
    String getUuid();
    String getJobName();
    String getDescription();
    String getProject();
    String getArgString();
    String getUser();
    String getTimeout();
    String getRetry();
    String getRetryDelay();
    String getGroupPath();
    String getUserRoleList();
    Boolean getScheduled();
    Boolean getScheduleEnabled();
    Boolean getExecutionEnabled();
    Boolean getMultipleExecutions();
    Boolean getNodesSelectedByDefault();
    Boolean getNodeKeepgoing();
    Boolean getDoNodedispatch();
    Boolean getNodeRankOrderAscending();
    Boolean getNodeFilterEditable();
    String getNotifyAvgDurationThreshold();
    String getTimeZone();
    String getDefaultTab();
    String getMaxMultipleExecutions();
    Date getDateCreated();
    Date getLastUpdated();
    Integer getNodeThreadcount();
    String  getNodeThreadcountDynamic();
    String  getNodeRankAttribute();
    String getServerNodeUUID();
    Map<String, Object> getPluginConfigMap();

    LogConfig getLogConfig();
    NodeConfig getNodeConfig();
    SortedSet<OptionData> getOptionSet();
    Set<NotificationData> getNotificationSet();
    WorkflowData getWorkflow();
    ScheduleData getSchedule();
    OrchestratorData getOrchestrator();

}
