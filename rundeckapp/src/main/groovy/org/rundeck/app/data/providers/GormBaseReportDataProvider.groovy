package org.rundeck.app.data.providers


import org.rundeck.app.data.model.v1.report.RdBaseReport
import org.rundeck.app.data.providers.v1.BaseReportDataProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.TransactionStatus
import rundeck.BaseReport
import rundeck.ExecReport
import rundeck.services.data.BaseReportDataService

class GormBaseReportDataProvider implements BaseReportDataProvider {
    @Autowired
    BaseReportDataService baseReportDataService
    @Override
    RdBaseReport get(Long id) {
        return BaseReport.get(id)
    }

    @Override
    List<RdBaseReport> findAllByCtxProject(String projectName) {
        return BaseReport.findAllByCtxProject(projectName)
    }

    @Override
    int countByCtxProject(String projectName) {
        return BaseReport.countByCtxProject(projectName)
    }

    @Override
    void deleteByCtxProject(String projectName) {
        BaseReport.deleteByCtxProject(projectName)
    }

    @Override
    Map toMap(RdBaseReport rdBaseReport) {
        BaseReport baseReport = rdBaseReport as BaseReport
        return baseReport.toMap()

    }

    @Override
    void deleteWithTransaction(String projectName) {
        BaseReport.withTransaction { TransactionStatus status ->
            try {
                BaseReport.deleteByCtxProject(projectName)
                ExecReport.deleteByCtxProject(projectName)
            } catch (Exception e){
                status.setRollbackOnly()
                throw e
            }
        }
    }

}
