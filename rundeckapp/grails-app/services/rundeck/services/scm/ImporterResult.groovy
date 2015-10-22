package rundeck.services.scm

import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.plugins.scm.ImportResult
import com.dtolabs.rundeck.plugins.scm.JobScmReference

/**
 * Created by greg on 10/2/15.
 */
class ImporterResult implements ImportResult {
    boolean successful
    String errorMessage
    JobScmReference job
    boolean created
    boolean modified

    static ImportResult fail(String message) {
        def result = new ImporterResult()
        result.successful = false
        result.errorMessage = message
        return result
    }

    @Override
    public String toString() {
        if (!successful) {
            return "Failed: " + errorMessage
        }
        return (created ? "Created " : "Modified ") + "Job: " + formatJob();
    }

    private String formatJob() {
        '{{Job ' + job.id + '}}'
    }
}
