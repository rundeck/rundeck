package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import grails.compiler.GrailsCompileStatic
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.data.exception.DataValidationException
import org.rundeck.app.data.validation.ValidationResponse
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.access.UnauthorizedAccess
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.web.RdAuthorizeExecution
import org.rundeck.core.auth.web.RdAuthorizeJob
import org.springframework.context.MessageSource
import rundeck.data.execution.RdExecution
import rundeck.data.paging.RdPageable
import rundeck.services.RdExecutionService

import javax.security.auth.Subject

class RdExecutionController extends ControllerBase {

    static ObjectMapper mapper = new ObjectMapper()
    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    AppAuthContextProcessor rundeckAuthContextProcessor
    RdExecutionService rdExecutionService
    MessageSource messageSource

    @RdAuthorizeExecution(RundeckAccess.Execution.AUTH_APP_READ_OR_VIEW)
    def get() {
        response.contentType = "application/json;utf-8"
        def job = rdExecutionService.getExecutionByUuid(params.id)
        if(job) {
            render mapper.writeValueAsString(job)
        } else {
            response.status = 404
        }
    }

    def delete() {
        response.contentType = "application/json;utf-8"
        def e = rdExecutionService.getExecutionByUuid(params.id)
        if(!e) {
            response.status = 404
            return
        }
        AuthContext authContext = rundeckAuthContextProcessor.getAuthContextForSubject(session.subject)
        if (!rundeckAuthContextProcessor.authorizeApplicationResourceAny(
                authContext,
                rundeckAuthContextProcessor.authResourceForProject(e.project),
                [AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]
        )) {
            throw new UnauthorizedAccess(AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.TYPE_EXECUTION, e.uuid)
        }
        render mapper.writeValueAsString(rdExecutionService.delete(params.id))
    }

    @RdAuthorizeJob(RundeckAccess.Job.AUTH_APP_READ_OR_VIEW)
    def listForJob() {
        response.contentType = "application/json;utf-8"
        RdPageable pageable = new RdPageable()
        if(params.max) pageable.max = params.max.toInteger()
        if(params.offset) pageable.offset = params.offset.toInteger()
        render mapper.writeValueAsString(rdExecutionService.listExecutionsForJob(params.id, pageable))
    }

}
