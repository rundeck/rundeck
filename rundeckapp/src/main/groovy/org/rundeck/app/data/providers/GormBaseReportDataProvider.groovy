package org.rundeck.app.data.providers

import org.rundeck.app.data.model.v1.report.RdBaseReport
import org.rundeck.app.data.providers.v1.BaseReportDataProvider
import rundeck.BaseReport

class GormBaseReportDataProvider implements BaseReportDataProvider {
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
}
