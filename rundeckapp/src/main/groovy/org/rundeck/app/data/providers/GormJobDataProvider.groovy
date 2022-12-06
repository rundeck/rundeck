package org.rundeck.app.data.providers


import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import org.rundeck.app.data.exception.DataValidationException
import rundeck.data.job.RdJob
import org.rundeck.app.data.job.converters.ScheduledExecutionFromRdJobUpdater
import org.rundeck.app.data.job.converters.ScheduledExecutionToJobConverter
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.providers.v1.job.JobDataProvider
import org.rundeck.spi.data.DataAccessException
import org.springframework.beans.factory.annotation.Autowired
import rundeck.ScheduledExecution
import rundeck.services.data.ScheduledExecutionDataService

@GrailsCompileStatic
@Transactional
class GormJobDataProvider implements JobDataProvider {

    @Autowired
    ScheduledExecutionDataService scheduledExecutionDataService

    @Override
    JobData get(Serializable id) {
        ScheduledExecution se = scheduledExecutionDataService.get(id)
        return se ? ScheduledExecutionToJobConverter.convert(se) : null
    }

    @Override
    JobData findByUuid(String uuid) {
        return ScheduledExecutionToJobConverter.convert(scheduledExecutionDataService.findByUuid(uuid))
    }

    @Override
    boolean existsByUuid(String uuid) {
        return scheduledExecutionDataService.countByUuid(uuid) == 1
    }

    @Override
    boolean existsByProjectAndJobNameAndGroupPath(String project, String jobName, String groupPath) {
        return ScheduledExecution.countByProjectAndJobNameAndGroupPath(project, jobName, groupPath) == 1
    }

    @Override
    JobData save(JobData job) throws DataAccessException {
        ScheduledExecution se = job.id ? scheduledExecutionDataService.get(job.id) : new ScheduledExecution()
        RdJob rdJob = (RdJob)job
        if(!rdJob.validate()) {
            rdJob.errors.allErrors.each { err ->
                println err.toString()
            }
            throw new DataValidationException(rdJob)
        }
        ScheduledExecutionFromRdJobUpdater.update(se, rdJob)
        return ScheduledExecutionToJobConverter.convert(se.save(failOnError:true, flush: true))
    }

    @Override
    void delete(Serializable id) throws DataAccessException {
        ScheduledExecution se = ScheduledExecution.get(id)
        se?.delete()
    }

    @Override
    void deleteByUuid(String uuid) throws DataAccessException {
        ScheduledExecution.findByUuid(uuid)?.delete()
    }
}
