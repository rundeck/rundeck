package webhooks

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthContextEvaluator
import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import com.dtolabs.rundeck.core.authorization.RdProjectAuthorize
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.webhook.WebhookEventException
import com.dtolabs.rundeck.plugins.webhook.WebhookDataImpl
import com.fasterxml.jackson.databind.ObjectMapper
import grails.converters.JSON
import groovy.transform.PackageScope
import org.rundeck.core.auth.AuthConstants

import javax.servlet.http.HttpServletResponse

class WebhookController {
    static allowedMethods = [post:'POST']

    def webhookService
    def frameworkService
    AuthContextEvaluator rundeckAuthContextEvaluator
    AuthContextProvider rundeckAuthContextProvider
    def apiService

    def admin() {}

    @RdProjectAuthorize("webhook.save")
    def save() {
        def msg = webhookService.saveHook(authContext,request.JSON)
        if(msg.err) response.status = 400

        render msg as JSON
    }

    @RdProjectAuthorize("webhook.delete")
    def remove() {
        Webhook webhook = webhookService.getWebhook(params.id.toLong())
        if(!webhook) {
            sendJsonError("Webhook not found")
            return
        }

        def output = webhookService.delete(webhook)
        if(output.err) response.status = 400
        render output as JSON

    }

    @RdProjectAuthorize(value="webhook.read")
    def get() {
        if(!params.id){
            return apiService.renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                                                           code: 'api.error.parameter.required', args: ['id']])

        }

        def webhook = webhookService.getWebhookForProjectWithAuth(params.id, params.project)
        if(!webhook){
            return sendJsonError("Webhook not found", HttpServletResponse.SC_NOT_FOUND)
        }
        render webhook as JSON
    }

    @RdProjectAuthorize(value="webhook.read")
    def list() {
        render webhookService.listWebhooksByProject(params.project) as JSON
    }

    @RdProjectAuthorize(value="webhook.read")
    def editorData() {
        def uidata = [:]
        uidata.hooks = webhookService.listWebhooksByProject(params.project)
        uidata.username = authContext.username
        uidata.roles = authContext.roles.join(",")
        render uidata as JSON
    }

    @RdProjectAuthorize(value="webhook.post")
    def post() {
        Webhook hook = webhookService.getWebhookByToken(Webhook.cleanAuthToken(params.authtoken))

        if(!hook) {
            sendJsonError("Webhook not found")
            return
        }
        if(!hook.enabled) {
            sendJsonError("Webhook not enabled",503)
            return
        }

        WebhookDataImpl whkdata = new WebhookDataImpl()
        whkdata.webhookUUID = hook.uuid
        whkdata.webhook = hook.name
        whkdata.timestamp = System.currentTimeMillis()
        whkdata.sender = request.remoteAddr
        whkdata.project = hook.project
        whkdata.contentType = request.contentType
        whkdata.data = request.inputStream

        try {
            def responder = webhookService.processWebhook(hook.eventPlugin, hook.pluginConfigurationJson, whkdata, authContext, request)
            responder.respond(response)
        } catch(WebhookEventException wee) {
            sendJsonError(wee.message)
        }
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
}
