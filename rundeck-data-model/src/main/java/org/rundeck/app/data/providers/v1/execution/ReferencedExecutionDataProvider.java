package org.rundeck.app.data.providers.v1.execution;

import org.rundeck.app.data.model.v1.execution.RdReferencedExecution;
import org.rundeck.app.data.model.v1.job.JobData;
import org.rundeck.app.data.providers.v1.DataProvider;

import java.util.List;

public interface ReferencedExecutionDataProvider extends DataProvider {
    Long updateOrCreateReference(Long refId, String jobUuid, Long execId, String status);
    RdReferencedExecution findByJobUuid(String jobUuid);
    List<Long> parentList(String jobUuid, int max);
    List executionProjectList(String jobUuid, int max);
    int countByJobUuid(String jobUuid);
    int countByJobUuidAndStatus(String jobUuid, String status);
    void deleteByExecutionId(Long id);
    void deleteByJobUuid(String jobUuid);
}
