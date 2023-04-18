package org.rundeck.app.data.providers

import groovy.util.logging.Slf4j
import org.hibernate.StaleObjectStateException
import org.rundeck.app.data.model.v1.execution.RdJobStats
import org.rundeck.app.data.providers.v1.execution.JobStatsDataProvider
import org.springframework.dao.DuplicateKeyException
import rundeck.ScheduledExecution
import rundeck.ScheduledExecutionStats

@Slf4j
class GormJobStatsDataProvider implements JobStatsDataProvider{
    @Override
    RdJobStats createJobStats(String jobUuid) {
        def stats = ScheduledExecutionStats.findByJobUuid(jobUuid)
        if(!stats) {
            stats = new ScheduledExecutionStats(jobUuid: jobUuid, contentMap: [execCount: 0, totalTime: -1, refExecCount: 0])
            stats.save(flsuh: true)
        }
        return stats
    }

    @Override
    void deleteByJobUuid(String jobUuid) {
        ScheduledExecutionStats.findAllByJobUuid(jobUuid).each { stats->
            stats.delete()
        }
    }

    @Override
    Boolean updateJobStats(String jobUuid, Long eId, long time) {
        def success = false
        try {
            ScheduledExecutionStats.withTransaction {
                def stats = ScheduledExecutionStats.getOrCreate(jobUuid)
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
                stats.setContentMap(statsMap)

                if (stats.validate()) {
                    if (stats.save(flush: true)) {
                        log.info("updated scheduled Execution Stats")
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
            def stats = ScheduledExecutionStats.getOrCreate(jobUuid)
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
}
