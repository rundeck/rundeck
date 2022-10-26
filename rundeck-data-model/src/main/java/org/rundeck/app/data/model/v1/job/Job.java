package org.rundeck.app.data.model.v1.job;

import org.rundeck.app.data.model.v1.job.config.LogConfig;
import org.rundeck.app.data.model.v1.job.config.NodeConfig;
import org.rundeck.app.data.model.v1.job.config.PluginConfig;
import org.rundeck.app.data.model.v1.job.notification.Notifications;
import org.rundeck.app.data.model.v1.job.option.JobOptions;
import org.rundeck.app.data.model.v1.job.orchestrator.Orchestrator;
import org.rundeck.app.data.model.v1.job.schedule.Schedule;
import org.rundeck.app.data.model.v1.job.workflow.Workflow;

import java.io.Serializable;
import java.util.Date;

public interface Job {
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
    Boolean isScheduled();
    Boolean isScheduleEnabled();
    Boolean isExecutionEnabled();
    Boolean isMultipleExecutions();
    String getNotifyAvgDurationThreshold();
    String getTimeZone();
    String getDefaultTab();
    String getMaxMultipleExecutions();
    Date getDateCreated();
    Date getLastUpdated();

    LogConfig getLogConfig();
    NodeConfig getNodeConfig();
    JobOptions getOptions();
    Notifications getNotifications();
    Workflow getWorkflow();
    Schedule getSchedule();
    Orchestrator getOrchestrator();
    PluginConfig getPluginConfig();
}
