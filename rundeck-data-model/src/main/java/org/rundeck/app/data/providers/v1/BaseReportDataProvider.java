package org.rundeck.app.data.providers.v1;

import org.rundeck.app.data.model.v1.report.RdBaseReport;

import java.util.List;

public interface BaseReportDataProvider extends DataProvider{
    RdBaseReport get(Long id);
    List<RdBaseReport> findAllByCtxProject(String projectName);
    int countByCtxProject(String projectName);
    void deleteByCtxProject(String projectName);

}
