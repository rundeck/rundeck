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

public interface JobData {
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
    List<String> getUserRoles();
    Boolean getScheduled();
    Boolean getScheduleEnabled();
    Boolean getExecutionEnabled();
    Boolean getMultipleExecutions();
    String getNotifyAvgDurationThreshold();
    String getTimeZone();
    String getDefaultTab();
    String getMaxMultipleExecutions();
    Date getDateCreated();
    Date getLastUpdated();
    String getServerNodeUUID();

    /**
     * Configuration for plugins that are attached to this job, such as execution lifecycle plugins
     * @return
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
     * @return
     */
    Map<String, JobComponentData> getComponents();
}
