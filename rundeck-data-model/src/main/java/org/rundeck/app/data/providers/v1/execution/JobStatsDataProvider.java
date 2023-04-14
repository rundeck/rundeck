package org.rundeck.app.data.providers.v1.execution;

import org.rundeck.app.data.providers.v1.DataProvider;

public interface JobStatsDataProvider extends DataProvider {

    void createJobStats(Long jobId);
    void deleteByJobId(Long jobId);
    Boolean updateJobStats(Long jobId, Long eId, long time);
}
