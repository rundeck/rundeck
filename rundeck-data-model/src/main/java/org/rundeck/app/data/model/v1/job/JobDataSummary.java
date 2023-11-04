package org.rundeck.app.data.model.v1.job;

public interface JobDataSummary extends JobGroupData {
    String getUuid();
    String getJobName();
    String getDescription();
    String getProject();
    Boolean getScheduled();
    Boolean getScheduleEnabled();
    Boolean getExecutionEnabled();
    String getServerNodeUUID();
}
