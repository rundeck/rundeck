package org.rundeck.app.data.job.schedule

import org.rundeck.app.data.model.v1.job.JobData
import rundeck.services.JobRevReferenceImpl

class DefaultJobDataChangeDetector implements JobDataChangeDetector {

    Boolean localScheduled
    String originalCron
    Boolean originalSchedule
    Boolean originalExecution
    String originalTz
    JobRevReferenceImpl originalRef


    boolean wasRenamed(JobData newJob) {
        originalRef.jobName != newJob.jobName || originalRef.groupPath != newJob.groupPath
    }

    @Override
    boolean schedulingWasChanged(JobData newJob) {
        return originalCron != DefaultCrontabExpressionGenerator.generateCrontab(newJob) ||
                originalSchedule != newJob.scheduleEnabled ||
                originalExecution != newJob.executionEnabled ||
                originalTz != newJob.timeZone ||
                localScheduled != newJob.scheduled ||
                wasRenamed(newJob)
    }
}
