package rundeck.services

import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.webhook.WebhookData
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

    def processWebhook(String pluginName, String pluginConfigJson, WebhookData data) {
        PluggableProviderService webhookPluginProviderService = rundeckPluginRegistry.createPluggableService(
                WebhookEventPlugin.class)

        Map pluginConfig = pluginConfigJson ? mapper.readValue(pluginConfigJson,HashMap) : [:]
        WebhookEventPlugin plugin = pluginService.configurePlugin(pluginName, webhookPluginProviderService, frameworkService.getFrameworkPropertyResolver(data.project,pluginConfig),
                                                                  PropertyScope.Instance).instance
        plugin.onEvent(data)
    }

    def createNewHook(def hookData) {
        if(Webhook.findByName(hookData.name)) return [err:"Webhook already exists"]
        Webhook newhook = new Webhook()
        newhook.name = hookData.name
        newhook.eventPlugin = hookData.eventPlugin
        newhook.pluginConfigurationJson = hookData.pluginConfig ? mapper.writeValueAsString(hookData.pluginConfig) : "{}"
        newhook.save(failOnError:true)
        return [msg:"added"]
    }

    def delete(String name) {
        Webhook hook = Webhook.findByName(name)
        if(!hook) return [err:"Webhook does not exist"]
        hook.delete(flush:true,failOnError: true)
        return [msg:"deleted ${name} webhook"]
    }

    def deleteAll() {
        Webhook.findAll().each {
            it.delete(flush:true, failOnError:true)
        }
    }
}
