package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import groovy.transform.CompileStatic
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.data.exception.DataValidationException
import org.rundeck.app.data.validation.ValidationResponse
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.web.RdAuthorizeExecution
import org.rundeck.core.auth.web.RdAuthorizeProject
import org.springframework.context.MessageSource
import rundeck.Execution
import rundeck.data.execution.RdExecution
import rundeck.data.job.RdJob
import rundeck.services.RdExecutionService

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

    @CompileStatic
    def save() {
        response.contentType = "application/json;utf-8"

        try {
            RdExecution execution = mapper.readValue(request.inputStream, RdExecution)
            render mapper.writeValueAsString(rdExecutionService.saveExecutionData(execution))
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
            renderUnauthorized("Unauthorized API call")
        }
        render mapper.writeValueAsString(rdExecutionService.delete(params.id))
    }

}
