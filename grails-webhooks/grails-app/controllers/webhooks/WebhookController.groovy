package webhooks

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthContextEvaluator
import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.webhook.WebhookEventException
import com.dtolabs.rundeck.plugins.webhook.WebhookDataImpl
import grails.converters.JSON
import groovy.transform.PackageScope
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.rundeck.app.data.model.v1.webhook.RdWebhook
import org.rundeck.core.auth.AuthConstants
import webhooks.authenticator.AuthorizationHeaderAuthenticator

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
@SecurityScheme(
    name = "webhookTokenHeader",
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.HEADER,
    paramName = "Authorization",
    description = "Webhook Event Authorization Security header requires the `Authorization` to match the secret value."
)
class WebhookController {
    static final String AUTH_HEADER = "Authorization"
    static final String FORM_URLENCODED = "application/x-www-form-urlencoded"
    static allowedMethods = [post:'POST']

    def webhookService
    AuthContextEvaluator rundeckAuthContextEvaluator
    AuthContextProvider rundeckAuthContextProvider
    def apiService

    def admin() {}

    @Post(uri = "/project/{project}/webhook/{id}")
    @Operation(
        method = "POST",
        summary = "Update A Webhook",
        description = '''Updates the specified webhook.

Since: v33''',
        tags = "webhook",
        parameters = [
            @Parameter(name = "project", in = ParameterIn
                .PATH, description = "Project Name", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "id", in = ParameterIn
                .PATH, description = "Webhook ID", required = true, schema = @Schema(type = "string"))
        ],
        requestBody = @RequestBody(
            description = '''Updated webhook data.

Along with the required fields you may send only the fields you want to update.

When updating a webhook you may not change the user associated with a webhook,
so suppling the `user` field will have no effect. Also, specifying an `authToken` field has no effect.''',
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(type = "object"),
                examples = @ExampleObject('''{
    "id": 3,
    "name": "Webhook Job Runner 1",
    "project": "Webhook",
    "eventPlugin":"plugin-provider-name",
    "config": {
        "argString": "-payload ${raw} -d ${data.one}",
        "jobId": "a54d07a1-033a-499f-9789-19bcacbd6e11"
    },
    "roles": "admin,user,webhook",
    "enabled": true,
    "useAuth": true,
    "regenAuth": true
}''')
            )
        ),
        responses = [@ApiResponse(
            responseCode = "200",
            description = "Successful response",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(type = "object"),
                examples = @ExampleObject('''{
"msg": "Saved webhook",
"generatedSecurityString":"generated security string"
}
''')
            )
        ),
            @ApiResponse(
                responseCode = "400",
                description = "Error response",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = "object"),
                    examples = @ExampleObject('''{
    "err":"error message"
}''')
                )
            )
        ]
    )
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

    @Post(uri = "/project/{project}/webhook")
    @Operation(
        method = "POST",
        summary = "Add A Webhook",
        description = '''

Required Fields:
```
project - the project that owns the webhook
name - the name of the webhook
user - string the webhook runs as this user
roles - string containing comma separated list of roles to use for the webhook
eventPlugin - string must be a valid plugin name
config - object containing config values for the specified plugin
enabled - boolean
```
Optional:

`useAuth` - if true, use header authorization
`regenAuth` - if true, use generate header authorization

Do not specify an `authToken` or `creator` field. They will be ignored.

Since: v33
''',
        tags = "webhook",
        parameters = [
            @Parameter(name = "project", in = ParameterIn
                .PATH, description = "Project Name", required = true, schema = @Schema(type = "string"))
        ],
        requestBody = @RequestBody(
            description = '''Webhook definition.''',
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(type = "object"),
                examples = @ExampleObject('''{
    "id": 3,
    "name": "Webhook Job Runner 1",
    "project": "Webhook",
    "eventPlugin":"plugin-provider-name",
    "config": {
        "argString": "-payload ${raw} -d ${data.one}",
        "jobId": "a54d07a1-033a-499f-9789-19bcacbd6e11"
    },
    "user": "username",
    "roles": "admin,user,webhook",
    "enabled": true,
    "useAuth": true,
    "regenAuth": true
}''')
            )
        ),
        responses = [@ApiResponse(
            responseCode = "200",
            description = "Successful response",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(type = "object"),
                examples = @ExampleObject('''{
"msg": "Saved webhook",
"generatedSecurityString":"generated security string"
}
''')
            )
        ),
            @ApiResponse(
                responseCode = "400",
                description = "Error response",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = "object"),
                    examples = @ExampleObject('''{
    "err":"error message"
}''')
                )
            )
        ]
    )
    /**
     * nb: dummy method to document create endpoint
     */
    protected def createWebhook_docs(){}

    @Delete(uri="/project/{project}/webhook/{id}")
    @Operation(
        method = "POST",
        summary = "Delete A Webhook",
        description = '''Deletes the webhook.

Since: v33''',
        tags = "webhook",
        parameters = [
            @Parameter(name = "project", in = ParameterIn
                .PATH, description = "Project Name", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "id", in = ParameterIn
                .PATH, description = "Webhook ID", required = true, schema = @Schema(type = "string"))
        ],
        responses = [@ApiResponse(
            responseCode = "200",
            description = "Successful response",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(type = "object"),
                examples = @ExampleObject('''{
"msg": "deleted webhook"
}
''')
            )
        ),
            @ApiResponse(
                responseCode = "400",
                description = "Error response",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(type = "object"),
                    examples = @ExampleObject('''{
    "err":"error message"
}''')
                )
            )
        ]
    )
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

    @Get(uri="/project/{project}/webhook/{id}")
    @Operation(
        method="GET",
        summary = "Get A Webhook",
        description = '''Get the webhook definition.

Since: v33''',
        tags = "webhook",
        parameters=[
            @Parameter(name = "project", in = ParameterIn
                .PATH, description = "Project Name", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "id", in = ParameterIn
                .PATH, description = "Webhook ID", required = true, schema = @Schema(type = "string"))
        ],
        responses=@ApiResponse(
            responseCode="200",
            description = "Successful response",
            content=@Content(
                mediaType='application/json',
                schema = @Schema(type="object"),
                examples = @ExampleObject('''{
    "authToken": "Z1vnbhShhQF3B0dQq7UhJTZMnGS92TBl",
    "config": {
        "argString": "-payload ${raw}",
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
}''')
            )
        )
    )
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
        description="""List the webhooks for the project.

Since: v33""",
        tags = "webhook",
        parameters = @Parameter(
            name = "project",
            in = ParameterIn.PATH,
            description = "Project Name",
            required = true,
            schema = @Schema(type = "string")
        ),
        responses = @ApiResponse(
            responseCode = "200",
            description = "List of webhooks",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                array = @ArraySchema(
                    schema = @Schema(type = "object")
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

    @Post("/webhook/{authtoken}")
    @Operation(
        method = "POST",
        summary = "Send Webhook Event",
        tags = "webhook",
        description = '''You may post whatever data you wish to the webhook endpoint, however the plugin you are 
using must
be able to handle the data you post. If the webhook plugin associated with the webhook can't handle
the content type posted you will get an error response.

The webhook plugin will determine the response received.
Please see the documentation for the plugin that is configured for the webhook endpoint you are using.

If the webhook is defined to require the authorization secret, then the `Authorization` HTTP header must be included
with a value that matches the secret.

Since: v33
''',
        parameters = @Parameter(name = "authtoken", in = ParameterIn.PATH,
            required = true, description = "Webhook auth token", schema = @Schema(type = "string")),
        security = @SecurityRequirement(
            name = "webhookTokenHeader"
        ),
        responses = [
            @ApiResponse(
                responseCode = "200",
                description = "Default response",
                content = @Content(
                    mediaType = "*/*",
                    examples = @ExampleObject('ok')
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Error response",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject('''{
"err":"Error message"
}''')
                )
            ),
            @ApiResponse(
                responseCode = "503",
                description = "Webhook not enabled",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject('''{
"err":"Webhook not enabled"
}''')
                )
            )
        ]
    )
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
