package org.rundeck.app.data.providers.v1;

import org.rundeck.app.data.model.v1.query.RdExecQuery;
import org.rundeck.app.data.model.v1.report.RdExecReport;
import org.rundeck.app.data.model.v1.report.dto.ReportFilter;
import org.rundeck.app.data.model.v1.report.dto.SaveReportRequest;
import org.rundeck.app.data.model.v1.report.dto.SaveReportResponse;

import java.util.Collection;
import java.util.List;

public interface ExecReportDataProvider extends DataProvider{
    RdExecReport get(Long id);
    RdExecReport fromExecWithScheduledAndSave(Long executionId);
    SaveReportResponse fromExecWithId(Long id);
    SaveReportResponse saveReport(SaveReportRequest saveReportRequest);
    List<RdExecReport> findAllByCtxProject(String projectName);
    List<RdExecReport> findAllByStatus(String status);
    List<RdExecReport> findAllByExecutionId(Long id);
    List<RdExecReport> findAllByCtxProjectAndExecutionIdInList(String projectName, List<Long> execIds);
    int countByCtxProject(String projectName);
    int countExecutionReports(RdExecQuery execQuery);
    int countExecutionReportsWithTransaction(RdExecQuery execQuery, boolean isJobs, Long scheduledExecutionId, Integer isolationLevel);
    int countAndSaveByStatus();
    Collection<String> getRunList(RdExecQuery execQuery, ReportFilter filters, boolean isJobs, Long scheduledExecutionId);
    void deleteByCtxProject(String projectName);
    void deleteWithTransaction(String projectName);
    void deleteAllByExecutionId(Long executionId);
}
