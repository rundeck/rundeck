package rundeck.data.util

import com.dtolabs.rundeck.core.jobs.JobReference
import groovy.transform.CompileStatic
import org.quartz.JobKey;
import org.rundeck.app.data.model.v1.job.JobData
import rundeck.data.job.JobReferenceImpl;

@CompileStatic
class JobDataUtil {

    static String getExtId(JobData jobData) {
        return jobData?.getUuid()
    }

    static String generateFullName(JobData job) {
        return generateFullName(job.groupPath, job.jobName)
    }

    static String generateFullName(String group, String jobname) {
        return [group?:'',jobname].join("/")
    }

    static String generateJobScheduledName(JobData job) {
        if(!job) return null
        return [job.uuid,job.jobName].join(":")
    }

    static String generateJobGroupName(JobData job) {
        if(!job) return null
        return [job.project, job.jobName,job.groupPath?job.groupPath: ''].join(":")
    }

    static JobKey createJobKeyFromJob(JobData job) {
        if(!job) return null
        return JobKey.jobKey(JobDataUtil.generateJobScheduledName(job), JobDataUtil.generateJobGroupName(job))
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
        return !job.optionSet ? false : job.optionSet?.any {
            it.secureInput || it.secureExposed
        }
    }

    static String generateCrontabExpression(JobData jobData) {
        return [jobData.schedule?.seconds?:'0',jobData.schedule?.minute?:'0',jobData.schedule?.hour?:'0',jobData.schedule?.dayOfMonth?.toUpperCase()?:'?',jobData.schedule?.month?.toUpperCase() ?:'*',!jobData.schedule?.dayOfMonth||jobData.schedule?.dayOfMonth=='?'?(jobData.schedule?.dayOfWeek?.toUpperCase()?:'*'):'?',jobData.schedule?.year?:'*'].join(" ")
    }
}
