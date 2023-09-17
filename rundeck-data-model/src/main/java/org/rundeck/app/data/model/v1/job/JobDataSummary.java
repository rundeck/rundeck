package org.rundeck.app.data.model.v1.job;

public interface JobDataSummary {
    String getUuid();
    String getJobName();
    String getDescription();
    String getGroupPath();
    String getProject();
    Boolean getScheduled();
    Boolean getScheduleEnabled();
    Boolean getExecutionEnabled();
    String getServerNodeUUID();
}
