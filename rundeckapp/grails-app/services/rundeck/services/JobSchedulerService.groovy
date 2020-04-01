package rundeck.services

import com.dtolabs.rundeck.core.schedule.JobScheduleFailure
import com.dtolabs.rundeck.core.schedule.JobScheduleManager
import grails.events.annotation.Subscriber
import groovy.transform.CompileStatic
import groovy.util.logging.Log4j
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent
import org.grails.datastore.mapping.engine.event.PostInsertEvent
import org.grails.datastore.mapping.engine.event.PostUpdateEvent
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

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

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
    Date scheduleJob(final String name, final String group, final Map data, final Date atTime, final boolean pending)
            throws JobScheduleFailure
    {
        return rundeckJobScheduleManager.scheduleJob(name, group, data, atTime, pending)
    }

    @Override
    boolean scheduleJobNow(final String name, final String group, final Map data, final boolean pending) throws JobScheduleFailure {
        return rundeckJobScheduleManager.scheduleJobNow(name, group, data, pending)
    }

    @Override
    Date reschedulePendingJob(final String name, final String group) {
        return rundeckJobScheduleManager.reschedulePendingJob(name, group)
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

    ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1)

    @Override
    void afterPropertiesSet() {
        GroupMatcher<TriggerKey> matcher = GroupMatcher.groupEquals(TRIGGER_GROUP_PENDING)
        quartzScheduler.pauseTriggers(matcher)

        /** Cleanup PENDING triggers for jobs older than 2 minutes */
        scheduledExecutor.scheduleAtFixedRate({
            try {
                cleanupTriggers()
            } catch (Throwable t) {
                log.error("Error cleaning up PENDING triggers: $t", t)
            }
        }, 15, 15, TimeUnit.SECONDS)
    }

    @Override
    void deleteJobSchedule(final String name, final String group) {
        quartzScheduler.deleteJob(new JobKey(name, group))
    }

    @Override
    Date scheduleJob(final String name, final String group, final Map data, final Date atTime, final boolean pending)
            throws JobScheduleFailure
    {
        data.put('meta.created', Instant.now())

        String triggerGroup = pending ? TRIGGER_GROUP_PENDING : group

        def jobDetail = JobBuilder.newJob(ExecutionJob)
                                  .withIdentity(name, group)
                                  .usingJobData(new JobDataMap(data ?: [:])).build()

        SimpleTrigger trigger = (SimpleTrigger) TriggerBuilder.newTrigger()
                                                              .withIdentity(name, triggerGroup)
                                                              .startAt(atTime)
                                                              .build()
        try {
            if (quartzScheduler.checkExists(jobDetail.getKey())) {
                return quartzScheduler.rescheduleJob(
                        TriggerKey.triggerKey(name, triggerGroup),
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
    boolean scheduleJobNow(final String name, final String group, final Map data, final boolean pending) throws JobScheduleFailure {
        data.put('meta.created', new Date().getTime())

        String triggerGroup = pending ? TRIGGER_GROUP_PENDING : group

        def jobDetail = JobBuilder.newJob(ExecutionJob).storeDurably()
                                  .withIdentity(name, group)
                                  .usingJobData(new JobDataMap(data ?: [:])).build()

        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity(name, triggerGroup).build()

        try {
            return quartzScheduler.scheduleJob(jobDetail, trigger) != null
        } catch (SchedulerException exc) {
            throw new JobScheduleFailure("caught exception while adding job: " + exc.getMessage(), exc)
        }
    }

    @Override
    Date reschedulePendingJob(String name, String group) {
        try {
            Trigger trigger = quartzScheduler.getTrigger(TriggerKey.triggerKey(name, TRIGGER_GROUP_PENDING))

            if (trigger == null) {
                log.debug("Pending trigger not found for reschedule: $name")
                return null
            }

            if (trigger) {
                Trigger newTrigger = trigger.getTriggerBuilder().withIdentity(name, group).build()
                quartzScheduler.rescheduleJob(trigger.getKey(), newTrigger)
            }
        } catch (SchedulerException exc) {
            throw new JobScheduleFailure("caught exception rescheduling pending job: " + exc.getMessage(), exc)
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

    /**
     * Removes PENDING triggers if their job detail indicates they are over 2 minutes old.
     * This allows for some unexpected delays in committing due to lock timeouts and etc.
     */
    private void cleanupTriggers() {
        GroupMatcher<TriggerKey> matcher = GroupMatcher.groupEquals(TRIGGER_GROUP_PENDING)
        quartzScheduler.getTriggerKeys(matcher).each { triggerKey ->
            if (triggerKey.group == TRIGGER_GROUP_PENDING) {
                Trigger trigger = quartzScheduler.getTrigger(triggerKey)
                Map data = trigger.jobDataMap
                Instant created = data.get('meta.created') as Instant

                if (created.isBefore(created.minus(2, ChronoUnit.MINUTES)))
                    quartzScheduler.unscheduleJob(triggerKey)
            }
        }
    }

    @Subscriber
    void afterUpdate(PostUpdateEvent event) {
        handleExecutionEvent(event)
    }

    @Subscriber
    void afterInsert(PostInsertEvent event) {
        handleExecutionEvent(event)
    }

    private handleExecutionEvent(AbstractPersistenceEvent event) {
        if(!(event.entityObject instanceof Execution))
            return

        Execution execution = event.entityObject as Execution

        rescheduleExecutionIfPending(execution)
    }

    private rescheduleExecutionIfPending(Execution execution) {
        log.debug("Rescheduling pending execution $execution.id")

        def ident = scheduledExecutionService.getJobIdent(execution.scheduledExecution, execution)

        reschedulePendingJob(ident.jobname, ident.groupname)
    }
}
