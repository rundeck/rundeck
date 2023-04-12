package org.rundeck.app.data.providers

import groovy.util.logging.Slf4j
import org.hibernate.StaleObjectStateException
import org.rundeck.app.data.providers.v1.execution.ScheduledExecutionStatsDataProvider
import org.springframework.dao.DuplicateKeyException
import rundeck.ScheduledExecution
import rundeck.ScheduledExecutionStats

@Slf4j
class GormScheduledExecutionStats implements ScheduledExecutionStatsDataProvider{
    @Override
    void createScheduledExecutionStats(Long seId) {
        def se = ScheduledExecution.findById(seId)
        def stats = ScheduledExecutionStats.findAllBySe(se)
        if(!stats) {
            new ScheduledExecutionStats(se: se)
                    .save(flush: true)
        }
    }

    @Override
    void deleteByScheduledExecutionId(Long seId) {
        def se = ScheduledExecution.findById(seId)
        ScheduledExecutionStats.findAllBySe(se).each {stats->
            stats.delete()
        }
    }

    @Override
    Boolean updateScheduledExecutionStats(Long seId, Long eId, long time) {
        def success = false
        try {
            ScheduledExecutionStats.withTransaction {
                def scheduledExecution = ScheduledExecution.get(seId)
                def seStats = scheduledExecution.getStats(true)

                def statsMap = seStats.getContentMap()
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
                seStats.setContentMap(statsMap)

                if (seStats.validate()) {
                    if (seStats.save(flush: true)) {
                        log.info("updated scheduled Execution Stats")
                    } else {
                        seStats.errors.allErrors.each { log.warn(it.defaultMessage) }
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
}
