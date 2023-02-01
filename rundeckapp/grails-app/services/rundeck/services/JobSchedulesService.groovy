package rundeck.services

import com.dtolabs.rundeck.core.schedule.SchedulesManager
import org.quartz.*
import org.rundeck.app.components.schedule.TriggerBuilderHelper
import rundeck.ScheduledExecution

class JobSchedulesService implements SchedulesManager {

    SchedulesManager rundeckJobSchedulesManager

    @Override
    Map handleScheduleDefinitions(String jobUUID, boolean isUpdate) {
        return rundeckJobSchedulesManager.handleScheduleDefinitions(jobUUID, isUpdate)
    }

    @Override
    TriggerBuilderHelper createTriggerBuilder(String jobName, String jobGroup, String cronExpression, int priority) {
        return rundeckJobSchedulesManager.createTriggerBuilder(jobName, jobGroup, cronExpression, priority)
    }

    @Override
    TriggerBuilderHelper createTriggerBuilder(String jobUUID, String cronExpression, String triggerName) {
        return rundeckJobSchedulesManager.createTriggerBuilder(jobUUID, cronExpression, triggerName)
    }

    @Override
    Date nextExecutionTime(String jobUUID, boolean require) {
        return rundeckJobSchedulesManager.nextExecutionTime(jobUUID, require)
    }

    @Override
    Map<String, Date> bulkNextExecutionTime(final String project, final List<String> jobUuids) {
        return rundeckJobSchedulesManager.bulkNextExecutionTime(project, jobUuids)
    }

    @Override
    boolean isScheduled(String jobUUID) {
        return rundeckJobSchedulesManager.isScheduled(jobUUID)
    }

    @Override
    List getAllScheduled(String serverUUID, String project) {
        return rundeckJobSchedulesManager.getAllScheduled(serverUUID, project)
    }

    @Override
    boolean shouldScheduleExecution(String jobUUID) {
        return rundeckJobSchedulesManager.shouldScheduleExecution(jobUUID)
    }

    @Override
    List getSchedulesJobToClaim(String toServerUUID, String fromServerUUID, boolean selectAll, String projectFilter, List<String> jobids) {
        return rundeckJobSchedulesManager.getSchedulesJobToClaim(toServerUUID, fromServerUUID, selectAll, projectFilter, jobids)
    }

    @Override
    List getJobsWithAdhocScheduledExecutionsToClaim(
        final String toServerUUID,
        final String fromServerUUID,
        final boolean selectAll,
        final String projectFilter
    ) {
        return rundeckJobSchedulesManager.
            getJobsWithAdhocScheduledExecutionsToClaim(toServerUUID, fromServerUUID, selectAll, projectFilter)
    }

    @Override
    List<Date> nextExecutions(String jobUuid, Date to, boolean past) {
        return rundeckJobSchedulesManager.nextExecutions(jobUuid, to, past)
    }

}

class LocalJobSchedulesManager implements SchedulesManager {

    def scheduledExecutionService
    def frameworkService
    Scheduler quartzScheduler

    @Override
    Map handleScheduleDefinitions(String jobUUID, boolean isUpdate) {
        def se = ScheduledExecution.findByUuid(jobUUID)
        def jobDetail = scheduledExecutionService.createJobDetail(se, se.generateJobScheduledName(), se.generateJobGroupName())
        def trigger = createTriggerBuilder(se)
        jobDetail.getJobDataMap().put("bySchedule", true)
        Date nextTime
        nextTime = scheduledExecutionService.registerOnQuartz(jobDetail, [trigger], false, se)
        return [nextTime: nextTime]
    }

    @Override
    TriggerBuilderHelper createTriggerBuilder(String jobName, String jobGroup, String cronExpression, int priority) {
        return createTriggerBuilderLocal(jobName, jobGroup, cronExpression, priority)
    }

    @Override
    TriggerBuilderHelper createTriggerBuilder(String jobUUID, String cronExpression, String triggerName) {
        return createTriggerBuilder(ScheduledExecution.findByUuid(jobUUID))
    }

    @Override
    Date nextExecutionTime(String jobUUID, boolean require) {
        return this.nextExecutionTime(ScheduledExecution.findByUuid(jobUUID), require)
    }

    @Override
    boolean isScheduled(String jobUUID) {
        return ScheduledExecution.findByUuid(jobUUID)?.scheduled
    }

    @Override
    List getAllScheduled(String serverUUID, String project) {
        return this.listScheduledJobs(serverUUID, project);
    }

    @Override
    boolean shouldScheduleExecution(String jobUUID) {
        return ScheduledExecution.findByUuid(jobUUID).shouldScheduleExecution()
    }

    @Override
    List getSchedulesJobToClaim(String toServerUUID, String fromServerUUID, boolean selectAll, String projectFilter, List<String> jobids) {
        return scheduledExecutionService.getSchedulesJobToClaim(toServerUUID, fromServerUUID, selectAll, projectFilter, jobids)
    }

    @Override
    List getJobsWithAdhocScheduledExecutionsToClaim(String toServerUUID, String fromServerUUID, boolean selectAll, String projectFilter){
        return scheduledExecutionService.getJobsWithAdhocScheduledExecutionsToClaim(toServerUUID, fromServerUUID, selectAll, projectFilter)
    }

    @Override
    List<Date> nextExecutions(String jobUuid, Date to, boolean past) {
        def triggerHelper = this.createTriggerBuilder(jobUuid,null,null)
        def jobDetail = scheduledExecutionService.createJobDetail(ScheduledExecution.findByUuid(jobUuid))
        def triggerBuilderList = scheduledExecutionService.applyTriggerComponents(jobDetail , [triggerHelper])
        def dates = []
        triggerBuilderList?.each{ builder ->
            def trigger = builder.triggerBuilder.build()
            if(past){
                dates.addAll(TriggerUtils.computeFireTimesBetween(trigger, (trigger.getCalendarName()? quartzScheduler.getCalendar(trigger.getCalendarName()):null), to, new Date()))
            }else {
                dates.addAll(TriggerUtils.computeFireTimesBetween(trigger, (trigger.getCalendarName()? quartzScheduler.getCalendar(trigger.getCalendarName()):null), new Date(), to))
            }
            Collections.sort(dates)
        }
        return dates
    }

    /**
     * Return the next scheduled or predicted execution time for the scheduled job, and if it is not scheduled
     * return a time in the future.  If the job is not scheduled on the current server (cluster mode), returns
     * the time that the job is expected to run on its configured server.
     * @param se
     * @return
     */
    Date nextExecutionTime(ScheduledExecution se, boolean require=false) {
        if(!se.scheduled){
            return null
        }
        if(!require && (!se.scheduleEnabled ||!se.executionEnabled ||
                !scheduledExecutionService.isProjectScheduledEnabled(se.project) ||
                !scheduledExecutionService.isProjectExecutionEnabled(se.project)
        )) {
            return null
        }
        return calculateNextExecutionTime(se)
    }

    Date calculateNextExecutionTime(ScheduledExecution se) {
        def triggerBuilder = createTriggerBuilder(se)
        def jobDetail = scheduledExecutionService.createJobDetail(se)
        scheduledExecutionService.applyTriggerComponents(jobDetail , [triggerBuilder])
        def trigger=triggerBuilder.triggerBuilder.build()

        def times = TriggerUtils.
            computeFireTimes(
                trigger,
                (trigger.getCalendarName() ? quartzScheduler.getCalendar(trigger.getCalendarName()) : null),
                1
            )
        return times?times.first():null
    }

    Map<String,Date> bulkNextExecutionTime(String project, List<String> jobUuids) {
        if(jobUuids.isEmpty() ||
           !scheduledExecutionService.isProjectScheduledEnabled(project) ||
           !scheduledExecutionService.isProjectExecutionEnabled(project)) return [:]
        def jobs = ScheduledExecution.findAllByUuidInList(jobUuids)
        Map results = [:]
        jobs.each { job ->
            if(job.scheduleEnabled && job.scheduled && job.executionEnabled && job.project == project) {
                results[job.uuid] = calculateNextExecutionTime(job)
            } else {
                results[job.uuid] = null
            }
        }
        return results
    }

    TriggerBuilderHelper createTriggerBuilderLocal(String jobName, String jobGroup, String cronExpression, int priority = 5) {
        TriggerBuilder triggerBuilder
        try {
            triggerBuilder = TriggerBuilder.newTrigger().withIdentity(jobName, jobGroup)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                    .withPriority(priority)

        } catch (java.text.ParseException ex) {
            throw new RuntimeException("Failed creating trigger. Invalid cron expression: " + cronExpression )
        }
        return (new TriggerHelperImpl(triggerBuilder, null))
    }

    TriggerBuilderHelper createTriggerBuilder(ScheduledExecution se) {
        TriggerBuilder triggerBuilder
        def cronExpression = se.generateCrontabExression()
        def builderParams = [:]
        try {
            if(se.timeZone){
                triggerBuilder = TriggerBuilder.newTrigger().withIdentity(se.generateJobScheduledName(), se.generateJobGroupName())
                        .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression).inTimeZone(TimeZone.getTimeZone(se.timeZone)))
                builderParams[TriggerHelperImpl.TIME_ZONE_KEY] = TimeZone.getTimeZone(se.timeZone)
            }else {
                triggerBuilder = TriggerBuilder.newTrigger().withIdentity(se.generateJobScheduledName(), se.generateJobGroupName())
                        .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
            }
        } catch (java.text.ParseException ex) {
            throw new RuntimeException("Failed creating trigger. Invalid cron expression: " + cronExpression )
        }
        return (new TriggerHelperImpl(triggerBuilder, builderParams))
    }

    /**
     * list scheduled jobs which match the given serverUUID, or all jobs if it is null.
     * @param serverUUID
     * @param project
     * @return
     */
    def listScheduledJobs(String serverUUID = null, String project = null){
        def results = ScheduledExecution.scheduledJobs()
        if (serverUUID) {
            results = results.withServerUUID(serverUUID)
        }
        if(project) {
            results = results.withProject(project)
        }
        results.list()
    }

}

class TriggerHelperImpl implements TriggerBuilderHelper {

    static final String TIME_ZONE_KEY = 'timeZone'

    TriggerBuilder triggerBuilder
    Map params

    TriggerHelperImpl(TriggerBuilder triggerBuilder, Map params) {
        this.triggerBuilder = triggerBuilder
        this.params = params
    }

    @Override
    Object getTriggerBuilder() {
        return triggerBuilder
    }

    @Override
    Map getParams() {
        return params
    }

    @Override
    Object getTimeZone() {
        if(params){
            return params[TIME_ZONE_KEY]
        }
        return null
    }
}
