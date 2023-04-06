package org.rundeck.app.data.providers.v1.execution;

import org.rundeck.app.data.model.v1.execution.RdReferencedExecution;
import org.rundeck.app.data.model.v1.job.JobData;
import org.rundeck.app.data.providers.v1.DataProvider;

import java.util.List;

public interface ReferencedExecutionDataProvider extends DataProvider {
    Long updateOrCreateReference(Long refId, Long seId, Long execId, String status);
    RdReferencedExecution findByScheduledExecutionId(Long seId);
    List<JobData> parentList(Long seId, int max);
    List executionProjectList(Long seId, int max);
    int countByScheduledExecution(Long seId);
    int countByScheduledExecutionAndStatus(Long seId, String status);
    void deleteByExecutionId(Long id);
    void deleteByScheduledExecutionId(Long id);
}
