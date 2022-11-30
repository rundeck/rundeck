package rundeck.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import org.rundeck.app.data.exception.DataValidationException
import org.rundeck.app.data.job.RdJob
import org.rundeck.app.data.job.converters.WorkflowExecutionItemFactory
import org.rundeck.app.data.validation.ValidationResponse
import org.springframework.context.MessageSource
import rundeck.ScheduledExecution
import rundeck.services.ExecutionUtilService
import rundeck.services.RdJobService

class RdJobController {

    MessageSource messageSource
    RdJobService rdJobService
    ObjectMapper mapper = new ObjectMapper()

    def get() {
        response.contentType = "application/json;utf-8"
        println "lookup id: ${params.id}"
        if(params.id.toString().length() == 36)
            render mapper.writeValueAsString(rdJobService.getJobByUuid(params.id))
        else
            render mapper.writeValueAsString(rdJobService.getJobById(params.id.toLong()))
    }

    def save() {
        response.contentType = "application/json;utf-8"

        try {
            RdJob job = mapper.readValue(request.inputStream, RdJob)
            render mapper.writeValueAsString(rdJobService.saveJob(job))
        } catch(InvalidFormatException ife) {
            response.status = 400
            render mapper.writeValueAsString([error: ife.message])
        } catch(DataValidationException dvex) {
            response.status = 400
            render mapper.writeValueAsString(new ValidationResponse().createFrom(messageSource, dvex.target))
        }

    }

    def delete() {
        response.contentType = "application/json;utf-8"
        rdJobService.delete(params.id)
        render mapper.writeValueAsString(["msg":"deleted"])
    }

    ExecutionUtilService executionUtilService

    def compare() {
        def rdjob = rdJobService.getJobByUuid(params.id)
        def se = ScheduledExecution.findByUuid(params.id)
        def newwei = WorkflowExecutionItemFactory.createExecutionItemForWorkflow(rdjob.workflow)
        def oldwei = executionUtilService.createExecutionItemForWorkflow(se.workflow)
        response.contentType = "application/json;utf-8"
        render mapper.writeValueAsString([newwei: newwei, oldwei: oldwei])
    }
}
