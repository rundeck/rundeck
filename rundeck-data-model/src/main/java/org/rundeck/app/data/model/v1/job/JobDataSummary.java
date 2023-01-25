package org.rundeck.app.data.model.v1.job;

import java.io.Serializable;

public interface JobDataSummary {
    Serializable getId();
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
