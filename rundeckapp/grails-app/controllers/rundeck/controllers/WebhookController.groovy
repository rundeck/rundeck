package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.NoSuchResourceException
import com.dtolabs.rundeck.core.webhook.WebhookEventException
import com.dtolabs.rundeck.plugins.webhook.WebhookData
import com.dtolabs.rundeck.server.authorization.AuthConstants
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.eventbus.EventBus
import grails.converters.JSON
import groovy.transform.PackageScope
import rundeck.Webhook

class WebhookController {
    private static final ObjectMapper mapper = new ObjectMapper()
    def webhookService
    def frameworkService

    def index() { }

    def save() {
        def msg = [:]
        try {
            msg = webhookService.saveHook(request.JSON)
        } catch(Exception ex) {
            msg.err = ex.message
        }
        render msg as JSON
    }

    def remove() {
        try {
            def output = webhookService.delete(params.id.toLong())
            render output as JSON
        } catch(Exception ex) {
            response.status = 500
            render new HashMap([err:ex.message]) as JSON
        }


    }

    def list() {
        def hooks = Webhook.findAll().collect {
            [id:it.id,name:it.name,eventPlugin:it.eventPlugin,config:mapper.readValue(it.pluginConfigurationJson,HashMap)]
        }
        render hooks as JSON
    }

    def webhookPlugins() {
        render webhookService.listWebhookPlugins() as JSON
    }

    def post() {
        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (!authorized(authContext, AuthConstants.RESOURCE_TYPE_WEBHOOK, AuthConstants.ACTION_POST)) {
            specifyUnauthorizedError()
            return
        }
        if(params.project) {
            if(!frameworkService.projectNames(authContext).contains(params.project)) {
                response.setStatus(400)
                def err = [error: "Invalid project: ${params.project}. Either it doesn't exist or you don't have access to it."]
                render err as JSON
                return
            }
        }

        String whkName = params.name
        Webhook hook = Webhook.findByName(whkName)

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
            response.status = 400
            render new HashMap([err:wee.message]) as JSON
        }
    }

    private def specifyUnauthorizedError() {
        response.setStatus(400)
        def err = [error:"You are not authorized to perform this action"]
        render err as JSON
    }

    @PackageScope
    boolean authorized(AuthContext authContext, Map resourceType = ADMIN_RESOURCE,String action = AuthConstants.ACTION_ADMIN) {
        List authorizedActions = [AuthConstants.ACTION_ADMIN]
        if(action != AuthConstants.ACTION_ADMIN) authorizedActions.add(action)
        frameworkService.authorizeApplicationResourceAny(authContext,resourceType,authorizedActions)

    }

    private static Map ADMIN_RESOURCE = Collections.unmodifiableMap(AuthorizationUtil.resourceType("admin"))
    private static Map PROJECT_RESOURCE = Collections.unmodifiableMap(AuthorizationUtil.resourceType("project"))
}
