package org.rundeck.app.data.providers

import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Log4j2
import org.rundeck.app.data.exception.DataValidationException
import org.rundeck.app.data.job.converters.ExecutionFromRdExecutionUpdater
import org.rundeck.app.data.job.converters.ExecutionToRdExecutionConverter
import org.rundeck.app.data.model.v1.DeletionResult
import org.rundeck.app.data.model.v1.execution.ExecutionData
import org.rundeck.app.data.providers.v1.execution.ExecutionDataProvider
import org.rundeck.spi.data.DataAccessException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.request.RequestContextHolder
import rundeck.Execution
import rundeck.data.execution.RdExecution
import rundeck.services.ExecutionService
import rundeck.services.FrameworkService

@Transactional
@Log4j2
class GormExecutionDataProvider implements ExecutionDataProvider {

    @Autowired
    ExecutionService executionService
    @Autowired
    FrameworkService frameworkService

    @Override
    ExecutionData get(Serializable id) {
        return ExecutionToRdExecutionConverter.convert(Execution.get(id))
    }

    @Override
    ExecutionData getByUuid(String uuid) {
        return ExecutionToRdExecutionConverter.convert(Execution.findByUuid(uuid))
    }

    @Override
    ExecutionData save(ExecutionData edata) throws DataAccessException {
        Execution e = edata.uuid ? Execution.findByUuid(edata.uuid) : null
        if(!e) {
            e = new Execution()
        }
        RdExecution rdExec = (RdExecution)edata
        if(!rdExec.validate()) {
            throw new DataValidationException(rdExec)
        }
        ExecutionFromRdExecutionUpdater.update(e, rdExec)
        return ExecutionToRdExecutionConverter.convert(e.save(failOnError:true, flush: true))
    }

    @Override
    DeletionResult delete(String uuid) {
        DeleteExecutionResult result = new DeleteExecutionResult()
        result.id = uuid
        def e = Execution.findByUuid(uuid)
        if(!e) {
            result.error = "Execution with id: ${uuid} not found"
            return
        }
        try {
            def authCtx = frameworkService.userAuthContext(getSession())
            def _result = executionService.deleteExecution(e, authCtx, authCtx.username)
            result.success = _result.success
            result.error = _result.error
        } catch(Exception ex) {
            log.error("Execution Deletion Failed", ex)
            result.error = "Failed to delete execution."
        }
        return result
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    def getSession() {
        RequestContextHolder.currentRequestAttributes().getSession()
    }

    static class DeleteExecutionResult implements DeletionResult {
        String dataType = "Execution"
        String id
        boolean success
        String error
    }
}
