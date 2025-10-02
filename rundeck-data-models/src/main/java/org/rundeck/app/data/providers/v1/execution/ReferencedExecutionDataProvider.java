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
package org.rundeck.app.data.providers.v1.execution;

import org.rundeck.app.data.model.v1.execution.RdReferencedExecution;
import org.rundeck.app.data.model.v1.job.JobData;
import org.rundeck.app.data.model.v1.job.JobDataSummary;
import org.rundeck.app.data.providers.v1.DataProvider;

import java.util.List;

public interface ReferencedExecutionDataProvider extends DataProvider {
    Long updateOrCreateReference(Long refId, String jobUuid, Long execId, String status);
    RdReferencedExecution findByJobUuid(String jobUuid);
    List<JobDataSummary> parentJobSummaries(String jobUuid, int max);
    List<String> executionProjectList(String jobUuid, int max);
    List<String> getExecutionUuidsByJobUuid(String jobUuid);
    int countByJobUuid(String jobUuid);
    int countByJobUuidAndStatus(String jobUuid, String status);
    void deleteByExecutionId(Long id);
    void deleteByJobUuid(String jobUuid);

    /**
     * Get executions id by job uuid
     * @param jobUuid job uuid
     * @return list of execution ids
     */
    List<Long> getExecutionsIdsByJobUuid(String jobUuid);
}
