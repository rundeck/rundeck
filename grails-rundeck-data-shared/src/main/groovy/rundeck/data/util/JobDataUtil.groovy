package rundeck.data.util

import com.dtolabs.rundeck.core.jobs.JobReference
import org.rundeck.app.data.model.v1.job.JobData
import rundeck.data.job.reference.JobReferenceImpl

class JobDataUtil {
    static JobReference asJobReference(JobData job) {
        return new JobReferenceImpl(id: job.uuid,
                jobName: job.jobName,
                groupPath: job.groupPath,
                project: job.project,
                serverUUID: job.serverNodeUUID)
    }
}
