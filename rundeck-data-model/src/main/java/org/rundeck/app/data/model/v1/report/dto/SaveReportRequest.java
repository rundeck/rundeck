package org.rundeck.app.data.model.v1.report.dto;

import java.util.Date;

public interface SaveReportRequest {
    Long getExecutionId();
    Date getDateStarted();
    String getJobId();
    String getReportId();
    Boolean getAdhocExecution();
    String getSucceededNodeList();
    String getFailedNodeList();
    String getFilterApplied();
    String getProject();
    String getAbortedByUser();
    String getAuthor();
    String getTitle();
    String getStatus();
    String getNode();
    String getMessage();
    Date getDateCompleted();
    String getAdhocScript();
    String getTags();
    String getJobUuid();
}
