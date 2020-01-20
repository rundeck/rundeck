package rundeck.services

import com.dtolabs.rundeck.core.schedule.JobCalendarBase
import com.dtolabs.rundeck.core.schedule.SchedulesManager
import org.hibernate.criterion.CriteriaSpecification
import org.quartz.Calendar
import org.quartz.CronScheduleBuilder
import org.quartz.Scheduler
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.quartz.TriggerKey
import org.quartz.TriggerUtils
import org.quartz.impl.calendar.BaseCalendar
import rundeck.ScheduledExecution

class JobSchedulesService implements SchedulesManager {

    static transactional = false
    SchedulesManager rundeckJobSchedulesManager

    @Override
    boolean isSchedulesEnable() {
        return rundeckJobSchedulesManager.isSchedulesEnable()
    }

    @Override
    Map handleScheduleDefinitions(String jobUUID, boolean isUpdate) {
        return rundeckJobSchedulesManager.handleScheduleDefinitions(jobUUID, isUpdate)
    }

    @Override
    Trigger createTrigger(String jobName, String jobGroup, String cronExpression, int priority) {
        return rundeckJobSchedulesManager.createTrigger(jobName, jobGroup, cronExpression, priority)
    }

    @Override
    Trigger createTrigger(String jobUUID, String calendarName, String cronExpression, String triggerName) {
        return rundeckJobSchedulesManager.createTrigger(jobUUID, calendarName, cronExpression, triggerName)
    }

    @Override
    Date nextExecutionTime(String jobUUID, boolean require) {
        return rundeckJobSchedulesManager.nextExecutionTime(jobUUID, require)
    }

    @Override
    boolean isScheduled(String jobUUID) {
        return rundeckJobSchedulesManager.isScheduled(jobUUID)
    }

    @Override
    List getAllScheduled(String serverUUID = null, String project = null) {
        return rundeckJobSchedulesManager.getAllScheduled(serverUUID, project)
    }

    @Override
    boolean shouldScheduleExecution(String jobUUID) {
        return rundeckJobSchedulesManager.shouldScheduleExecution(jobUUID)
    }

    @Override
    void persistSchedulesToJob(String uuid, List schedules, Boolean shouldSchedule, String project) {
        rundeckJobSchedulesManager.persistSchedulesToJob(uuid, schedules, shouldSchedule, project)
    }

    @Override
    List getJobSchedules(String uuid, String project) {
        return rundeckJobSchedulesManager.getJobSchedules(uuid, project)
    }

    @Override
    List getSchedulesJobToClaim(String toServerUUID, String fromServerUUID, boolean selectAll, String projectFilter, List<String> jobids) {
        return rundeckJobSchedulesManager.getSchedulesJobToClaim(toServerUUID, fromServerUUID, selectAll, projectFilter, jobids)
    }

    @Override
    List<Date> nextExecutions(String jobUuid, Date to, boolean past) {
        return rundeckJobSchedulesManager.nextExecutions(jobUuid, to, past)
    }

    def setJobSchedules(ScheduledExecution se){
        if(this.isSchedulesEnable()){
            def scheduleDefinitions = this.getJobSchedules(se.uuid, se.project)
            se.scheduleDefinitions = scheduleDefinitions
        }
    }
}

class LocalJobSchedulesManager implements SchedulesManager {

    def scheduledExecutionService
    def frameworkService
    Scheduler quartzScheduler
    def jobSchedulerCalendarService

    @Override
    boolean isSchedulesEnable() {
        return false
    }

    @Override
    Map handleScheduleDefinitions(String jobUUID, boolean isUpdate) {
        def se = ScheduledExecution.findByUuid(jobUUID)
        def jobDetail = scheduledExecutionService.createJobDetail(se, se.generateJobScheduledName(), se.generateJobGroupName())
        def calendarName = handleJobCalendar(se)
        def trigger = createTrigger(se, calendarName)
        jobDetail.getJobDataMap().put("bySchedule", true)
        Date nextTime
        if ( scheduledExecutionService.hasJobScheduled(se) ) {
            nextTime = quartzScheduler.rescheduleJob(TriggerKey.triggerKey(se.generateJobScheduledName(), se.generateJobGroupName()), trigger)
        } else {
            nextTime = quartzScheduler.scheduleJob(jobDetail, trigger)
        }
        return [nextTime: nextTime]
    }

    @Override
    Trigger createTrigger(String jobName, String jobGroup, String cronExpression, int priority) {
        return localCreateTrigger(jobName, jobGroup, cronExpression, priority)
    }

    @Override
    Trigger createTrigger(String jobUUID, String calendarName, String cronExpression, String triggerName) {
        return createTrigger(ScheduledExecution.findByUuid(jobUUID), calendarName)
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
    void persistSchedulesToJob(String uuid, List schedules, Boolean shouldSchedule, String project) {}

    @Override
    List getJobSchedules(String uuid, String project) {
        return null
    }

    @Override
    List getSchedulesJobToClaim(String toServerUUID, String fromServerUUID, boolean selectAll, String projectFilter, List<String> jobids) {
        return null
    }

    @Override
    List<Date> nextExecutions(String jobUuid, Date to, boolean past) {
        def trigger = this.createTrigger(jobUuid,null,null,null)
        if(past){
            return TriggerUtils.computeFireTimesBetween(trigger, handleJobCalendar(ScheduledExecution.findByUuid(jobUuid)), to, new Date())
        }else {
            return TriggerUtils.computeFireTimesBetween(trigger, handleJobCalendar(ScheduledExecution.findByUuid(jobUuid)), new Date(), to)
        }
    }

    public static final long TWO_HUNDRED_YEARS=1000l * 60l * 60l * 24l * 365l * 200l

    /**
     * Return the next scheduled or predicted execution time for the scheduled job, and if it is not scheduled
     * return a time in the future.  If the job is not scheduled on the current server (cluster mode), returns
     * the time that the job is expected to run on its configured server.
     * @param se
     * @return
     */
    def Date nextExecutionTime(ScheduledExecution se, boolean require=false) {
        if(!se.scheduled){
            return new Date(TWO_HUNDRED_YEARS)
        }
        if(!require && (!se.scheduleEnabled || !se.executionEnabled)){
            return null
        }
        if(frameworkService.isClusterModeEnabled()) {
            //In cluster mode the local quartz scheduler trigger info might have outdated schedule information
            //so we always generate the next execution time from the db job info
            return tempNextExecutionTime(se)
        } else {
            return quartzScheduler.getTrigger(TriggerKey.triggerKey(se.generateJobScheduledName(), se.generateJobGroupName()))?.nextFireTime
        }
    }

    /**
     * Return the Date for the next execution time for a scheduled job
     * @param se
     * @return
     */
    Date tempNextExecutionTime(ScheduledExecution se){
        def trigger = createTrigger(se)
        return TriggerUtils.computeFireTimes(trigger, handleJobCalendar(se.uuid), 1)
    }

    Trigger localCreateTrigger(String jobName, String jobGroup, String cronExpression, int priority = 5) {
        Trigger trigger
        try {
            trigger = TriggerBuilder.newTrigger().withIdentity(jobName, jobGroup)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                    .withPriority(priority)
                    .build()

        } catch (java.text.ParseException ex) {
            throw new RuntimeException("Failed creating trigger. Invalid cron expression: " + cronExpression )
        }
        return trigger
    }

    Trigger createTrigger(ScheduledExecution se, calendarName = null) {
        Trigger trigger
        def cronExpression = se.generateCrontabExression()
        try {
            if(se.timeZone){
                trigger = TriggerBuilder.newTrigger().withIdentity(se.generateJobScheduledName(), se.generateJobGroupName())
                        .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression).inTimeZone(TimeZone.getTimeZone(se.timeZone)))
                        .modifiedByCalendar(calendarName)
                        .build()
            }else {
                trigger = TriggerBuilder.newTrigger().withIdentity(se.generateJobScheduledName(), se.generateJobGroupName())
                        .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                        .modifiedByCalendar(calendarName)
                        .build()
            }
        } catch (java.text.ParseException ex) {
            throw new RuntimeException("Failed creating trigger. Invalid cron expression: " + cronExpression )
        }
        return trigger
    }

    def handleJobCalendar(scheduledExecution){
        if(jobSchedulerCalendarService.isCalendarEnable()){
            JobCalendarBase calendar = jobSchedulerCalendarService.getQuartzCalendar(scheduledExecution.project, scheduledExecution.uuid)
            if(calendar){
                if(scheduledExecution.timeZone){
                    calendar.setTimeZone(TimeZone.getTimeZone(scheduledExecution.timeZone))
                }
                this.registerCalendar(calendar)
                def calendarName = calendar.registerName
                return calendarName
            }
        }
        return null
    }

    /**
     * add a calendar into the quartz scheduler
     * @param JobCalendarBase calendar
     */
    def registerCalendar(JobCalendarBase calendar){
        if(!quartzScheduler.getCalendar(calendar.registerName)) {
            quartzScheduler.addCalendar(calendar.registerName, calendar, false, false)

        }
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