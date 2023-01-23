package org.rundeck.app.data.providers.v1;

import org.rundeck.app.data.model.v1.report.RdExecReport;

import java.util.List;

public interface ExecReportDataProvider extends DataProvider{
    RdExecReport get(Long id);
    List<RdExecReport> findAllByStatus(String status);
    List<RdExecReport> findAllByExecutionId(Long id);
    List<RdExecReport> findAllByCtxProjectAndExecutionIdInList(String projectName, List<Long> execIds);
    int countByCtxProject(String projectName);
    void deleteByCtxProject(String projectName);
}
