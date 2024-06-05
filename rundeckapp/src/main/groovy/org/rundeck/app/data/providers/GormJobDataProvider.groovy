package org.rundeck.app.data.providers

import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Log4j2
import org.rundeck.app.data.job.converters.ScheduledExecutionFromRdJobUpdater
import org.rundeck.app.data.job.converters.ScheduledExecutionToJobConverter
import org.rundeck.app.data.model.v1.DeletionResult
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.model.v1.job.JobDataSummary
import org.rundeck.app.data.providers.v1.job.JobDataProvider
import org.rundeck.spi.data.DataAccessException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.request.RequestContextHolder
import rundeck.ScheduledExecution
import rundeck.services.FrameworkService
import rundeck.services.ScheduledExecutionService
import rundeck.services.data.ScheduledExecutionDataService

import javax.persistence.EntityNotFoundException

@GrailsCompileStatic
@Transactional
@Log4j2
class GormJobDataProvider extends GormJobQueryProvider implements JobDataProvider {

    @Autowired
    ScheduledExecutionDataService scheduledExecutionDataService
    @Autowired
    FrameworkService frameworkService
    @Autowired
    ScheduledExecutionService scheduledExecutionService

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
    @CompileDynamic
    Optional<JobDataSummary> findBasicByUuid(String uuid) {
        def crit = createCriteria()
        def scheduled = crit.get{
            eq("uuid", uuid)
            projections{
                for (String key : PROJECTION_KEYS) {
                    property key
                }
            }
        }
        if(!scheduled) {
            return Optional.empty()
        }
        else{
            return Optional.of(
                summaryFromProjection(scheduled)
            )
        }
    }

    @Override
    boolean existsByUuid(String uuid) {
        return scheduledExecutionDataService.countByUuid(uuid) == 1
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    boolean existsByProjectAndJobNameAndGroupPath(String project, String jobName, String groupPath) {
        return ScheduledExecution.countByProjectAndJobNameAndGroupPath(project, jobName, groupPath) == 1
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    @Override
    JobData save(JobData job) throws DataAccessException {
        ScheduledExecution se = job.uuid ? scheduledExecutionDataService.findByUuid(job.uuid) : null
        if(!se) {
            se = new ScheduledExecution()
        }
        ScheduledExecutionFromRdJobUpdater.update(se, job)
        def saved = se.save(failOnError:true, flush: true)
        return saved
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    def getSession() {
        RequestContextHolder.currentRequestAttributes().getSession()
    }

    @Override
    DeletionResult delete(Serializable id) throws DataAccessException {
        _deleteJob(ScheduledExecution.get(id))
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    DeletionResult deleteByUuid(String uuid) throws DataAccessException {
        _deleteJob(ScheduledExecution.findByUuid(uuid))
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    DeletionResult _deleteJob(ScheduledExecution se) {
        if(!se) throw new EntityNotFoundException("Job not found")
        def authCtx = frameworkService.userAuthContext(getSession())
        return scheduledExecutionService.deleteScheduledExecution(se,false,authCtx,authCtx.username)
    }

}
