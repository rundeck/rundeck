package rundeck.data.job

import groovy.transform.CompileStatic
import groovy.transform.ToString;
import org.rundeck.app.data.model.v1.job.JobDataSummary

@CompileStatic
@ToString
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
}
