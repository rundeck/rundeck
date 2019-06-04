package rundeck.services

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.webhook.WebhookEventContextImpl
import com.dtolabs.rundeck.plugins.webhook.WebhookData
import com.dtolabs.rundeck.plugins.webhook.WebhookEventContext
import com.dtolabs.rundeck.plugins.webhook.WebhookEventPlugin
import com.fasterxml.jackson.databind.ObjectMapper
import grails.gorm.transactions.Transactional
import rundeck.Webhook

@Transactional
class WebhookService {
    private static final ObjectMapper mapper = new ObjectMapper()

    def rundeckPluginRegistry
    def pluginService
    def frameworkService
    def rundeckAuthorizedServicesProvider

    def processWebhook(String pluginName, String pluginConfigJson, WebhookData data, UserAndRolesAuthContext authContext) {
        PluggableProviderService webhookPluginProviderService = rundeckPluginRegistry.createPluggableService(
                WebhookEventPlugin.class)

        Map pluginConfig = pluginConfigJson ? mapper.readValue(pluginConfigJson,HashMap) : [:]
        WebhookEventPlugin plugin = pluginService.configurePlugin(pluginName, webhookPluginProviderService, frameworkService.getFrameworkPropertyResolver(data.project,pluginConfig),
                                                                  PropertyScope.Instance).instance

        WebhookEventContext context = new WebhookEventContextImpl(rundeckAuthorizedServicesProvider.getServicesWith(authContext))
        plugin.onEvent(context,data)
    }

    def saveHook(def hookData) {
        Webhook hook
        if(hookData.id) {
            hook = Webhook.get(hookData.id)
            if (!hook) return [err: "Webhook not found"]
        } else {
            hook = new Webhook()
        }
        hook.name = hookData.name
        hook.eventPlugin = hookData.eventPlugin
        hook.pluginConfigurationJson = hookData.config ? mapper.writeValueAsString(hookData.config) : "{}"
        hook.save(failOnError:true)
        return [msg:"saved"]
    }

    def delete(Long id) {
        Webhook hook = Webhook.get(id)
        String name = hook.name
        if(!hook) return [err:"Webhook does not exist"]
        hook.delete(flush:true,failOnError: true)
        return [msg:"deleted ${name} webhook"]
    }

    def listWebhookPlugins() {
        return pluginService.listPlugins(WebhookEventPlugin).collect {
            [name:it.value.name,
            configProps: it.value.description.properties.collect { it.name }]
        }
    }
}
