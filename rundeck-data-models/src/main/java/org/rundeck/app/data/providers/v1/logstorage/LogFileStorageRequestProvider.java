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
package org.rundeck.app.data.providers.v1.logstorage;

import org.rundeck.app.data.model.v1.logstorage.LogFileStorageRequestData;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LogFileStorageRequestProvider {
    LogFileStorageRequestData get(Long id);
    LogFileStorageRequestData retryLoad(Long requestId, Long retryMaxMs);
    LogFileStorageRequestData build(String pluginName, String filetype, Boolean completed, String executionUuid);
    LogFileStorageRequestData create(LogFileStorageRequestData data) throws Exception;
    LogFileStorageRequestData update(String executionUuid, LogFileStorageRequestData data) throws Exception;
    LogFileStorageRequestData updateFiletypeAndCompleted(String executionUuid, String filetype, Boolean completed) throws Exception;
    LogFileStorageRequestData updateCompleted(String executionUuid, Boolean completed) throws Exception;
    void delete(String executionUuid);
    LogFileStorageRequestData findByExecutionUuid(String executionUuid);
    List<LogFileStorageRequestData> listByIncompleteAndClusterNodeNotInExecUuids(String serverUUID, Set<String> execUuids, Map paging);
    Long countByIncompleteAndClusterNodeNotInExecUuids(String serverUUID, Set<String> execUuids);

}
