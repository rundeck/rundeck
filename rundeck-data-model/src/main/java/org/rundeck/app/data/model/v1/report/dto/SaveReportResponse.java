package org.rundeck.app.data.model.v1.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rundeck.app.data.model.v1.report.RdBaseReport;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveReportResponse {
    RdBaseReport report;
    boolean isSaved;
}
