package org.rundeck.app.data.providers.v1.logstorage;

import org.rundeck.app.data.model.v1.logstorage.RdLogFileStorageRequest;
import org.rundeck.app.data.providers.v1.logstorage.dto.CompletedStatusLogFileStorageResponse;
import org.rundeck.app.data.providers.v1.logstorage.dto.DuplicateLogFileStorageResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LogFileStorageRequestProvider {
    RdLogFileStorageRequest get(Long id);
    RdLogFileStorageRequest retryLoad(Long requestId, Long retryMaxMs);
    RdLogFileStorageRequest build(String pluginName, String filetype, Boolean completed, Long executionId);
    RdLogFileStorageRequest create(RdLogFileStorageRequest data) throws Exception;
    RdLogFileStorageRequest update(Long id, RdLogFileStorageRequest data) throws Exception;
    RdLogFileStorageRequest updateFiletypeAndCompleted(Long id, String filetype, Boolean completed) throws Exception;
    RdLogFileStorageRequest updateCompleted(Long id, Boolean completed) throws Exception;
    void delete(Long id);
    RdLogFileStorageRequest findByExecutionId(Long executionId);
    List<DuplicateLogFileStorageResponse> findDuplicates();
    List<CompletedStatusLogFileStorageResponse> listCompletedStatusByExecutionId(Long executionId);
    List<RdLogFileStorageRequest> listByIncompleteAndClusterNodeNotInExecIds(String serverUUID, Set<Long> execIds, Map paging);
    Long countByIncompleteAndClusterNodeNotInExecIds(String serverUUID, Set<Long> execIds);

}
