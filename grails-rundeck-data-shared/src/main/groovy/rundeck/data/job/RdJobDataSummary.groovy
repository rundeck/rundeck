package rundeck.data.job;

import org.rundeck.app.data.model.v1.job.JobDataSummary

class RdJobDataSummary implements JobDataSummary {
    Long id
    String uuid
    String jobName
    String groupPath
    String project
    String description
    String serverNodeUUID
    Boolean scheduled
    Boolean scheduleEnabled
    Boolean executionEnabled
}
