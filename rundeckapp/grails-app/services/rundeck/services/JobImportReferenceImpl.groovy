package rundeck.services

import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.plugins.scm.JobImportReference
import com.dtolabs.rundeck.plugins.scm.JobScmReference
import com.dtolabs.rundeck.plugins.scm.JobSerializer

/**
 * Created by greg on 9/14/15.
 */
class JobImportReferenceImpl extends JobReferenceImpl implements JobScmReference {
    Long version
    Map scmImportMetadata
    Long importVersion
    JobSerializer jobSerializer

    JobImportReferenceImpl(final JobRevReference job, final long importVersion, final Map scmImportMetadata) {
        this.id=job.id
        this.groupPath=job.groupPath
        this.jobName=job.jobName
        this.version=job.version
        this.project=job.project
        this.scmImportMetadata = scmImportMetadata
        this.importVersion = importVersion
    }
}
