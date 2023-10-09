package rundeck.services

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.execution.PreparedExecutionReference
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.schedule.JobScheduleFailure
import com.dtolabs.rundeck.core.schedule.JobScheduleManager
import grails.events.annotation.Subscriber
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
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
import rundeck.data.quartz.QuartzJobSpecifier
import rundeck.quartzjobs.ExecutionJob

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Service which calls methods on the configured JobScheduleManager bean
 */
@CompileStatic
class JobSchedulerService implements JobScheduleManager {
    static transactional = false

    def JobScheduleManager rundeckJobScheduleManager

    @Override
    void deleteJobSchedule(final String quartzJobName, final String quartzJobGroup) {
        rundeckJobScheduleManager.deleteJobSchedule(quartzJobName, quartzJobGroup)
    }

    @Override
    Date scheduleJob(final String quartzJobName, final String quartzJobGroup, final Map data, final Date atTime, final boolean pending)
            throws JobScheduleFailure
    {
        return rundeckJobScheduleManager.scheduleJob(quartzJobName, quartzJobGroup, data, atTime, pending)
    }

    @Override
    boolean scheduleJobNow(final String quartzJobName, final String quartzJobGroup, final Map data, final boolean pending) throws JobScheduleFailure {
        return rundeckJobScheduleManager.scheduleJobNow(quartzJobName, quartzJobGroup, data, pending)
    }

    @Override
    Date reschedulePendingJob(final String quartzJobName, final String quartzJobGroup) {
        return rundeckJobScheduleManager.reschedulePendingJob(quartzJobName, quartzJobGroup)
    }

    @Override
    boolean updateScheduleOwner(final JobReference job) {
        return rundeckJobScheduleManager.updateScheduleOwner(job)
    }

    @Override
    String determineExecNode(JobReference job) {
        return rundeckJobScheduleManager.determineExecNode(job)
    }

    @Override
    boolean tryAcquireExecCleanerJob(String uuid, String project) {
        return rundeckJobScheduleManager.tryAcquireExecCleanerJob(uuid, project);
    }

    @Override
    boolean scheduleRemoteJob(Map data) {
        return rundeckJobScheduleManager.scheduleRemoteJob(data)
    }

    @Override
    BeforeExecutionBehavior beforeExecution(final PreparedExecutionReference execution, Map<String, Object> dataMap, UserAndRolesAuthContext authContext) {
        return rundeckJobScheduleManager.beforeExecution(execution, dataMap, authContext)
    }

    @Override
    void afterExecution(final PreparedExecutionReference execution, Map<String, Object> dataMap, UserAndRolesAuthContext authContext) {
        rundeckJobScheduleManager.afterExecution(execution, dataMap, authContext)
    }
}

/**
 * Internal manager to schedule {@link ExecutionJob}s via quartz
 */
@Slf4j
@CompileStatic
class QuartzJobScheduleManagerService implements JobScheduleManager, InitializingBean {

    @Autowired
    Scheduler quartzScheduler

    @Autowired
    def FrameworkService frameworkService

    @Autowired
    ScheduledExecutionService scheduledExecutionService

    @Autowired
    QuartzJobSpecifier quartzJobSpecifier

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
    void deleteJobSchedule(final String quartzJobName, final String quartzJobGroup) {
        quartzScheduler.deleteJob(new JobKey(quartzJobName, quartzJobGroup))
    }

    @Override
    Date scheduleJob(final String quartzJobName, final String quartzJobGroup, final Map data, final Date atTime, final boolean pending)
            throws JobScheduleFailure
    {
        data.put('meta.created', Instant.now())

        String triggerGroup = pending ? TRIGGER_GROUP_PENDING : quartzJobGroup

        def jobDetail = JobBuilder.newJob(quartzJobSpecifier.getJobClass())
                                  .withIdentity(quartzJobName, quartzJobGroup)
                                  .usingJobData(new JobDataMap(data ?: [:])).build()

        SimpleTrigger trigger = (SimpleTrigger) TriggerBuilder.newTrigger()
                                                              .withIdentity(quartzJobName, triggerGroup)
                                                              .startAt(atTime)
                                                              .build()
        try {
            if (quartzScheduler.checkExists(jobDetail.getKey())) {
                return quartzScheduler.rescheduleJob(
                    TriggerKey.triggerKey(quartzJobName, triggerGroup),
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
    boolean scheduleJobNow(final String quartzJobName, final String quartzJobGroup, final Map data, final boolean pending) throws JobScheduleFailure {
        data.put('meta.created', Instant.now())

        String triggerGroup = pending ? TRIGGER_GROUP_PENDING : quartzJobGroup

        def jobDetail = JobBuilder.newJob(quartzJobSpecifier.getJobClass())
                                  .withIdentity(quartzJobName, quartzJobGroup)
                                  .usingJobData(new JobDataMap(data ?: [:])).build()

        Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity(quartzJobName, triggerGroup).build()

        try {
            return quartzScheduler.scheduleJob(jobDetail, trigger) != null
        } catch (SchedulerException exc) {
            throw new JobScheduleFailure("caught exception while adding job: " + exc.getMessage(), exc)
        }
    }

    @Override
    Date reschedulePendingJob(String quartzJobName, String quartzJobGroup) {
        try {
            Trigger trigger = quartzScheduler.getTrigger(TriggerKey.triggerKey(quartzJobName, TRIGGER_GROUP_PENDING))

            if (trigger == null) {
                log.debug("Pending trigger not found for reschedule: $quartzJobName")
                return null
            }

            if (trigger) {
                Trigger newTrigger = trigger.getTriggerBuilder().withIdentity(quartzJobName, quartzJobGroup).build()
                quartzScheduler.rescheduleJob(trigger.getKey(), newTrigger)
            }
        } catch (SchedulerException exc) {
            throw new JobScheduleFailure("caught exception rescheduling pending job: " + exc.getMessage(), exc)
        }
    }

    @Override
    boolean updateScheduleOwner(final JobReference job) {
        return true
    }

    @Override
    String determineExecNode(JobReference job) {
        return frameworkService.serverUUID
    }

    @Override
    boolean tryAcquireExecCleanerJob(String uuid, String project) {
        return true
    }

    @Override
    boolean scheduleRemoteJob(Map data) {
        false
    }

    @Override
    BeforeExecutionBehavior beforeExecution(final PreparedExecutionReference execution, Map<String,Object> dataMap, UserAndRolesAuthContext authContext) {
        return BeforeExecutionBehavior.proceed
    }

    @Override
    void afterExecution(final PreparedExecutionReference execution,Map<String,Object> dataMap, UserAndRolesAuthContext authContext) {

    }

    /**
     * Removes PENDING triggers if their job detail indicates they are over 2 minutes old.
     * This allows for some unexpected delays in committing due to lock timeouts and etc.
     */
    private void cleanupTriggers() {
        GroupMatcher<TriggerKey> matcher = GroupMatcher.groupEquals(TRIGGER_GROUP_PENDING)
        if(quartzScheduler.isShutdown()) return
        quartzScheduler.getTriggerKeys(matcher).each { triggerKey ->
            if (triggerKey.group == TRIGGER_GROUP_PENDING) {
                Trigger trigger = quartzScheduler.getTrigger(triggerKey)
                /** Trigger may have been rescheduled already under heavy activity */
                if (trigger == null)
                    return

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
