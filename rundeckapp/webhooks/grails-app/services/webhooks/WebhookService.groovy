package webhooks

import com.dtolabs.rundeck.core.authentication.tokens.AuthTokenManager
import com.dtolabs.rundeck.core.authentication.tokens.AuthenticationToken
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.PluginAdapterUtility
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.webhook.WebhookEventContextImpl
import com.dtolabs.rundeck.plugins.webhook.WebhookData
import com.dtolabs.rundeck.plugins.webhook.WebhookEventContext
import com.dtolabs.rundeck.plugins.webhook.WebhookEventPlugin
import com.fasterxml.jackson.databind.ObjectMapper
import grails.gorm.transactions.Transactional
import org.apache.log4j.Logger
import webhooks.Webhook

@Transactional
class WebhookService {
    private static final Logger LOG4J_LOGGER = Logger.getLogger("org.rundeck.webhook.events")
    private static final ObjectMapper mapper = new ObjectMapper()

    def rundeckPluginRegistry
    def pluginService
    def frameworkService
    def rundeckAuthorizedServicesProvider
    def apiService
    def messageSource
    def userService
    def rundeckAuthTokenManagerService

    def processWebhook(String pluginName, String pluginConfigJson, WebhookData data, UserAndRolesAuthContext authContext) {
        LOG4J_LOGGER.info("processing '" + data.webhook + "' with plugin '" + pluginName + "' triggered by: '" + authContext.username+"'")
        PluggableProviderService webhookPluginProviderService = rundeckPluginRegistry.createPluggableService(
                WebhookEventPlugin.class)

        Map pluginConfig = pluginConfigJson ? mapper.readValue(pluginConfigJson,HashMap) : [:]
        WebhookEventPlugin plugin = pluginService.configurePlugin(pluginName, webhookPluginProviderService, frameworkService.getFrameworkPropertyResolver(data.project,pluginConfig),
                                                                  PropertyScope.Instance).instance

        PluginAdapterUtility.setConfig(plugin, pluginConfig)

        WebhookEventContext context = new WebhookEventContextImpl(rundeckAuthorizedServicesProvider.getServicesWith(authContext))
        plugin.onEvent(context,data)
    }

    def listWebhooksByProject(String project) {
        Webhook.findAllByProject(project).collect {
            AuthenticationToken authToken = rundeckAuthTokenManagerService.getToken(it.authToken)
            [id:it.id, name:it.name, project: it.project, user:authToken.ownerName, roles: authToken.authRoles, authToken:it.authToken, eventPlugin:it.eventPlugin, config:mapper.readValue(it.pluginConfigurationJson, HashMap)]
        }
    }

    def saveHook(UserAndRolesAuthContext authContext,def hookData) {
        Webhook hook
        if(hookData.id) {
            hook = Webhook.get(hookData.id)
            if (!hook) return [err: "Webhook not found"]
            rundeckAuthTokenManagerService.updateAuthRoles(hook.authToken,hookData.roles)
        } else {
            String checkUser = hookData.user ?: authContext.username
            if (!userService.validateUserExists(checkUser)) return [err: "Webhook user '${checkUser}' not found"]
            hook = new Webhook()
            Set<String> roles = hookData.roles ? rundeckAuthTokenManagerService.parseAuthRoles(hookData.roles) : authContext.roles
            hook.authToken = apiService.generateUserToken(authContext,null,checkUser,roles, false, true).token
        }
        hook.name = hookData.name
        hook.project = hookData.project
        hook.eventPlugin = hookData.eventPlugin
        hook.pluginConfigurationJson = hookData.config ? mapper.writeValueAsString(hookData.config) : "{}"
        if(hook.save()) {
            return [msg: "saved"]
        } else {
            return [err: hook.errors.allErrors.collect { messageSource.getMessage(it,null) }.join(",")]
        }
    }

    def delete(Webhook hook) {
        String authToken = hook.authToken
        if(hook.delete()) {
            rundeckAuthTokenManagerService.deleteToken(authToken)
            return [msg: "deleted ${name} webhook"]
        } else {
            return [err: hook.errors.allErrors.collect { messageSource.getMessage(it,null) }.join(",")]
        }
    }

    Webhook getWebhook(Long id) {
        return Webhook.get(id)
    }

    Webhook getWebhookByToken(String token) {
        return Webhook.findByAuthToken(token)
    }
}
