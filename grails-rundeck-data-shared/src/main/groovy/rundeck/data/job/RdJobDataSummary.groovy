package rundeck.data.job;

import org.rundeck.app.data.model.v1.job.JobDataSummary
import rundeck.data.util.JobDataUtil

class RdJobDataSummary implements JobDataSummary {
    String uuid
    String jobName
    String groupPath
    String project
    String description
    String serverNodeUUID
    Boolean scheduled
    Boolean scheduleEnabled
    Boolean executionEnabled

    @Override
    String toString() {
        return JobDataUtil.generateFullName(groupPath,jobName) + " - ${description}"
    }
}
