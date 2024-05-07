package rundeck.data.job

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.rundeck.app.data.model.v1.job.JobBrowseItem
import org.rundeck.app.data.model.v1.job.JobDataSummary

@CompileStatic
@ToString
class RdJobBrowseItem implements JobBrowseItem {
    boolean job
    String groupPath
    JobDataSummary jobData
}
