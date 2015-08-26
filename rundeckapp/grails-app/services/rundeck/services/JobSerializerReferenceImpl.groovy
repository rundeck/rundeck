package rundeck.services

import com.dtolabs.rundeck.core.jobs.JobExportReference
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.plugins.scm.JobSerializer
import com.dtolabs.rundeck.plugins.scm.ScmPlugin

/**
 * Created by greg on 4/28/15.
 */
class JobSerializerReferenceImpl extends JobReferenceImpl implements JobExportReference{
    JobSerializer jobSerializer
    Long version

    JobSerializerReferenceImpl(
            JobRevReference ref,
            JobSerializer jobSerializer
    )
    {
        this.id=ref.id
        this.groupPath=ref.groupPath
        this.jobName=ref.jobName
        this.project=ref.project
        this.version=ref.version
        this.jobSerializer=jobSerializer
    }

}
