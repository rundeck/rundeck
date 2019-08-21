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
import groovy.transform.PackageScope
import org.apache.log4j.Logger
import webhooks.Webhook

@Transactional
class WebhookService {
    private static final Logger LOG4J_LOGGER = Logger.getLogger("org.rundeck.webhook.events")
    private static final ObjectMapper mapper = new ObjectMapper()
    private static final String KEY_STORE_PREFIX = "\${KS:"
    private static final String END_MARKER = "}"

    def rundeckPluginRegistry
    def pluginService
    def frameworkService
    def rundeckAuthorizedServicesProvider
    def apiService
    def messageSource
    def userService
    def rundeckAuthTokenManagerService
    def storageService

    def processWebhook(String pluginName, String pluginConfigJson, WebhookData data, UserAndRolesAuthContext authContext) {
        LOG4J_LOGGER.info("processing '" + data.webhook + "' with plugin '" + pluginName + "' triggered by: '" + authContext.username+"'")
        PluggableProviderService webhookPluginProviderService = rundeckPluginRegistry.createPluggableService(
                WebhookEventPlugin.class)

        Map pluginConfig = pluginConfigJson ? mapper.readValue(pluginConfigJson,HashMap) : [:]
        replaceSecureOpts(authContext,pluginConfig)
        WebhookEventPlugin plugin = pluginService.configurePlugin(pluginName, webhookPluginProviderService, frameworkService.getFrameworkPropertyResolver(data.project,pluginConfig),
                                                                  PropertyScope.Instance).instance

        PluginAdapterUtility.setConfig(plugin, pluginConfig)

        WebhookEventContext context = new WebhookEventContextImpl(rundeckAuthorizedServicesProvider.getServicesWith(authContext))
        plugin.onEvent(context,data)
    }

    @PackageScope
    void replaceSecureOpts(UserAndRolesAuthContext authContext, Map configProps) {
        if(configProps.isEmpty()) return
        def keystore = storageService.storageTreeWithContext(authContext)

        Stack<Object> items = []

        configProps.each { idx, i -> items.push([i, configProps, idx]) }

        while(true) {
            if (items.empty())
                break

            def elem = items.pop()

            def (item, parent, index) = elem

            if (item instanceof Map) {
                item.each { idx, i -> items.push([i, item, idx]) }
                continue
            } else if (item instanceof List) {
                item.eachWithIndex { i, idx -> items.push([i, item, idx]) }
                continue
            }

            if (item instanceof String) {
                if(item && item.contains(KEY_STORE_PREFIX)) {
                    String replaced = item
                    int startIdx = -1
                    while(replaced.indexOf(KEY_STORE_PREFIX,startIdx+1) != -1) {
                        startIdx = replaced.indexOf(KEY_STORE_PREFIX)
                        int endIdx = replaced.indexOf(END_MARKER,startIdx)
                        if(endIdx == -1) {
                            log.error("Invalid substitution string, terminating marker not found in value: ${replaced}")
                            break
                        }
                        String valueToReplace = replaced.substring(startIdx,endIdx+1)
                        String keyPath = valueToReplace.substring(KEY_STORE_PREFIX.length(),valueToReplace.length()-1)
                        if(keystore.hasPassword(keyPath)) {
                            String replacementValue = new String(keystore.readPassword(keyPath))
                            replaced = replaced.replace(valueToReplace,replacementValue)
                        } else {
                            log.warn("key was not found in key store: ${keyPath}")
                        }
                    }
                    parent[index] = replaced
                }
            }
        }
    }

    def listWebhooksByProject(String project) {
        Webhook.findAllByProject(project).collect {
            AuthenticationToken authToken = rundeckAuthTokenManagerService.getToken(it.authToken)
            [id:it.id, name:it.name, project: it.project, user:authToken.ownerName, creator:authToken.creator, roles: authToken.authRoles, authToken:it.authToken, eventPlugin:it.eventPlugin, config:mapper.readValue(it.pluginConfigurationJson, HashMap)]
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

    def importWebhook(hook) {
        if(Webhook.findByAuthToken(hook.apiToken.token)) return [msg:"Webhook exists"]
        if(!rundeckAuthTokenManagerService.importWebhookToken(hook.apiToken.token, hook.apiToken.creator, hook.apiToken.user, hook.apiToken.roles)) return [err:"Unable to create webhook api token"]
        Webhook ihook = new Webhook()
        ihook.name = hook.name
        ihook.authToken = hook.apiToken.token
        ihook.project = hook.project
        ihook.eventPlugin = hook.eventPlugin
        ihook.pluginConfigurationJson = hook.pluginConfiguration
        try {
            ihook.save(failOnError:true)
            return [msg:"Webhook ${hook.name} imported"]
        } catch(Exception ex) {
            log.error("Failed to import webhook", ex)
            return [err:"Unable to import webhoook ${hook.name}. Error:"+ex.message]
        }

    }

    Webhook getWebhook(Long id) {
        return Webhook.get(id)
    }

    Webhook getWebhookByToken(String token) {
        return Webhook.findByAuthToken(token)
    }
}
