package org.rundeck.app.data.model.v1.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rundeck.app.data.model.v1.report.RdExecReport;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveReportResponseImpl implements SaveReportResponse {
    RdExecReport report;
    Boolean isSaved;
    String errors;

}
