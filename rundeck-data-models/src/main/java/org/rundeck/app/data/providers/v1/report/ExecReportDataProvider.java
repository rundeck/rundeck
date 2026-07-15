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
package org.rundeck.app.data.providers.v1.report;

import org.rundeck.app.data.model.v1.query.RdExecQuery;
import org.rundeck.app.data.model.v1.report.RdExecReport;
import org.rundeck.app.data.model.v1.report.dto.SaveReportRequest;
import org.rundeck.app.data.model.v1.report.dto.SaveReportResponse;
import org.rundeck.app.data.providers.v1.DataProvider;

import java.util.Collection;
import java.util.List;

public interface ExecReportDataProvider extends DataProvider {
    RdExecReport get(Long id);
    SaveReportResponse saveReport(SaveReportRequest saveReportRequest);
    List<RdExecReport> findAllByProject(String projectName);
    List<RdExecReport> findAllByStatus(String status);
    List<RdExecReport> findAllByProjectAndExecutionUuidInList(String projectName, List<String> execUuids);
    int countByProject(String projectName);
    int countExecutionReports(RdExecQuery execQuery);
    int countExecutionReportsWithTransaction(RdExecQuery execQuery, boolean isJobs, String jobUuid);
    int countAndSaveByStatus();
    @Deprecated
    default List<RdExecReport> getExecutionReports(RdExecQuery execQuery, boolean isJobs, String jobUuid, List<String> executionUuids){
        return null;
    }
    void deleteByProject(String projectName);
    void deleteWithTransaction(String projectName);
    void deleteAllByExecutionUuid(String executionUuid);

    /**
     * Get execution reports with transaction and list of executions ids
     * This method can be used when you already have the executions ids
     * @param execQuery
     * @param isJobs
     * @param jobUuid
     * @param executionIds
     * @return List<RdExecReport>
     */
    default List<RdExecReport> getExecutionReportsByExecsIds(RdExecQuery execQuery, boolean isJobs, String jobUuid, List<Long> executionIds){
        return null;
    }

    /**
     * Count execution reports with transaction and list of executions ids
     * This method can be used when you already have the executions ids
     * @param query query
     * @param isJobs is job
     * @param jobUuid scheduled execution uuid
     * @param execsId list of execution ids
     * @return count
     */
    default int countExecutionReportsWithTransaction(RdExecQuery query, boolean isJobs, String jobUuid, List<Long> execsId){
        return 0;
    }
}
