package org.rundeck.app.data.providers

import spock.lang.Specification

class JobStatsAccumulatorSpec extends Specification {

    GormJobStatsDataProvider provider
    JobStatsAccumulator accumulator

    def setup() {
        provider = Mock(GormJobStatsDataProvider)
        accumulator = new JobStatsAccumulator(jobStatsDataProvider: provider)
    }

    def "flush saves each job once, in arrival order"() {
        given:
            def jobA = UUID.randomUUID().toString()
            def jobB = UUID.randomUUID().toString()
            accumulator.add(jobA, execution(1L))
            accumulator.add(jobA, execution(2L))
            accumulator.add(jobB, execution(3L))

        when:
            accumulator.flush()

        then:
            1 * provider.updateJobStats(jobA, { it*.executionId == [1L, 2L] }) >> true
            1 * provider.updateJobStats(jobB, { it*.executionId == [3L] }) >> true
    }

    def "executions that arrive while a job is being saved wait for the next flush"() {
        given:
            def jobUuid = UUID.randomUUID().toString()
            accumulator.add(jobUuid, execution(1L))

        when:
            accumulator.flush()

        then:
            1 * provider.updateJobStats(jobUuid, { it*.executionId == [1L] }) >> {
                accumulator.add(jobUuid, execution(2L))
                accumulator.flush()
                true
            }
            0 * provider.updateJobStats(jobUuid, { it*.executionId == [2L] })

        when:
            accumulator.flush()

        then:
            1 * provider.updateJobStats(jobUuid, { it*.executionId == [2L] }) >> true
    }

    def "a failed save is retried ahead of executions that arrived during the save"() {
        given:
            def jobUuid = UUID.randomUUID().toString()
            accumulator.add(jobUuid, execution(1L))

        when:
            accumulator.flush()

        then:
            1 * provider.updateJobStats(jobUuid, { it*.executionId == [1L] }) >> {
                accumulator.add(jobUuid, execution(2L))
                false
            }

        when:
            accumulator.flush()

        then:
            1 * provider.updateJobStats(jobUuid, { it*.executionId == [1L, 2L] }) >> true
    }

    private static JobExecutionResult execution(Long id) {
        new JobExecutionResult(id, 1000L, "succeeded", new Date())
    }
}
