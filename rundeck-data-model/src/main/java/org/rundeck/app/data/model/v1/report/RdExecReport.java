package org.rundeck.app.data.model.v1.report;

public interface RdExecReport {
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
