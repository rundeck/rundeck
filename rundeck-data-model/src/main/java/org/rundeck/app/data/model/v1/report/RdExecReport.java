package org.rundeck.app.data.model.v1.report;

import java.util.Date;

public interface RdExecReport{
    String getNode();
    String getTitle();
    String getStatus();
    String getCtxProject();
    String getProject();
    String getReportId();
    String getTags();
    String getAuthor();
    Date getDateStarted();
    Date getDateCompleted();
    String getMessage();
    Long getExecutionId();
    String getJcJobId();
    String getJobId();
    Boolean getAdhocExecution();
    String getAdhocScript();
    String getAbortedByUser();
    String getSucceededNodeList();
    String getFailedNodeList();
    String getFilterApplied();
}
