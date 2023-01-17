package rundeck.controllers

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import org.rundeck.app.data.exception.DataValidationException
import org.rundeck.app.execution.workflow.WorkflowExecutionItemFactory
import rundeck.data.job.RdJob
import org.rundeck.app.data.validation.ValidationResponse
import org.springframework.context.MessageSource
import rundeck.ScheduledExecution
import rundeck.services.ExecutionUtilService
import rundeck.services.RdJobService

class RdJobController {

    MessageSource messageSource
    RdJobService rdJobService
    WorkflowExecutionItemFactory workflowExecutionItemFactory
    ObjectMapper mapper = new ObjectMapper()

    RdJobController() {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    def editpage() {}

    def get() {
        response.contentType = "application/json;utf-8"
        try {
            def uuid = UUID.fromString(params.id)
            render mapper.writeValueAsString(rdJobService.getJobByUuid(uuid.toString()))
            return
        } catch(IllegalArgumentException ignored) {
            println "oops"
        }

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
        def newwei = workflowExecutionItemFactory.createExecutionItemForWorkflow(rdjob.workflow)
        def oldwei = executionUtilService.createExecutionItemForWorkflow(se.workflow)
        response.contentType = "application/json;utf-8"
        render mapper.writeValueAsString([newwei: newwei, oldwei: oldwei])
    }
}
