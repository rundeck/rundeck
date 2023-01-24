package rundeck.services

import org.rundeck.app.data.model.v1.DeletionResult
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.model.v1.query.JobQueryInputData
import org.rundeck.app.data.providers.v1.job.JobDataProvider

class RdJobService {

    JobDataProvider jobDataProvider

    JobData getJobById(Long id) {
        return jobDataProvider.get(id)
    }

    JobData getJobByUuid(String uuid) {
        return jobDataProvider.findByUuid(uuid)
    }

    JobData saveJob(JobData job) {
        return jobDataProvider.save(job)
    }

    DeletionResult delete(String id) {
        try {
            return jobDataProvider.delete(id.toLong())
        } catch(NumberFormatException ignored){}
        return jobDataProvider.deleteByUuid(id)
    }

    def listJobs(JobQueryInputData jobQueryInputData) {
        jobDataProvider.queryJobs(jobQueryInputData)
    }
}
