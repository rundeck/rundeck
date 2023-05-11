package org.rundeck.app.data.providers.v1.logstorage;

import org.rundeck.app.data.model.v1.logstorage.LogFileStorageRequestData;
import org.rundeck.app.data.providers.v1.logstorage.dto.CompletedStatusLogFileStorageResponse;
import org.rundeck.app.data.providers.v1.logstorage.dto.DuplicateLogFileStorageResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LogFileStorageRequestProvider {
    LogFileStorageRequestData get(Long id);
    LogFileStorageRequestData retryLoad(Long requestId, Long retryMaxMs);
    LogFileStorageRequestData build(String pluginName, String filetype, Boolean completed, Long executionId);
    LogFileStorageRequestData create(LogFileStorageRequestData data) throws Exception;
    LogFileStorageRequestData update(Long id, LogFileStorageRequestData data) throws Exception;
    LogFileStorageRequestData updateFiletypeAndCompleted(Long id, String filetype, Boolean completed) throws Exception;
    LogFileStorageRequestData updateCompleted(Long id, Boolean completed) throws Exception;
    void delete(Long id);
    LogFileStorageRequestData findByExecutionId(Long executionId);
    List<DuplicateLogFileStorageResponse> findDuplicates();
    List<CompletedStatusLogFileStorageResponse> listCompletedStatusByExecutionId(Long executionId);
    List<LogFileStorageRequestData> listByIncompleteAndClusterNodeNotInExecIds(String serverUUID, Set<Long> execIds, Map paging);
    Long countByIncompleteAndClusterNodeNotInExecIds(String serverUUID, Set<Long> execIds);

}
