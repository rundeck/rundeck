package rundeck.data.report;

import org.rundeck.app.data.model.v1.report.RdExecReport;
import org.rundeck.app.data.model.v1.report.dto.SaveReportResponse;

class SaveReportResponseImpl implements SaveReportResponse {
    RdExecReport report;
    Boolean isSaved;
    String errors;
}
