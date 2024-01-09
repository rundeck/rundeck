package org.rundeck.app.data.model.v1.execution;

import java.util.Date;

public interface ExecutionDataSummary {
    String getUuid();
    String getJobUuid();
    String getProject();
    String getStatus();
    Date getDateStarted();
    Date getDateCompleted();
    String getServerNodeUUID();
}
