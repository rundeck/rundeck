package org.rundeck.app.data.providers.v1.execution;

import org.rundeck.app.data.providers.v1.DataProvider;

public interface ScheduledExecutionStatsDataProvider extends DataProvider {

    void createScheduledExecutionStats(Long seId);
    void deleteByScheduledExecutionId(Long seId);
    Boolean updateScheduledExecutionStats(Long seId, Long eId, long time);
}
