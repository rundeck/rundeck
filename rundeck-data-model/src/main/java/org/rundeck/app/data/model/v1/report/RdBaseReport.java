package org.rundeck.app.data.model.v1.report;

import java.util.Date;

public interface RdBaseReport {
    String getNode();
    String getTitle();
    String getStatus();
    String getActionType();
    String getCtxProject();
    String getCtxType();
    String getCtxName();
    String getMaprefUri();
    String getReportId();
    String getTags();
    String getAuthor();
    Date getDateStarted();
    Date getDateCompleted();
    String getMessage();
}
