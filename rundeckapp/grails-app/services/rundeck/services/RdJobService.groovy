package rundeck.services

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.rundeck.app.data.job.converters.ScheduledExecutionToJobConverter
import org.rundeck.app.data.model.v1.DeletionResult
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.model.v1.query.JobQueryInputData
import org.rundeck.app.data.providers.v1.job.JobDataProvider
import rundeck.ScheduledExecution
import rundeck.data.job.RdJob

@CompileStatic
class RdJobService {

    JobDataProvider jobDataProvider

    /**
     * @deprecated This method allows the lookup of ScheduledExecution by either the internal id or the uuid of the
     * job. This behavior was introduced when uuids were added to mark jobs. Now ALL jobs should have uuids, so
     * use the getJobByUuid method explicitly and do not rely on this behavior
     * @param id
     * @return
     */
    @Deprecated
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

    @CompileDynamic
    RdJob convertToRdJob(JobData job) {
        if(job instanceof ScheduledExecution) return ScheduledExecutionToJobConverter.convert(job)
        return job
    }

    JobData getJobById(Long id) {
        return jobDataProvider.get(id)
    }

    JobData getJobByUuid(String uuid) {
        return jobDataProvider.findByUuid(uuid)
    }

    boolean existsByUuid(String uuid) {
        return jobDataProvider.existsByUuid(uuid)
    }

    JobData saveJob(JobData job) {
        //run before save processes
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
