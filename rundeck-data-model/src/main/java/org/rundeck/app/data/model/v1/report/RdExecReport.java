package org.rundeck.app.data.model.v1.report;

import java.util.Date;

public interface RdExecReport{
    String getNode();
    String getTitle();
    String getStatus();
    String getActionType();
    String getCtxProject();
    String getCtxType();
    String getCtxName();
    String getMaprefUri();
    String getReportId();
    String getTags();
    String getAuthor();
    Date getDateStarted();
    Date getDateCompleted();
    String getMessage();
    String getCtxCommand();
    String getCtxController();
    Long getExecutionId();
    String getJcJobId();
    Boolean getAdhocExecution();
    String getAdhocScript();
    String getAbortedByUser();
    String getSucceededNodeList();
    String getFailedNodeList();
    String getFilterApplied();
}
