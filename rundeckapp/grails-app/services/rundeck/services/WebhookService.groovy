package rundeck.services

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
import org.rundeck.app.spi.AppService
import org.rundeck.app.spi.Services
import rundeck.AuthToken
import rundeck.User
import rundeck.Webhook

@Transactional
class WebhookService {
    private static final Logger LOG4J_LOGGER = Logger.getLogger("org.rundeck.webhooks")
    private static final ObjectMapper mapper = new ObjectMapper()

    def rundeckPluginRegistry
    def pluginService
    def frameworkService
    def rundeckAuthorizedServicesProvider
    def apiService
    def messageSource

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

    def saveHook(UserAndRolesAuthContext authContext,def hookData) {
        Webhook hook
        User user = hookData.user ? User.findByLogin(hookData.user) : authContext.username
        if (!user) return [err: "Webhook user '${hookData.user}' not found"]
        if(hookData.id) {
            hook = Webhook.get(hookData.id)
            hook.authToken.user = user
            hook.authToken.authRoles = hookData.roles
            if (!hook) return [err: "Webhook not found"]
        } else {
            hook = new Webhook()
            Set<String> roles = hookData.roles ? AuthToken.parseAuthRoles(hookData.roles) : authContext.roles
            hook.authToken = apiService.generateAuthToken(authContext.username,user,roles, null, true)
        }
        hook.name = hookData.name
        hook.eventPlugin = hookData.eventPlugin
        hook.pluginConfigurationJson = hookData.config ? mapper.writeValueAsString(hookData.config) : "{}"
        if(hook.save()) {
            return [msg: "saved"]
        } else {
            return [err: hook.errors.allErrors.collect { messageSource.getMessage(it,null) }.join(",")]
        }
    }

    def delete(Long id) {
        Webhook hook = Webhook.get(id)
        String name = hook.name
        if(!hook) return [err:"Webhook does not exist"]
        if(hook.delete()) {
            return [msg: "deleted ${name} webhook"]
        } else {
            return [err: hook.errors.allErrors.collect { messageSource.getMessage(it,null) }.join(",")]
        }
    }

    Webhook getWebhook(String webhookName) {
        return Webhook.findByName(webhookName)
    }
}
