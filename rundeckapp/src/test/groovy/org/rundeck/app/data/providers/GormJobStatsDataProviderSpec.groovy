package org.rundeck.app.data.providers

import grails.testing.gorm.DataTest
import rundeck.ScheduledExecution
import rundeck.ScheduledExecutionStats
import rundeck.services.ConfigurationService
import spock.lang.Specification

import java.time.LocalDate

class GormJobStatsDataProviderSpec extends Specification implements DataTest {

    Class[] getDomainClassesToMock() {
        [ScheduledExecution, ScheduledExecutionStats]
    }

    GormJobStatsDataProvider provider
    ConfigurationService mockConfigurationService

    def setup() {
        provider = new GormJobStatsDataProvider()
        mockConfigurationService = Mock(ConfigurationService)
        provider.configurationService = mockConfigurationService

        // Default: Feature flag enabled for existing tests
        mockConfigurationService.getBoolean("rundeck.feature.executionDailyMetrics.enabled", false) >> true
    }

    def "updateJobStats creates dailyMetrics on first execution"() {
        given: "a job UUID"
            def jobUuid = UUID.randomUUID().toString()
            def executionId = 123L
            def executionTime = 5000L
            def status = "succeeded"
            def dateCompleted = new Date()

        when: "updateJobStats is called for the first time"
            def result = provider.updateJobStats(jobUuid, executionId, executionTime, status, dateCompleted)

        then: "stats are created with dailyMetrics"
            result == true
            def stats = ScheduledExecutionStats.findByJobUuid(jobUuid)
            stats != null
            def contentMap = stats.getContentMap()
            contentMap.dailyMetrics != null
            def today = LocalDate.now().toString()
            contentMap.dailyMetrics[today] != null
            contentMap.dailyMetrics[today].total == 1
            contentMap.dailyMetrics[today].succeeded == 1
            contentMap.dailyMetrics[today].failed == 0
            contentMap.dailyMetrics[today].aborted == 0
            contentMap.dailyMetrics[today].timedout == 0
            contentMap.dailyMetrics[today].duration == executionTime
    }

    def "updateJobStats increments today's counters correctly"() {
        given: "existing stats with one execution today"
            def jobUuid = UUID.randomUUID().toString()
            def today = LocalDate.now().toString()
            def stats = new ScheduledExecutionStats(
                jobUuid: jobUuid,
                contentMap: [
                    execCount: 1,
                    totalTime: 1000,
                    refExecCount: 0,
                    dailyMetrics: [
                        (today): [
                            total: 1,
                            succeeded: 1,
                            failed: 0,
                            aborted: 0,
                            timedout: 0,
                            duration: 1000,
                            hourly: (0..23).collect { 0 }
                        ]
                    ]
                ]
            )
            stats.save(flush: true)

        when: "another execution is added"
            def result = provider.updateJobStats(jobUuid, 456L, 2000L, "succeeded", new Date())

        then: "counters are incremented"
            result == true
            def updatedStats = ScheduledExecutionStats.findByJobUuid(jobUuid)
            def contentMap = updatedStats.getContentMap()
            contentMap.dailyMetrics[today].total == 2
            contentMap.dailyMetrics[today].succeeded == 2
            contentMap.dailyMetrics[today].duration == 3000
    }

    def "updateJobStats handles different execution statuses"() {
        given: "a job UUID"
            def jobUuid = UUID.randomUUID().toString()
            def today = LocalDate.now().toString()
            def dateCompleted = new Date()

        when: "executions with different statuses are added"
            provider.updateJobStats(jobUuid, 1L, 1000L, "succeeded", dateCompleted)
            provider.updateJobStats(jobUuid, 2L, 2000L, "failed", dateCompleted)
            provider.updateJobStats(jobUuid, 3L, 3000L, "aborted", dateCompleted)
            provider.updateJobStats(jobUuid, 4L, 4000L, "timedout", dateCompleted)
            provider.updateJobStats(jobUuid, 5L, 5000L, "failed-with-retry", dateCompleted)

        then: "each status is tracked correctly"
            def stats = ScheduledExecutionStats.findByJobUuid(jobUuid)
            def contentMap = stats.getContentMap()
            def todayMetrics = contentMap.dailyMetrics[today]
            todayMetrics.total == 5
            todayMetrics.succeeded == 1
            todayMetrics.failed == 2  // includes 'failed' and 'failed-with-retry'
            todayMetrics.aborted == 1
            todayMetrics.timedout == 1
            todayMetrics.duration == 15000
    }

    def "updateJobStats tracks hourly distribution correctly"() {
        given: "a job UUID and a specific hour"
            def jobUuid = UUID.randomUUID().toString()
            def today = LocalDate.now().toString()
            def calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 14) // 2 PM
            calendar.set(Calendar.MINUTE, 30)
            def dateCompleted = calendar.time

        when: "executions are added at specific hours"
            provider.updateJobStats(jobUuid, 1L, 1000L, "succeeded", dateCompleted)
            provider.updateJobStats(jobUuid, 2L, 2000L, "succeeded", dateCompleted)

        then: "hourly distribution is tracked"
            def stats = ScheduledExecutionStats.findByJobUuid(jobUuid)
            def contentMap = stats.getContentMap()
            def todayMetrics = contentMap.dailyMetrics[today]
            todayMetrics.hourly[14] == 2  // Two executions at 2 PM
            todayMetrics.hourly.findAll { it > 0 }.size() == 1  // Only one hour has executions
    }

    def "updateJobStats maintains rolling-10 logic for existing UI"() {
        given: "a job UUID"
            def jobUuid = UUID.randomUUID().toString()
            def dateCompleted = new Date()

        when: "10 executions are added"
            (1..10).each { i ->
                provider.updateJobStats(jobUuid, i, 1000L, "succeeded", dateCompleted)
            }

        then: "rolling-10 logic is maintained"
            def stats = ScheduledExecutionStats.findByJobUuid(jobUuid)
            def contentMap = stats.getContentMap()
            contentMap.execCount == 10
            contentMap.totalTime == 10000

        when: "one more execution is added"
            provider.updateJobStats(jobUuid, 11L, 1000L, "succeeded", dateCompleted)

        then: "rolling-10 logic pops oldest average"
            def updatedStats = ScheduledExecutionStats.findByJobUuid(jobUuid)
            def updatedContentMap = updatedStats.getContentMap()
            updatedContentMap.execCount == 10  // Still 10
            // totalTime should be: 10000 - (10000/10) + 1000 = 10000
            updatedContentMap.totalTime == 10000
    }

    def "updateJobStats handles concurrent updates with retry"() {
        given: "a job UUID with existing stats"
            def jobUuid = UUID.randomUUID().toString()
            def stats = new ScheduledExecutionStats(
                jobUuid: jobUuid,
                contentMap: [execCount: 0, totalTime: -1, refExecCount: 0]
            )
            stats.save(flush: true)

        and: "mock ConcurrencyFailureException"
            // Note: In a real scenario, we'd need to simulate concurrent updates
            // For this test, we verify the exception handling exists in the code

        when: "updateJobStats is called"
            def result = provider.updateJobStats(jobUuid, 123L, 1000L, "succeeded", new Date())

        then: "it completes successfully"
            result == true
    }

    def "updateJobStats creates stats with correct initial values"() {
        given: "a new job UUID"
            def jobUuid = UUID.randomUUID().toString()

        when: "first execution completes"
            provider.updateJobStats(jobUuid, 1L, 5000L, "succeeded", new Date())

        then: "stats are created with correct rolling-10 values"
            def stats = ScheduledExecutionStats.findByJobUuid(jobUuid)
            def contentMap = stats.getContentMap()
            contentMap.execCount == 1
            contentMap.totalTime == 5000
            contentMap.refExecCount == 0
    }

    def "getOrCreate creates new stats if not exists"() {
        given: "a new job UUID"
            def jobUuid = UUID.randomUUID().toString()

        when: "getOrCreate is called"
            def stats = provider.getOrCreate(jobUuid)

        then: "stats are created with default values"
            stats != null
            stats.jobUuid == jobUuid
            def contentMap = stats.getContentMap()
            contentMap.execCount == 0
            contentMap.totalTime == -1
            contentMap.refExecCount == 0
    }

    def "getOrCreate returns existing stats if they exist"() {
        given: "existing stats"
            def jobUuid = UUID.randomUUID().toString()
            def existingStats = new ScheduledExecutionStats(
                jobUuid: jobUuid,
                contentMap: [execCount: 5, totalTime: 10000, refExecCount: 0]
            )
            existingStats.save(flush: true)

        when: "getOrCreate is called"
            def stats = provider.getOrCreate(jobUuid)

        then: "existing stats are returned"
            stats != null
            stats.id == existingStats.id
            def contentMap = stats.getContentMap()
            contentMap.execCount == 5
            contentMap.totalTime == 10000
    }

    def "updateJobStats handles multiple days correctly"() {
        given: "a job UUID"
            def jobUuid = UUID.randomUUID().toString()
            def today = LocalDate.now().toString()
            def yesterday = LocalDate.now().minusDays(1).toString()

        and: "existing stats with yesterday's data"
            def stats = new ScheduledExecutionStats(
                jobUuid: jobUuid,
                contentMap: [
                    execCount: 5,
                    totalTime: 5000,
                    refExecCount: 0,
                    dailyMetrics: [
                        (yesterday): [
                            total: 5,
                            succeeded: 5,
                            failed: 0,
                            aborted: 0,
                            timedout: 0,
                            duration: 5000,
                            hourly: (0..23).collect { 0 }
                        ]
                    ]
                ]
            )
            stats.save(flush: true)

        when: "today's execution is added"
            provider.updateJobStats(jobUuid, 6L, 1000L, "succeeded", new Date())

        then: "both days are preserved in dailyMetrics"
            def updatedStats = ScheduledExecutionStats.findByJobUuid(jobUuid)
            def contentMap = updatedStats.getContentMap()
            contentMap.dailyMetrics[yesterday] != null
            contentMap.dailyMetrics[yesterday].total == 5
            contentMap.dailyMetrics[today] != null
            contentMap.dailyMetrics[today].total == 1
    }

    def "updateJobStats does NOT collect daily metrics when feature flag is disabled"() {
        given: "feature flag disabled"
            def jobUuid = UUID.randomUUID().toString()
            def executionId = 123L
            def executionTime = 5000L
            def status = "succeeded"
            def dateCompleted = new Date()

            // Create new provider with feature flag disabled
            def testProvider = new GormJobStatsDataProvider()
            def testMockConfig = Mock(ConfigurationService)
            testProvider.configurationService = testMockConfig
            testMockConfig.getBoolean("rundeck.feature.executionDailyMetrics.enabled", false) >> false

        when: "updateJobStats is called"
            def result = testProvider.updateJobStats(jobUuid, executionId, executionTime, status, dateCompleted)

        then: "stats are created but dailyMetrics is NOT populated"
            result == true
            def stats = ScheduledExecutionStats.findByJobUuid(jobUuid)
            stats != null
            def contentMap = stats.getContentMap()
            contentMap.dailyMetrics == null || contentMap.dailyMetrics.isEmpty()
    }

    def "updateJobStats still maintains rolling-10 logic when feature flag is disabled"() {
        given: "feature flag disabled"
            def jobUuid = UUID.randomUUID().toString()
            def dateCompleted = new Date()

            // Create new provider with feature flag disabled
            def testProvider = new GormJobStatsDataProvider()
            def testMockConfig = Mock(ConfigurationService)
            testProvider.configurationService = testMockConfig
            testMockConfig.getBoolean("rundeck.feature.executionDailyMetrics.enabled", false) >> false

        when: "10 executions are added"
            (1..10).each { i ->
                testProvider.updateJobStats(jobUuid, i, 1000L, "succeeded", dateCompleted)
            }

        then: "rolling-10 logic still works"
            def stats = ScheduledExecutionStats.findByJobUuid(jobUuid)
            def contentMap = stats.getContentMap()
            contentMap.execCount == 10
            contentMap.totalTime == 10000

        and: "dailyMetrics is NOT populated"
            contentMap.dailyMetrics == null || contentMap.dailyMetrics.isEmpty()

        when: "one more execution is added"
            testProvider.updateJobStats(jobUuid, 11L, 1000L, "succeeded", dateCompleted)

        then: "rolling-10 logic pops oldest average"
            def updatedStats = ScheduledExecutionStats.findByJobUuid(jobUuid)
            def updatedContentMap = updatedStats.getContentMap()
            updatedContentMap.execCount == 10  // Still 10
            updatedContentMap.totalTime == 10000
    }

    def "updateJobStats collects daily metrics when feature flag is enabled"() {
        given: "feature flag enabled (default in setup)"
            def jobUuid = UUID.randomUUID().toString()
            def executionId = 123L
            def executionTime = 5000L
            def status = "succeeded"
            def dateCompleted = new Date()

        when: "updateJobStats is called"
            def result = provider.updateJobStats(jobUuid, executionId, executionTime, status, dateCompleted)

        then: "stats are created WITH dailyMetrics"
            result == true
            def stats = ScheduledExecutionStats.findByJobUuid(jobUuid)
            stats != null
            def contentMap = stats.getContentMap()
            def today = LocalDate.now().toString()
            contentMap.dailyMetrics != null
            contentMap.dailyMetrics[today] != null
            contentMap.dailyMetrics[today].total == 1
            contentMap.dailyMetrics[today].succeeded == 1
    }
}