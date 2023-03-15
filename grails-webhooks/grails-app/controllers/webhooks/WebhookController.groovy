package webhooks

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthContextEvaluator
import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.webhook.WebhookEventException
import com.dtolabs.rundeck.plugins.webhook.WebhookDataImpl
import grails.converters.JSON
import groovy.transform.PackageScope
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.rundeck.app.data.model.v1.webhook.RdWebhook
import org.rundeck.core.auth.AuthConstants
import webhooks.authenticator.AuthorizationHeaderAuthenticator

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
class WebhookController {
    static final String AUTH_HEADER = "Authorization"
    static final String FORM_URLENCODED = "application/x-www-form-urlencoded"
    static allowedMethods = [post:'POST']

    def webhookService
    AuthContextEvaluator rundeckAuthContextEvaluator
    AuthContextProvider rundeckAuthContextProvider
    def apiService

    def admin() {}

    def save() {
        String project = request.JSON.project
        if(!project){
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                                                           code: 'api.error.parameter.required', args: ['project']])

        }
        UserAndRolesAuthContext authContext = rundeckAuthContextProvider.getAuthContextForSubjectAndProject(session.subject, project)
        if (!rundeckAuthContextEvaluator.authorizeProjectResourceAny(
            authContext,
            AuthConstants.RESOURCE_TYPE_WEBHOOK,
            [AuthConstants.ACTION_CREATE, AuthConstants.ACTION_UPDATE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN],
            project
        )) {
            sendJsonError("You are not authorized to perform this action")
            return
        }

        def msg = webhookService.saveHook(authContext,request.JSON)
        if(msg.err) response.status = 400

        render msg as JSON
    }

    def remove() {
        if(!params.project){
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                                                           code: 'api.error.parameter.required', args: ['project']])

        }
        RdWebhook webhook = webhookService.getWebhook(params.id.toLong())
        if(!webhook) {
            sendJsonError("Webhook not found")
            return
        }

        UserAndRolesAuthContext authContext = rundeckAuthContextProvider.getAuthContextForSubjectAndProject(session.subject, params.project)
        if (!authorized(authContext, webhook.project, AuthConstants.ACTION_DELETE)) {
            sendJsonError("You are not authorized to perform this action")
            return
        }
        def output = webhookService.delete(webhook)
        if(output.err) response.status = 400
        render output as JSON

    }

    def get() {
        if(!params.project){
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                                                           code: 'api.error.parameter.required', args: ['project']])

        }
        if(!params.id){
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                                                           code: 'api.error.parameter.required', args: ['id']])

        }
        UserAndRolesAuthContext authContext = rundeckAuthContextProvider.getAuthContextForSubjectAndProject(session.subject,params.project)
        if (!authorized(authContext, params.project, AuthConstants.ACTION_READ)) {
            sendJsonError("You do not have access to this resource")
            return
        }
        def webhook = webhookService.getWebhookForProjectWithAuth(params.id, params.project)
        if(!webhook){
            return sendJsonError("Webhook not found", HttpServletResponse.SC_NOT_FOUND)
        }
        render webhook as JSON
    }

    @Get(uri="/project/{project}/webhooks")
    @Operation(
        method="GET",
        summary="List Project Webhooks",
        description="""List the webhooks for the project""",
        tags = "webhook",
        responses=@ApiResponse(
            responseCode="200",
            description="List of webhooks",
            content=@Content(
                mediaType = 'application/json',
                array = @ArraySchema(
                    schema = @Schema(type="object")
                ),
                examples = @ExampleObject('''[
    {
        "authToken": "Z1vnbhShhQF3B0dQq7UhJTZMnGS92TBl",
        "config": {
            "argString": "-payload \${raw}",
            "jobId": "a54d07a1-033a-499f-9789-19bcacbd6e11"
        },
        "creator": "admin",
        "enabled": true,
        "eventPlugin": "webhook-run-job",
        "id": 3,
        "name": "Webhook Job Runner",
        "project": "Webhook",
        "roles": "admin,user",
        "user": "admin"
    },
    {
        "authToken": "p9ttreh05Zd222g5yBXocEMXmCJ1skOX",
        "config": {},
        "creator": "admin",
        "enabled": true,
        "eventPlugin": "log-webhook-event",
        "id": 4,
        "name": "Log it Hook",
        "project": "Webhook",
        "roles": "admin,user",
        "user": "admin"
    }
]''')
            )
        )
    )
    def list() {
        if(!params.project){
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                                                           code: 'api.error.parameter.required', args: ['project']])

        }
        UserAndRolesAuthContext authContext = rundeckAuthContextProvider.getAuthContextForSubjectAndProject(session.subject, params.project)
        if (!authorized(authContext, params.project, AuthConstants.ACTION_READ)) {
            sendJsonError("You do not have access to this resource")
            return
        }
        render webhookService.listWebhooksByProject(params.project) as JSON
    }

    def editorData() {
        if(!params.project){
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                                                           code: 'api.error.parameter.required', args: ['project']])

        }
        UserAndRolesAuthContext authContext = rundeckAuthContextProvider.getAuthContextForSubjectAndProject(session.subject, params.project)
        if (!authorized(authContext, params.project, AuthConstants.ACTION_READ)) {
            sendJsonError("You do not have access to this resource")
            return
        }

        def uidata = [:]
        uidata.hooks = webhookService.listWebhooksByProject(params.project)
        uidata.username = authContext.username
        uidata.roles = authContext.roles.join(",")
        render uidata as JSON
    }

    def post() {
        RdWebhook hook = webhookService.getWebhookByToken(Webhook.cleanAuthToken(params.authtoken))

        if(!hook) {
            sendJsonError("Webhook not found")
            return
        }
        if(!hook.enabled) {
            sendJsonError("Webhook not enabled",503)
            return
        }

        UserAndRolesAuthContext authContext = rundeckAuthContextProvider.getAuthContextForSubjectAndProject(session.subject, hook.project)
        if (!authorized(authContext, hook.project, AuthConstants.ACTION_POST)) {
            sendJsonError("You are not authorized to perform this action")
            return
        }

        if(hook.authConfigJson && !authorizeWebhook(hook, request)) {
            sendJsonError("Failed webhook authorization")
            return
        }

        WebhookDataImpl whkdata = new WebhookDataImpl()
        whkdata.webhookUUID = hook.uuid
        whkdata.webhook = hook.name
        whkdata.timestamp = System.currentTimeMillis()
        whkdata.sender = request.remoteAddr
        whkdata.project = hook.project
        whkdata.contentType = request.contentType
        if(FORM_URLENCODED == request.contentType) {
            whkdata.formData = cleanedFormDataFromParams(params)
        } else {
            whkdata.data = request.inputStream
        }

        try {
            def responder = webhookService.processWebhook(hook.eventPlugin, hook.pluginConfigurationJson, whkdata, authContext, request)
            responder.respond(response)
        } catch(WebhookEventException wee) {
            sendJsonError(wee.message)
        }
    }
    static final def KEYS_TO_CLEAN = ["controller","action","authtoken","api_version"]

    static Map cleanedFormDataFromParams(def params) {
        def cleanMap = new HashMap(params)
        cleanMap.removeAll( e -> KEYS_TO_CLEAN.contains(e.key))
        return cleanMap
    }

    private def sendJsonError(String errMessage,int statusCode = 400) {
        response.setStatus(statusCode)
        def err = [err:errMessage]
        render err as JSON
    }

    @PackageScope
    boolean authorized(AuthContext authContext, String project, String action) {
        List authorizedActions = [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]
        if(action != AuthConstants.ACTION_ADMIN) authorizedActions.add(action)
        rundeckAuthContextEvaluator.authorizeProjectResourceAny(authContext,AuthConstants.RESOURCE_TYPE_WEBHOOK,authorizedActions,project)
    }

    @PackageScope
    boolean authorizeWebhook(RdWebhook hook, HttpServletRequest request) {
        return new AuthorizationHeaderAuthenticator().authenticate(hook, request)

    }
}
