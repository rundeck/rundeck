package org.rundeck.app.data.providers;

import org.rundeck.app.data.model.v1.report.RdExecReport;
import org.rundeck.app.data.providers.v1.ExecReportDataProvider;
import rundeck.ExecReport;

class GormExecReportDataProvider implements ExecReportDataProvider {
    @Override
    RdExecReport get(Long id){
        return ExecReport.get(id)
    }
    @Override
    List<RdExecReport> findAllByStatus(String status) {
        return ExecReport.findAllByStatus(status)
    }
    @Override
    List<RdExecReport> findAllByExecutionId(Long id) {
        return ExecReport.findAllByExecutionId(id)
    }
    @Override
    List<RdExecReport> findAllByCtxProjectAndExecutionIdInList(String projectName, List<Long> execIds) {
        return ExecReport.findAllByCtxProjectAndExecutionIdInList(projectName, execIds)
    }

    @Override
    int countByCtxProject(String projectName) {
        return ExecReport.countByCtxProject(projectName)
    }

    @Override
    void deleteByCtxProject(String projectName) {
        ExecReport.deleteByCtxProject(projectName)
    }
}
