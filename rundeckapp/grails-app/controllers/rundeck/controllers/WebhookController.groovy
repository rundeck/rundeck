package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.webhook.WebhookEventException
import com.dtolabs.rundeck.plugins.webhook.WebhookData
import com.dtolabs.rundeck.server.authorization.AuthConstants
import com.fasterxml.jackson.databind.ObjectMapper
import grails.converters.JSON
import groovy.transform.PackageScope
import rundeck.Webhook

class WebhookController {
    private static final ObjectMapper mapper = new ObjectMapper()
    def webhookService
    def frameworkService

    def index() { }

    def save() {
        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (!frameworkService.authorizeApplicationResourceAny(authContext,AuthConstants.RESOURCE_TYPE_WEBHOOK, [AuthConstants.ACTION_CREATE,AuthConstants.ACTION_UPDATE])) {
            sendJsonError("You are not authorized to perform this action")
            return
        }
        def msg = [:]
        try {
            msg = webhookService.saveHook(authContext,request.JSON)
        } catch(Exception ex) {
            response.status = 500
            msg.err = ex.message
        }
        render msg as JSON
    }

    def remove() {
        try {
            UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
            if (!authorized(authContext, AuthConstants.RESOURCE_TYPE_WEBHOOK, AuthConstants.ACTION_DELETE)) {
                sendJsonError("You are not authorized to perform this action")
                return
            }
            def output = webhookService.delete(params.id.toLong())
            render output as JSON
        } catch(Exception ex) {
            response.status = 500
            render new HashMap([err:ex.message]) as JSON
        }
    }

    def list() {
        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (!authorized(authContext, AuthConstants.RESOURCE_TYPE_WEBHOOK, AuthConstants.ACTION_READ)) {
            sendJsonError("You do not have access to this resource")
            return
        }
        render listHooks() as JSON
    }

    def uiData() {
        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (!authorized(authContext, AuthConstants.RESOURCE_TYPE_WEBHOOK, AuthConstants.ACTION_READ)) {
            sendJsonError("You do not have access to this resource")
            return
        }

        def uidata = [:]
        uidata.hooks = listHooks()
        uidata.username = authContext.username
        uidata.roles = authContext.roles.join(",")
        render uidata as JSON
    }

    private def listHooks() {
        Webhook.findAll().collect {
            [id:it.id, name:it.name, user:it.authToken.user.login, roles: it.authToken.authRoles, authToken:it.authToken.token, eventPlugin:it.eventPlugin, config:mapper.readValue(it.pluginConfigurationJson, HashMap)]
        }
    }

    def webhookPlugins() {
        render webhookService.listWebhookPlugins() as JSON
    }

    def post() {
        String whkName = params.name
        String token = params.authtoken
        Webhook hook = webhookService.getWebhook(whkName)

        if(!hook) {
            sendJsonError("Hook '${whkName}' not found")
            return
        }
        //Webhook tokens may only be used for the webhook which owns them
        if(hook.authToken.token != token) {
            sendJsonError("Invalid webhook token")
            return
        }

        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (!authorized(authContext, AuthConstants.RESOURCE_TYPE_WEBHOOK, AuthConstants.ACTION_POST)) {
            sendJsonError("You are not authorized to perform this action")
            return
        }

        if(params.project) {
            if(!frameworkService.projectNames(authContext).contains(params.project)) {
                sendJsonError("Invalid project: ${params.project}. Either it doesn't exist or you don't have access to it.")
                return
            }
        }

        WebhookData whkdata = new WebhookData()
        whkdata.webhook = hook.name
        whkdata.timestamp = System.currentTimeMillis()
        whkdata.sender = request.remoteAddr
        whkdata.project = params.project
        whkdata.contentType = request.contentType
        whkdata.data = request.inputStream

        try {
            webhookService.processWebhook(hook.eventPlugin, hook.pluginConfigurationJson, whkdata, authContext)
            render new HashMap([msg: "ok"]) as JSON
        } catch(WebhookEventException wee) {
            sendJsonError(wee.message)
        }
    }

    private def sendJsonError(String errMessage) {
        response.setStatus(400)
        def err = [err:errMessage]
        render err as JSON
    }

    @PackageScope
    boolean authorized(AuthContext authContext, Map resourceType = ADMIN_RESOURCE,String action = AuthConstants.ACTION_ADMIN) {
        List authorizedActions = [AuthConstants.ACTION_ADMIN]
        if(action != AuthConstants.ACTION_ADMIN) authorizedActions.add(action)
        frameworkService.authorizeApplicationResourceAny(authContext,resourceType,authorizedActions)
    }

    private static Map ADMIN_RESOURCE = Collections.unmodifiableMap(AuthorizationUtil.resourceType("admin"))
}
