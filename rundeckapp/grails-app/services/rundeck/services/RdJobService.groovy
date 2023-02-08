package rundeck.services

import groovy.transform.CompileStatic
import org.rundeck.app.data.model.v1.DeletionResult
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.model.v1.query.JobQueryInputData
import org.rundeck.app.data.providers.v1.job.JobDataProvider

@CompileStatic
class RdJobService {

    JobDataProvider jobDataProvider

    JobData getJobByIdOrUuid(Serializable id) {
        JobData found = null
        if (id instanceof Long) {
            return getJobById(id)
        } else if (id instanceof String) {
            //attempt to parse as long id
            try {
                def idlong = Long.parseLong(id)
                found = getJobById(idlong)
            } catch (NumberFormatException e) {
            }
            if (!found) {
                found = getJobByUuid(id)
            }
        }
        return found
    }

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
