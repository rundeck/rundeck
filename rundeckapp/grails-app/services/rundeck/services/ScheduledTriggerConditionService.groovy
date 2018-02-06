package rundeck.services

import com.dtolabs.rundeck.server.plugins.trigger.condition.ScheduleTriggerCondition
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.TriggerKey
import org.rundeck.core.triggers.Action
import org.rundeck.core.triggers.TriggerActionInvoker
import org.rundeck.core.triggers.TriggerCondition
import org.rundeck.core.triggers.TriggerConditionHandler
import rundeck.quartzjobs.RDTriggerConditionJob

/**
 * Handles trigger based schedules
 */
class ScheduledTriggerConditionService implements TriggerConditionHandler<RDTriggerContext> {

    static transactional = false

    Scheduler quartzScheduler
    def scheduledExecutionService
    def executionService

    @Override
    boolean onStartup() {
        true
    }

    @Override
    boolean handlesConditionChecks(TriggerCondition condition, RDTriggerContext contextInfo) {
        condition instanceof ScheduleTriggerCondition
    }


    @Override
    void registerConditionChecksForAction(String triggerId, RDTriggerContext contextInfo, TriggerCondition condition, Action action, TriggerActionInvoker service) {
        ScheduleTriggerCondition scheduled = (ScheduleTriggerCondition) condition

        log.error("schedule trigger using quartz for $triggerId, context $contextInfo, condition $condition, action $action, invoker $service")
        scheduleTrigger(triggerId, contextInfo, scheduled, action, service)

    }

    @Override
    void deregisterConditionChecksForAction(String triggerId, RDTriggerContext contextInfo, TriggerCondition condition, Action action, TriggerActionInvoker service) {

        log.error("deregister trigger from quartz for $triggerId, context $contextInfo, condition $condition, action $action, invoker $service")
        unscheduleTrigger(triggerId, contextInfo)
    }

    boolean unscheduleTrigger(String triggerId, RDTriggerContext contextInfo) {

        def quartzJobName = triggerId
        def quartzJobGroup = 'ScheduledTriggerConditionService'
        if (quartzScheduler.checkExists(JobKey.jobKey(quartzJobName, quartzJobGroup))) {
            log.info("Removing existing trigger $triggerId with context $contextInfo: " + quartzJobName)

            return quartzScheduler.unscheduleJob(TriggerKey.triggerKey(quartzJobName, quartzJobGroup))
        }
        false
    }

    Date scheduleTrigger(String triggerId, RDTriggerContext contextInfo, ScheduleTriggerCondition scheduled, Action action, TriggerActionInvoker invoker) {

        def quartzJobName = triggerId
        def quartzJobGroup = 'ScheduledTriggerConditionService'
        def jobDesc = "Attempt to schedule job $quartzJobName with context $contextInfo"
        if (!executionService.executionsAreActive) {
            log.warn("$jobDesc, but executions are disabled.")
            return null
        }

        if (!scheduledExecutionService.shouldScheduleInThisProject(contextInfo.project)) {
            log.warn("$jobDesc, but project executions are disabled.")
            return null
        }


        def jobDetailBuilder = JobBuilder.newJob(RDTriggerConditionJob)
                .withIdentity(quartzJobName, quartzJobGroup)
//                .withDescription(scheduled.)
                .usingJobData(new JobDataMap(createJobDetailMap(triggerId, contextInfo, scheduled, action, invoker)))


        def jobDetail = jobDetailBuilder.build()
        def trigger = scheduled.createTrigger(quartzJobName, quartzJobGroup)

        def Date nextTime

        if (quartzScheduler.checkExists(JobKey.jobKey(quartzJobName, quartzJobGroup))) {
            log.info("rescheduling existing trigger $triggerId with context $contextInfo: " + quartzJobName)

            nextTime = quartzScheduler.rescheduleJob(TriggerKey.triggerKey(quartzJobName, quartzJobGroup), trigger)
        } else {
            log.info("scheduling new trigger $triggerId with context $contextInfo: " + quartzJobName)
            nextTime = quartzScheduler.scheduleJob(jobDetail, trigger)
        }

        log.info("scheduled trigger $triggerId. next run: " + nextTime.toString())
        return nextTime
    }

    Map createJobDetailMap(String triggerId, RDTriggerContext contextInfo, ScheduleTriggerCondition scheduled, Action action, TriggerActionInvoker invoker) {
        [
                triggerId: triggerId,
                context  : contextInfo,
                condition: scheduled,
                action   : action,
                invoker  : invoker
        ]
    }
}
