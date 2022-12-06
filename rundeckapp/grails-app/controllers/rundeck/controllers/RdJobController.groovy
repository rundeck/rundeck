package rundeck.controllers

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import groovy.transform.CompileStatic
import org.rundeck.app.data.exception.DataValidationException
import rundeck.data.job.RdJob
import org.rundeck.app.data.validation.ValidationResponse
import org.springframework.context.MessageSource
import rundeck.data.job.query.RdJobQueryInput
import rundeck.services.RdJobService

import javax.persistence.EntityNotFoundException

class RdJobController extends ControllerBase {

    MessageSource messageSource
    RdJobService rdJobService
    ObjectMapper mapper = new ObjectMapper()

    RdJobController() {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    def editpage() {}

    def get() {
        response.contentType = "application/json;utf-8"
        def job = rdJobService.getJobByIdOrUuid(params.id)
        if(job) {
            render mapper.writeValueAsString(job)
        } else {
            response.status = 404
        }

    }

    @CompileStatic
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
        try {
            render mapper.writeValueAsString(rdJobService.delete(params.id))
        } catch(EntityNotFoundException ex) {
            response.status = 404
            render mapper.writeValueAsString(["err":ex.message])
        }
    }

    def query(RdJobQueryInput qry) {
        response.contentType = "application/json;utf-8"
        qry.inputParamMap = params
        render mapper.writeValueAsString(rdJobService.listJobs(qry))
    }
}
