package rundeck.services

import com.dtolabs.rundeck.core.schedule.JobScheduleFailure
import com.dtolabs.rundeck.core.schedule.JobScheduleManager
import grails.events.annotation.Subscriber
import groovy.transform.CompileStatic
import groovy.util.logging.Log4j
import org.grails.datastore.mapping.engine.event.PostInsertEvent
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.SchedulerException
import org.quartz.SimpleTrigger
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.quartz.TriggerKey
import org.quartz.impl.matchers.GroupMatcher
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import rundeck.Execution
import rundeck.quartzjobs.ExecutionJob

/**
 * Service which calls methods on the configured JobScheduleManager bean
 */
class JobSchedulerService implements JobScheduleManager {
    static transactional = false

    def JobScheduleManager rundeckJobScheduleManager

    @Override
    void deleteJobSchedule(final String name, final String group) {
        rundeckJobScheduleManager.deleteJobSchedule(name, group)
    }

    @Override
    Date scheduleJob(final String name, final String group, final Map data, final Date atTime)
            throws JobScheduleFailure
    {
        return rundeckJobScheduleManager.scheduleJob(name, group, data, atTime)
    }

    @Override
    boolean scheduleJobNow(final String name, final String group, final Map data) throws JobScheduleFailure {
        return rundeckJobScheduleManager.scheduleJobNow(name, group, data)
    }

    @Override
    boolean updateScheduleOwner(final String name, final String group, final Map data) {
        return rundeckJobScheduleManager.updateScheduleOwner(name, group, data)
    }

    @Override
    String determineExecNode(String name, String group, Map data, String project) {
        return rundeckJobScheduleManager.determineExecNode(name, group, data, project)
    }

    @Override
    List<String> getDeadMembers(String uuid) {
        return rundeckJobScheduleManager.getDeadMembers(uuid);
    }

    @Override
    boolean scheduleRemoteJob(Map data) {
        return rundeckJobScheduleManager.scheduleRemoteJob(data)
    }
}

/**
 * Internal manager to schedule {@link ExecutionJob}s via quartz
 */
@Log4j
@CompileStatic
class QuartzJobScheduleManagerService implements JobScheduleManager, InitializingBean {

    @Autowired
    Scheduler quartzScheduler

    @Autowired
    def FrameworkService frameworkService

    @Autowired
    ScheduledExecutionService scheduledExecutionService

    static String TRIGGER_GROUP_PENDING = 'pending'

    @Override
    void afterPropertiesSet() {
        GroupMatcher<TriggerKey> matcher = GroupMatcher.groupEquals(TRIGGER_GROUP_PENDING)
        quartzScheduler.pauseTriggers(matcher)
    }

    @Override
    void deleteJobSchedule(final String name, final String group) {
        quartzScheduler.deleteJob(new JobKey(name, group))
    }

    @Override
    Date scheduleJob(final String name, final String group, final Map data, final Date atTime)
            throws JobScheduleFailure
    {
        def jobDetail = JobBuilder.newJob(ExecutionJob)
                                  .withIdentity(name, group)
                                  .usingJobData(new JobDataMap(data ?: [:])).build()

        SimpleTrigger trigger = (SimpleTrigger) TriggerBuilder.newTrigger()
                                                              .withIdentity(name, TRIGGER_GROUP_PENDING)
                                                              .startAt(atTime)
                                                              .build()
        try {
            if (quartzScheduler.checkExists(jobDetail.getKey())) {
                return quartzScheduler.rescheduleJob(
                        TriggerKey.triggerKey(name, group),
                        trigger
                )
            } else {
                return quartzScheduler.scheduleJob(jobDetail, trigger)
            }
        } catch (SchedulerException exc) {
            throw new JobScheduleFailure("caught exception while adding job: " + exc.getMessage(), exc)
        }
    }

    @Override
    boolean scheduleJobNow(final String name, final String group, final Map data) throws JobScheduleFailure {
        log.info('ScheduleJobNow')
        def jobDetail = JobBuilder.newJob(ExecutionJob).storeDurably()
                                  .withIdentity(name, group)
                                  .usingJobData(new JobDataMap(data ?: [:])).build()

        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity(name, TRIGGER_GROUP_PENDING).build()

        try {
            return quartzScheduler.scheduleJob(jobDetail, trigger) != null
        } catch (SchedulerException exc) {
            throw new JobScheduleFailure("caught exception while adding job: " + exc.getMessage(), exc)
        }
    }

    void reschedulePendingExecution(Execution execution) {
        log.info("Execution insert event for $execution")

        def ident = scheduledExecutionService.getJobIdent(execution.scheduledExecution, execution)

        Trigger trigger = quartzScheduler.getTrigger(TriggerKey.triggerKey(ident.jobname, TRIGGER_GROUP_PENDING))

        if (trigger) {
            Trigger newTrigger = trigger.getTriggerBuilder().withIdentity(ident.jobname, ident.groupname).build()
            quartzScheduler.rescheduleJob(trigger.getKey(), newTrigger)
        }
    }

    @Override
    boolean updateScheduleOwner(final String name, final String group, final Map data) {
        return true
    }

    @Override
    String determineExecNode(String name, String group, Map data, String project) {
        return frameworkService.serverUUID
    }

    @Override
    List<String> getDeadMembers(String uuid) {
        return null;
    }

    @Override
    boolean scheduleRemoteJob(Map data) {
        false
    }

//    @Subscriber
    void onCreateExecution(Execution e) {
        reschedulePendingExecution(e)
    }

    @Subscriber
    void afterInsert(PostInsertEvent event) {
        if(!(event.entityObject instanceof Execution))
            return

        reschedulePendingExecution(event.entityObject as Execution)
    }


}
