package org.rundeck.app.data.providers.v1;

import org.rundeck.app.data.model.v1.query.RdExecQuery;
import org.rundeck.app.data.model.v1.report.RdExecReport;
import org.rundeck.app.data.model.v1.report.dto.SaveReportRequest;
import org.rundeck.app.data.model.v1.report.dto.SaveReportResponse;

import java.util.Collection;
import java.util.List;

public interface ExecReportDataProvider extends DataProvider{
    RdExecReport get(Long id);
    SaveReportResponse createReportFromExecution(Long id);
    SaveReportResponse saveReport(SaveReportRequest saveReportRequest);
    List<RdExecReport> findAllByProject(String projectName);
    List<RdExecReport> findAllByStatus(String status);
    List<RdExecReport> findAllByExecutionId(Long id);
    List<RdExecReport> findAllByProjectAndExecutionIdInList(String projectName, List<Long> execIds);
    int countByProject(String projectName);
    int countExecutionReports(RdExecQuery execQuery);
    int countExecutionReportsWithTransaction(RdExecQuery execQuery, boolean isJobs, Long scheduledExecutionId);
    int countAndSaveByStatus();
    Collection<String> getExecutionReports(RdExecQuery execQuery, boolean isJobs, Long scheduledExecutionId);
    void deleteByProject(String projectName);
    void deleteWithTransaction(String projectName);
    void deleteAllByExecutionId(Long executionId);
}
