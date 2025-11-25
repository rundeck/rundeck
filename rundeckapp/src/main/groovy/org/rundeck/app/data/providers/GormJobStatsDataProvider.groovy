package org.rundeck.app.data.providers

import groovy.util.logging.Slf4j
import org.hibernate.StaleObjectStateException
import org.rundeck.app.data.model.v1.execution.RdJobStats
import org.rundeck.app.data.model.v1.execution.dto.StatsContent
import org.rundeck.app.data.model.v1.execution.dto.StatsContentImpl
import org.rundeck.app.data.providers.v1.execution.JobStatsDataProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import rundeck.ScheduledExecution
import rundeck.ScheduledExecutionStats
import rundeck.services.ConfigurationService

import java.time.LocalDate

@Slf4j
class GormJobStatsDataProvider implements JobStatsDataProvider{
    @Autowired
    ConfigurationService configurationService

    @Override
    RdJobStats createJobStats(String jobUuid) {
        def stats = getOrCreate(jobUuid)
        return stats
    }

    @Override
    void deleteByJobUuid(String jobUuid) {
        ScheduledExecutionStats.findAllByJobUuid(jobUuid).each { stats->
            stats.delete()
        }
    }

    @Override
    Boolean updateJobStats(String jobUuid, Long eId, long time, String status, Date dateCompleted) {
        def success = false
        try {
            ScheduledExecutionStats.withTransaction {
                def stats = getOrCreate(jobUuid)
                def statsMap = stats.getContentMap()

                // === EXISTING LOGIC (Rolling-10 for UI) ===
                if (null == statsMap.execCount || 0 == statsMap.execCount || null == statsMap.totalTime || 0 == statsMap.totalTime) {
                    statsMap.execCount = 1
                    statsMap.totalTime = time
                } else if (statsMap.execCount > 0 && statsMap.execCount < 10) {
                    statsMap.execCount++
                    statsMap.totalTime += time
                } else if (statsMap.execCount >= 10) {
                    def popTime = statsMap.totalTime.intdiv(statsMap.execCount)
                    statsMap.totalTime -= popTime
                    statsMap.totalTime += time
                }

                // === NEW LOGIC (Date-keyed 7-day metrics) - RUN-3768 ===
                // Feature flag: rundeck.executionDailyMetrics.enabled
                def featureFlagEnabled = configurationService.getBoolean("executionDailyMetrics.enabled", false)
                log.info("[METRICS-API][RUN-3768] Feature flag check: configurationService.getBoolean('executionDailyMetrics.enabled', false) = ${featureFlagEnabled}")
                if (featureFlagEnabled) {
                    def today = LocalDate.now().toString()
                    def dailyMetrics = statsMap.dailyMetrics ?: [:]
                    def todayMetrics = dailyMetrics[today] ?: [
                        total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0,
                        duration: 0, hourly: (0..23).collect { 0 }
                    ]

                    // Increment today's metrics
                    todayMetrics.total++

                    switch(status) {
                        case 'succeeded':
                            todayMetrics.succeeded++
                            break
                        case 'failed':
                        case 'failed-with-retry':
                            todayMetrics.failed++
                            break
                        case 'aborted':
                            todayMetrics.aborted++
                            break
                        case 'timedout':
                            todayMetrics.timedout++
                            break
                    }
                    todayMetrics.duration += time

                    // Extract hour from execution completion time
                    if (dateCompleted) {
                        def hourOfDay = dateCompleted.toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .getHour()
                        def hourlyList = todayMetrics.hourly as List
                        hourlyList[hourOfDay] = (hourlyList[hourOfDay] ?: 0) + 1
                        todayMetrics.hourly = hourlyList
                    }

                    // Save back to dailyMetrics
                    dailyMetrics[today] = todayMetrics
                    statsMap.dailyMetrics = dailyMetrics

                    log.debug("Updated daily metrics for ${jobUuid}: today=${today}, total=${todayMetrics.total}, status=${status}")
                } else {
                    log.debug("Daily metrics collection disabled for ${jobUuid} (feature flag: rundeck.executionDailyMetrics.enabled=false)")
                }

                stats.setContentMap(statsMap)

                if (stats.validate()) {
                    if (stats.save(flush: true)) {
                        log.debug("Updated job stats for ${jobUuid}")
                    } else {
                        stats.errors.allErrors.each { log.warn(it.defaultMessage) }
                        log.warn("failed saving execution to history")
                    }
                    success = true
                }
            }
        } catch (org.springframework.dao.ConcurrencyFailureException e) {
            log.warn("Caught ConcurrencyFailureException, will retry updateScheduledExecStatistics for ${eId}")
        } catch (StaleObjectStateException e) {
            log.warn("Caught StaleObjectState, will retry updateScheduledExecStatistics for ${eId}")
        } catch (DuplicateKeyException ve) {
            log.warn("Caught DuplicateKeyException for migrated stats, will retry updateScheduledExecStatistics for ${eId}")
        }
        return success
    }

    @Override
    Boolean updateJobRefStats(String jobUuid, long time) {
        def success = false
        try {
            def stats = getOrCreate(jobUuid)
            def statsMap = stats.getContentMap()

            if (null == statsMap.execCount || 0 == statsMap.execCount || null == statsMap.totalTime || 0 == statsMap.totalTime) {
                statsMap.execCount = 1
                statsMap.totalTime = time
            } else if (statsMap.execCount > 0 && statsMap.execCount < 10) {
                statsMap.execCount++
                statsMap.totalTime += time
            } else if (statsMap.execCount >= 10) {
                def popTime = statsMap.totalTime.intdiv(statsMap.execCount)
                statsMap.totalTime -= popTime
                statsMap.totalTime += time
            }


            if (!statsMap.refExecCount) {
                statsMap.refExecCount = 1
            } else {
                statsMap.refExecCount++
            }
            stats.setContentMap(statsMap)

            if (stats.validate()) {
                if (stats.save(flush: true)) {
                    log.info("updated referenced Job Stats")
                } else {
                    stats.errors.allErrors.each { log.warn(it.defaultMessage) }
                    log.warn("failed saving referenced Job Stats")
                }
                success = true
            }
        } catch (org.springframework.dao.ConcurrencyFailureException e) {
            log.warn("Caught ConcurrencyFailureException, dismissed statistic for referenced Job")
        } catch (StaleObjectStateException e) {
            log.warn("Caught StaleObjectState, dismissed statistic for for referenced Job")
        } catch (DuplicateKeyException ve) {
            // Do something ...
            log.warn("Caught DuplicateKeyException for migrated stats, dismissed statistic for referenced Job")
        }
        return success
    }

    @Override
    StatsContent getStatsContent(String jobUuid) {
        def stats = ScheduledExecutionStats.findByJobUuid(jobUuid)
        if(!stats){
            def se = ScheduledExecution.findByUuid(jobUuid)
            if(se){
                stats = new ScheduledExecutionStats(jobUuid: jobUuid, contentMap: [execCount: se.execCount, totalTime: se.totalTime, refExecCount: se.refExecCount])
                stats.save()
                return new StatsContentImpl(se.execCount, se.totalTime, se.refExecCount)
            }
        }else{
            def contentMap = stats.getContentMap()
            if(contentMap){
                def statsContent = new StatsContentImpl()
                if(contentMap.execCount){
                    statsContent.execCount = contentMap.execCount
                }
                if(contentMap.totalTime){
                    statsContent.totalTime = contentMap.totalTime
                }
                if(contentMap.refExecCount){
                    statsContent.refExecCount = contentMap.refExecCount
                }
                return statsContent
            }
        }
        return null
    }

    ScheduledExecutionStats getOrCreate(String jobUuid) {
        def stats = ScheduledExecutionStats.findByJobUuid(jobUuid)
        if(!stats) {
            stats = new ScheduledExecutionStats(jobUuid: jobUuid, contentMap: [execCount: 0, totalTime: -1, refExecCount: 0])
            stats.save()
        }
        return stats
    }
}
