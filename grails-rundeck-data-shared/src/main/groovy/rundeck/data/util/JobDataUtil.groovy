package rundeck.data.util

import com.dtolabs.rundeck.core.jobs.JobReference;
import org.rundeck.app.data.model.v1.job.JobData
import rundeck.data.job.JobReferenceImpl;

class JobDataUtil {

    static String getExtId(JobData jobData) {
        return jobData?.getUuid()?:jobData?.getId()?.toString()
    }

    static String generateFullName(JobData job) {
        return [job.groupPath?:'',job.jobName].join("/")
    }

    static String generateJobScheduledName(JobData job) {
        return [job.id,job.jobName].join(":")
    }

    static String generateJobGroupName(JobData job) {
        return [job.project, job.jobName,job.groupPath?job.groupPath: ''].join(":")
    }

    static JobReference asJobReference(JobData job) {
        return new JobReferenceImpl(id: getExtId(job),
                jobName: job.jobName,
                groupPath: job.groupPath,
                project: job.project,
                serverUUID: job.serverNodeUUID)
    }

    static long getTimeoutDuration(JobData job){
        job.timeout? Sizes.parseTimeDuration(job.timeout):-1
    }

    static boolean hasNodesSelectedByDefault(JobData job) {
        null == job.nodeConfig?.nodesSelectedByDefault || job.nodeConfig?.nodesSelectedByDefault
    }

    static boolean shouldScheduleExecution(JobData job) {
        return job.scheduled && hasExecutionEnabled(job) && hasScheduleEnabled(job)
    }

    static boolean hasScheduleEnabled(JobData job) {
        return (null == job.scheduleEnabled || job.scheduleEnabled)
    }

    static boolean hasExecutionEnabled(JobData job) {
        return (null == job.executionEnabled || job.executionEnabled)
    }

    static boolean hasSecureOptions(JobData job) {
        return !job.optionsSet ? false : job.optionSet?.any {
            it.secureInput || it.secureExposed
        }
    }
}
