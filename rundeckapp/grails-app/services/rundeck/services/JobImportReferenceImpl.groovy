package rundeck.services

import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.plugins.scm.JobImportReference

/**
 * Created by greg on 9/14/15.
 */
class JobImportReferenceImpl extends JobReferenceImpl implements JobImportReference {
    Long version
    Map scmImportMetadata

    JobImportReferenceImpl(final JobRevReference job, final Map scmImportMetadata) {
        this.id=job.id
        this.groupPath=job.groupPath
        this.jobName=job.jobName
        this.version=job.version
        this.project=job.project
        this.scmImportMetadata = scmImportMetadata
    }
}
