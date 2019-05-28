package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.common.NoSuchResourceException
import com.dtolabs.rundeck.plugins.webhook.WebhookData
import grails.converters.JSON
import groovy.transform.PackageScope
import rundeck.Webhook

class WebhookController {
    def webhookService
    def frameworkService

    def index() { }

    def add() {
        def msg = webhookService.createNewHook(request.JSON)
        render msg as JSON
    }

    def remove() {
        try {
            def output = webhookService.delete(params.name)
            render output as JSON
        } catch(Exception ex) {
            response.status = 500
            render new HashMap([err:ex.message]) as JSON
        }


    }

    def removeAll() {
        webhookService.deleteAll()
        render new HashMap([msg:"deleted all"]) as JSON
    }

    def list() {
        render Webhook.findAll() as JSON
    }

    def post() {
        if (!authorized(WEBHOOK_RESOURCE,"post")) {
            specifyUnauthorizedError()
            return
        }
        try {
            frameworkService.getFrameworkProject(params.project)
        } catch(NoSuchResourceException ex) {
            response.setStatus(400)
            def err = [error:ex.message]
            render err as JSON
            return
        }

        //TODO: authorize with API token
        String whkName = params.name
        Webhook hook = Webhook.findByName(whkName)

        WebhookData whkdata = new WebhookData()
        whkdata.webhook = hook.name
        whkdata.timestamp = System.currentTimeMillis()
        whkdata.sender = request.remoteAddr
        whkdata.project = params.project
        whkdata.data = request.inputStream

        webhookService.processWebhook(hook.eventPlugin, hook.pluginConfigurationJson, whkdata)
        render new HashMap([msg:"ok"]) as JSON
    }

    private def specifyUnauthorizedError() {
        response.setStatus(400)
        def err = [error:"You are not authorized to perform this action"]
        render err as JSON
    }

    @PackageScope
    boolean authorized(Map resourceType = ADMIN_RESOURCE,String action = "admin") {
        List authorizedActions = ["admin"]
        if(action != "admin") authorizedActions.add(action)
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        frameworkService.authorizeApplicationResourceAny(authContext,resourceType,authorizedActions)

    }

    private static Map WEBHOOK_RESOURCE = Collections.unmodifiableMap(AuthorizationUtil.resourceType("webhook"))
    private static Map ADMIN_RESOURCE = Collections.unmodifiableMap(AuthorizationUtil.resourceType("admin"))
}
