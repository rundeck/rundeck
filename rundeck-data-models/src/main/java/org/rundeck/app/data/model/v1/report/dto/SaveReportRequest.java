/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rundeck.app.data.model.v1.report.dto;

import java.util.Date;

public interface SaveReportRequest {
    Long getExecutionId();
    Date getDateStarted();
    String getJobId();
    String getReportId();
    Boolean getAdhocExecution();
    String getSucceededNodeList();
    String getFailedNodeList();
    String getFilterApplied();
    String getProject();
    String getAbortedByUser();
    String getAuthor();
    String getTitle();
    String getStatus();
    String getNode();
    String getMessage();
    Date getDateCompleted();
    String getAdhocScript();
    String getTags();
    String getJobUuid();
    String getExecutionUuid();
}
