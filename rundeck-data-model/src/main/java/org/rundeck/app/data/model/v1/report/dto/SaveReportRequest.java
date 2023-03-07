package org.rundeck.app.data.model.v1.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveReportRequest {
    Long executionId;
    Date dateStarted;
    String jcJobId;
    String reportId;
    boolean adhocExecution;
    String succeededNodeList;
    String failedNodeList;
    String filterApplied;
    String ctxProject;
    String abortedByUser;
    String author;
    String title;
    String status;
    String node;
    String message;
    Date dateCompleted;
    Date rundeckEpochDateStarted;
    Date epochDateStarted;
    Date rundeckEpochDateEnded;
    Date epochDateEnded;
    String actionType;
    String adhocScript;
    String tags;
}
