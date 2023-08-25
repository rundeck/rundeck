package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import groovy.transform.CompileStatic
import org.rundeck.app.data.exception.DataValidationException
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.access.UnauthorizedAccess
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.web.RdAuthorizeJob
import rundeck.data.job.RdJob
import org.rundeck.app.data.validation.ValidationResponse
import org.springframework.context.MessageSource
import rundeck.data.job.query.RdJobQueryInput
import rundeck.services.RdJobService

import javax.persistence.EntityNotFoundException
import javax.security.auth.Subject

class RdJobController extends ControllerBase {

    MessageSource messageSource
    RdJobService rdJobService
    ObjectMapper mapper = new ObjectMapper()

    RdJobController() {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    def editpage() {}

    @RdAuthorizeJob(RundeckAccess.Job.AUTH_APP_READ_OR_VIEW)
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
            AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject((Subject)session.getAttribute("subject"))
            if (!rundeckAuthContextProcessor.authorizeProjectResourceAny(
                    authContext,
                    AuthConstants.RESOURCE_TYPE_JOB,
                    [AuthConstants.ACTION_CREATE, AuthConstants.ACTION_UPDATE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN],
                    job.project
            )) {
                throw new UnauthorizedAccess("create or update", AuthConstants.TYPE_JOB, job.uuid)
            }
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
        def job = rdJobService.getJobByUuid(params.id)
        if(!job) {
            response.status = 404
            return
        }
        try {
            AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject((Subject)session.getAttribute("subject"))
            if (!rundeckAuthContextProcessor.authorizeProjectResourceAny(
                    authContext,
                    AuthConstants.RESOURCE_TYPE_JOB,
                    [AuthConstants.ACTION_DELETE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN],
                    job.project
            )) {
                throw new UnauthorizedAccess(AuthConstants.ACTION_DELETE, AuthConstants.TYPE_JOB, job.uuid)
            }
            render mapper.writeValueAsString(rdJobService.delete(job.uuid))
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
