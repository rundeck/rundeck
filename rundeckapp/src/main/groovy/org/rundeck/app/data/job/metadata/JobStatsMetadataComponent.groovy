package org.rundeck.app.data.job.metadata

import groovy.transform.CompileStatic
import org.rundeck.app.components.jobs.ComponentMeta
import org.rundeck.app.components.jobs.JobMetadataComponent
import org.rundeck.app.components.jobs.stats.JobStatsProvider
import org.rundeck.app.data.model.v1.job.JobDataSummary
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

@CompileStatic
class JobStatsMetadataComponent implements JobMetadataComponent {
    static final String STATS_NAME = 'stats'
    @Autowired
    @Qualifier('scheduledExecutionService')
    JobStatsProvider jobStatsProvider

    @Override
    Set<String> getAvailableMetadataNames() {
        return [STATS_NAME].toSet()
    }

    @Override
    Optional<List<ComponentMeta>> getMetadataForJob(final String id, String project, final Set<String> names) {
        if (!names.contains(STATS_NAME) && !names.contains('*')) {
            return Optional.empty()
        }
        return Optional.of([ComponentMeta.with(STATS_NAME, getJobStats(id, project))])
    }

    @Override
    Optional<List<ComponentMeta>> getMetadataForJob(final JobDataSummary job, final Set<String> names) {
        if (!names.contains(STATS_NAME) && !names.contains('*')) {
            return Optional.empty()
        }
        return Optional.of([ComponentMeta.with(STATS_NAME, getJobStats(job.uuid, job.project))])
    }

    Map<String, Object> getJobStats(String uuid, String project) {
        def stats = jobStatsProvider.calculateJobStats(uuid)
        Map<String, Object> result = new HashMap<>(
            [
                averageDuration: stats?.averageDuration ?: 0,
                executionCount : stats?.execCount ?: 0,
                successRate    : stats?.successRate,
            ]
        )
        return result
    }
}
