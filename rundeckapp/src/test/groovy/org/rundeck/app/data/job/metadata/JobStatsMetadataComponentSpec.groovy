package org.rundeck.app.data.job.metadata

import org.rundeck.app.components.jobs.ComponentMeta
import org.rundeck.app.components.jobs.stats.JobStatsProvider
import spock.lang.Specification

class JobStatsMetadataComponentSpec extends Specification {
    def "metadata names"() {
        def component = new JobStatsMetadataComponent()
        component.jobStatsProvider = Mock(JobStatsProvider)
        expect:
            component.getAvailableMetadataNames() == ['stats'].toSet()

    }

    def "get stats"() {
        def component = new JobStatsMetadataComponent()
        component.jobStatsProvider = Mock(JobStatsProvider)

        when:
            def result = component.getMetadataForJob('id', 'project', ['stats'].toSet())
        then:
            result.isPresent()
            ComponentMeta res = result.get()[0]
            res.data == [
                averageDuration: avg,
                successRate    : sRate,
                executionCount : count
            ]
            1 * component.jobStatsProvider.calculateJobStats('id') >> Mock(JobStatsProvider.JobStats) {
                _ * getSuccessRate() >> sRate
                _ * getAverageDuration() >> avg
                _ * getExecCount() >> count
            }

        where:
            sRate | avg | count
            1.0   | 100 | 1
            0     | 100 | 10
            -1.0  | 100 | 10
            -1.0  | 0   | 0
            -1.0  | -1   | 0
            -1.0  | 0   | -1
    }
}
