package rundeck.services

import com.dtolabs.rundeck.server.plugins.tasks.trigger.QuartzSchedulerTaskTrigger
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.SchedulerException
import org.quartz.TriggerBuilder
import org.quartz.TriggerKey
import org.rundeck.core.plugins.CollaboratesWith
import org.rundeck.core.tasks.TaskAction
import org.rundeck.core.tasks.TaskActionInvoker
import org.rundeck.core.tasks.TaskCondition
import org.rundeck.core.tasks.TaskTrigger
import org.rundeck.core.tasks.TaskTriggerHandler
import org.rundeck.core.tasks.TriggerException
import rundeck.quartzjobs.RDTaskTriggerJob

/**
 * Handles trigger based schedules
 */
class ScheduledTaskTriggerService implements TaskTriggerHandler<RDTaskContext> {

    static transactional = false

    Scheduler quartzScheduler
    def scheduledExecutionService
    def executionService

    @Override
    boolean onStartup() {
        true
    }

    @Override
    boolean handlesTrigger(TaskTrigger trigger, RDTaskContext context) {
        trigger instanceof QuartzSchedulerTaskTrigger
    }

    @Override
    boolean registerTriggerForAction(
        final RDTaskContext context,
        final TaskTrigger trigger,
        final List<TaskCondition> conditions,
        final TaskAction action,
        final TaskActionInvoker<RDTaskContext> service
    ) throws TriggerException {
        QuartzSchedulerTaskTrigger scheduled = (QuartzSchedulerTaskTrigger) trigger
        if (!scheduled.validSchedule) {
            log.info(
                "scheduled task ${context.taskId}: not scheduling the Quartz Trigger of type $scheduled: not valid"
            )
            return false
        }
        log.info(
            "schedule task using quartz for $context.taskId, context $context, trigger $trigger, action $action, invoker $service"
        )
        scheduleTrigger(context, scheduled, conditions, action, service) != null

    }

    @Override
    void deregisterTriggerForAction(
        final RDTaskContext context,
        final TaskTrigger trigger,
        final List<TaskCondition> conditions,
        final TaskAction action,
        final TaskActionInvoker<RDTaskContext> service
    ) {
        log.error(
            "deregister task from quartz for ${context.taskId}, context $context, trigger $trigger, action $action, invoker $service"
        )
        unscheduleTrigger(context)
    }

    boolean unscheduleTrigger(RDTaskContext contextInfo) {

        def quartzJobName = contextInfo.taskId
        def quartzJobGroup = 'ScheduledTaskTriggerService'
        if (quartzScheduler.checkExists(JobKey.jobKey(quartzJobName, quartzJobGroup))) {
            log.info("Removing existing task ${contextInfo.taskId} with context $contextInfo: " + quartzJobName)
            return quartzScheduler.unscheduleJob(TriggerKey.triggerKey(quartzJobName, quartzJobGroup))
        }
        false
    }

    Date scheduleTrigger(
        RDTaskContext context,
        QuartzSchedulerTaskTrigger scheduled,
        final List<TaskCondition> taskConditions,
        TaskAction action,
        TaskActionInvoker invoker
    ) throws TriggerException {

        def quartzJobName = context.taskId
        def quartzJobGroup = 'ScheduledTaskTriggerService'
        def jobDesc = "Attempt to schedule job $quartzJobName with context $context"
        if (!executionService.executionsAreActive) {
            log.warn("$jobDesc, but executions are disabled.")
            return null
        }

        if (!scheduledExecutionService.shouldScheduleInThisProject(context.project)) {
            log.warn("$jobDesc, but project executions are disabled.")
            return null
        }


        def jobDetailBuilder =
            JobBuilder
                .newJob(RDTaskTriggerJob)
                .withIdentity(quartzJobName, quartzJobGroup)
                .usingJobData(
                new JobDataMap(createJobDetailMap(context, scheduled, action, invoker))
            )



        def triggerbuilder = TriggerBuilder.newTrigger().withIdentity(quartzJobName, quartzJobGroup)

        scheduled.withQuartzTriggerBuilder(triggerbuilder)
        taskConditions.findAll { it instanceof CollaboratesWith }.each { condition ->

            CollaboratesWith collab = (CollaboratesWith) condition

            if (collab.canCollaborateWith(TriggerBuilder)) {
                collab.collaborateWith(triggerbuilder)
            }

        }
        def trigger = triggerbuilder.build()

        def Date nextTime
        def jobDetail = jobDetailBuilder.build()

        try {
            if (quartzScheduler.checkExists(JobKey.jobKey(quartzJobName, quartzJobGroup))) {
                log.info("rescheduling existing task ${context.taskId} with context $context: " + quartzJobName)

                nextTime = quartzScheduler.rescheduleJob(TriggerKey.triggerKey(quartzJobName, quartzJobGroup), trigger)
            } else {
                log.info("scheduling new task ${context.taskId} with context $context: " + quartzJobName)
                nextTime = quartzScheduler.scheduleJob(jobDetail, trigger)
            }
            log.info("scheduled task ${context.taskId}. next run: " + nextTime.toString())
        } catch (SchedulerException e) {
            log.debug("Failed scheduling quartz trigger for task: ${context.taskId}: $e", e)
            throw new TriggerException("Failed scheduling quartz trigger for task: ${e.message}",e)
        }
        return nextTime
    }

    Map createJobDetailMap(

        RDTaskContext contextInfo,
        QuartzSchedulerTaskTrigger trigger,
        TaskAction action,
        TaskActionInvoker invoker
    ) {
        [

            context: contextInfo,
            trigger: trigger,
            action : action,
            invoker: invoker
        ]
    }
}
