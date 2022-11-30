package rundeck.services

import org.rundeck.app.data.model.v1.job.JobData
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

    void delete(String id) {
        if(id.length() == 36) jobDataProvider.deleteByUuid(id)
        else jobDataProvider.delete(id.toLong())
    }
}
