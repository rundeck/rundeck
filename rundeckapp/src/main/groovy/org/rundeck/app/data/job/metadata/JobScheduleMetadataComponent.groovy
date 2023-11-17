package org.rundeck.app.data.job.metadata

import com.dtolabs.rundeck.core.schedule.SchedulesManager
import groovy.transform.CompileStatic
import org.rundeck.app.components.jobs.ComponentMeta
import org.rundeck.app.components.jobs.JobMetadataComponent
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.model.v1.job.JobDataSummary
import org.rundeck.app.data.model.v1.job.schedule.ScheduleData
import org.rundeck.app.data.providers.v1.job.JobDataProvider
import org.springframework.beans.factory.annotation.Autowired

/**
 * Includes schedule information from job definitions
 */
@CompileStatic
class JobScheduleMetadataComponent implements JobMetadataComponent {
    static final String SCHEDULE_NAME = 'schedule'
    @Autowired
    JobDataProvider jobDataProvider
    @Autowired
    SchedulesManager jobSchedulesService

    @Override
    Set<String> getAvailableMetadataNames() {
        return [SCHEDULE_NAME].toSet()
    }

    @Override
    Optional<List<ComponentMeta>> getMetadataForJob(final String id, String project,final Set<String> names) {
        if (!names.contains(SCHEDULE_NAME) && !names.contains('*')) {
            return Optional.empty()
        }
        Optional<JobDataSummary> foundJob = jobDataProvider.findBasicByUuid(id)
        return foundJob.map {
            toMapData(it)
        }.map {
            [ComponentMeta.with(SCHEDULE_NAME, it)]
        }
    }

    @Override
    Optional<List<ComponentMeta>> getMetadataForJob(final JobDataSummary job, final Set<String> names) {
        if (!names.contains(SCHEDULE_NAME) && !names.contains('*')) {
            return Optional.empty()
        }
        def data = toMapData(job)
        if (data) {
            return Optional.of([ComponentMeta.with(SCHEDULE_NAME, data)])
        }
        return Optional.empty()
    }

    Map<String, Object> toMapData(JobDataSummary jobData) {
        def scheduleData = [:]
        boolean scheduled=false
        //only look up schedule info if the job is scheduled and schedule is enabled
        if (jobData.scheduleEnabled && jobSchedulesService.isScheduled(jobData.uuid)) {
            scheduled=true
            JobData foundJob = jobDataProvider.findByUuid(jobData.uuid)
            scheduleData = scheduleDataMap(foundJob.schedule)
        }
        def data = [
            scheduleEnabled  : jobData.scheduleEnabled,
            executionEnabled : jobData.executionEnabled,
            schedule         : scheduleData,
            serverNodeUUID   : jobData.serverNodeUUID,
            nextExecutionTime: scheduled?jobSchedulesService.nextExecutionTime(jobData.uuid, false):null,
            hasSchedule      : jobSchedulesService.isScheduled(jobData.uuid),
        ]
        return data
    }

    Map<String, Object> toMapData(JobData jobData) {
        [
            scheduleEnabled  : jobData.scheduleEnabled,
            executionEnabled : jobData.executionEnabled,
            schedule         : scheduleDataMap(jobData.schedule),
            serverNodeUUID   : jobData.serverNodeUUID,
            nextExecutionTime: jobSchedulesService.nextExecutionTime(jobData.uuid, false),
            hasSchedule      : jobSchedulesService.isScheduled(jobData.uuid),
        ]
    }

    static Map<String, Object> scheduleDataMap(ScheduleData scheduleData) {
        if (!scheduleData) {
            return [:]
        }
        [
            minute       : scheduleData.minute,
            hour         : scheduleData.hour,
            month        : scheduleData.month,
            seconds      : scheduleData.seconds,
            year         : scheduleData.year,
            dayOfWeek    : scheduleData.dayOfWeek,
            dayOfMonth   : scheduleData.dayOfMonth,
            crontabString: scheduleData.crontabString,
        ] as Map<String, Object>
    }
}
