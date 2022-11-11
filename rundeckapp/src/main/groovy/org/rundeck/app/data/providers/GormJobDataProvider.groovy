package org.rundeck.app.data.providers


import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import org.rundeck.app.data.job.RdJob
import org.rundeck.app.data.job.ScheduledExecutionFromRdJobUpdater
import org.rundeck.app.data.job.ScheduledExecutionToJobConverter
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.providers.v1.job.JobDataProvider
import org.rundeck.spi.data.DataAccessException
import rundeck.ScheduledExecution
import rundeck.services.data.ScheduledExecutionDataService

@GrailsCompileStatic
@Transactional
class GormJobDataProvider implements JobDataProvider {

    ScheduledExecutionDataService scheduledExecutionDataService

    @Override
    JobData get(Serializable id) {
        ScheduledExecution se = scheduledExecutionDataService.get(id)
        return ScheduledExecutionToJobConverter.convert(se)
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
    JobData save(JobData job) throws DataAccessException {
        ScheduledExecution se = job.id ? scheduledExecutionDataService.get(job.id) : new ScheduledExecution()
        ScheduledExecutionFromRdJobUpdater.update(se, (RdJob)job)
        return ScheduledExecutionToJobConverter.convert(se.save(failOnError:true, flush: true))
    }

    @Override
    void delete(Serializable id) throws DataAccessException {
        ScheduledExecution se = ScheduledExecution.get(id)
        se.delete()
    }
}
