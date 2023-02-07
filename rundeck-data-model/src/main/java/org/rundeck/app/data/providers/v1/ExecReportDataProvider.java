package org.rundeck.app.data.providers.v1;

import org.rundeck.app.data.model.v1.report.RdExecReport;
import org.rundeck.app.data.model.v1.report.dto.SaveReportResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ExecReportDataProvider extends DataProvider{
    RdExecReport get(Long id);
    RdExecReport fromExecWithScheduledAndSave(Long executionId);
    SaveReportResponse fromExecWithId(Long id);
    SaveReportResponse saveFromMap(Map execReportMap);
    List<RdExecReport> findAllByStatus(String status);
    List<RdExecReport> findAllByExecutionId(Long id);
    List<RdExecReport> findAllByCtxProjectAndExecutionIdInList(String projectName, List<Long> execIds);
    int countByCtxProject(String projectName);
    int countExecutionReports(Map execQueryMap);
    int countExecutionReportsWithTransaction(Map execQueryMap, boolean isJobs, Long scheduledExecutionId, Integer isolationLevel);
    int countAndSaveByStatus();
    Collection<String> getRunList(Map execQueryMap, Map filters, boolean isJobs, Long scheduledExecutionId);
    void deleteByCtxProject(String projectName);
    void deleteAllByExecutionId(Long executionId);
}
