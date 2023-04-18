package org.rundeck.app.data.providers.v1.execution;

import org.rundeck.app.data.model.v1.execution.RdJobStats;
import org.rundeck.app.data.providers.v1.DataProvider;

public interface JobStatsDataProvider extends DataProvider {

    RdJobStats createJobStats(String jobUuid);
    void deleteByJobUuid(String jobUuid);
    Boolean updateJobStats(String jobUuid, Long eId, long time);
    Boolean updateJobRefStats(String jobUuid, long time);
}
