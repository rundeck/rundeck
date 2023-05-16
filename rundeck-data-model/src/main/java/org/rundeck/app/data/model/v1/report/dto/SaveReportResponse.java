package org.rundeck.app.data.model.v1.report.dto;

import org.rundeck.app.data.model.v1.report.RdExecReport;

public interface SaveReportResponse {
    RdExecReport getReport();
    Boolean getIsSaved();
    String getErrors();
}
