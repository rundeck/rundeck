package org.rundeck.app.data.providers.v1.execution;

import org.rundeck.app.data.model.v1.execution.RdReferencedExecution;
import org.rundeck.app.data.model.v1.job.JobData;
import org.rundeck.app.data.providers.v1.DataProvider;

import java.util.List;

public interface ReferencedExecutionDataProvider extends DataProvider {
    Long updateOrCreateReference(Long refId, Long jobId, Long execId, String status);
    RdReferencedExecution findByJobId(Long jobId);
    List<Long> parentList(Long jobId, int max);
    List executionProjectList(Long jobId, int max);
    int countByJobId(Long jobId);
    int countByJobIdAndStatus(Long jobId, String status);
    void deleteByExecutionId(Long id);
    void deleteByJobId(Long id);
}
