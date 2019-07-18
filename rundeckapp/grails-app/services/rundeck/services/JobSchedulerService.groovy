package rundeck.services

import com.dtolabs.rundeck.core.schedule.JobScheduleFailure
import com.dtolabs.rundeck.core.schedule.JobScheduleManager
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.SchedulerException
import org.quartz.SimpleTrigger
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.quartz.TriggerKey
import org.springframework.beans.factory.annotation.Autowired
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
}

/**
 * Internal manager to schedule {@link ExecutionJob}s via quartz
 */
class QuartzJobScheduleManager implements JobScheduleManager {

    @Autowired
    def Scheduler quartzScheduler

    @Autowired
    def FrameworkService frameworkService


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
                                                              .withIdentity(name, group)
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
        def jobDetail = JobBuilder.newJob(ExecutionJob)
                                  .withIdentity(name, group)
                                  .usingJobData(new JobDataMap(data ?: [:])).build()

        def Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity(name + "Trigger").build()

        try {
            return quartzScheduler.scheduleJob(jobDetail, trigger) != null
        } catch (SchedulerException exc) {
            throw new JobScheduleFailure("caught exception while adding job: " + exc.getMessage(), exc)
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
}
