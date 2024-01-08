package rundeck.data.job.query

import groovy.transform.CompileStatic
import org.rundeck.app.data.model.v1.query.JobBrowseInput

@CompileStatic
class RdJobBrowseInput implements JobBrowseInput{
    String project
    String path
}
