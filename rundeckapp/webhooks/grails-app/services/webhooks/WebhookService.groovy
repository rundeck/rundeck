package webhooks


import com.dtolabs.rundeck.core.authentication.tokens.AuthenticationToken
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.PluginAdapterUtility
import com.dtolabs.rundeck.core.plugins.configuration.PluginCustomConfigValidator
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.webhook.WebhookEventContextImpl
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginCustomConfig
import com.dtolabs.rundeck.plugins.webhook.WebhookDataImpl
import com.dtolabs.rundeck.plugins.webhook.WebhookEventContext
import com.dtolabs.rundeck.plugins.webhook.WebhookEventPlugin
import com.fasterxml.jackson.databind.ObjectMapper
import grails.gorm.transactions.Transactional
import groovy.transform.PackageScope
import org.apache.log4j.Logger

import javax.servlet.http.HttpServletRequest

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

    def processWebhook(String pluginName, String pluginConfigJson, WebhookDataImpl data, UserAndRolesAuthContext authContext, HttpServletRequest request) {
        LOG4J_LOGGER.info("processing '" + data.webhook + "' with plugin '" + pluginName + "' triggered by: '" + authContext.username+"'")
        Map pluginConfig = pluginConfigJson ? mapper.readValue(pluginConfigJson,HashMap) : [:]
        replaceSecureOpts(authContext,pluginConfig)
        WebhookEventPlugin plugin = pluginService.configurePlugin(pluginName, WebhookEventPlugin.class, frameworkService.getFrameworkPropertyResolver(data.project,pluginConfig),
                                                                  PropertyScope.Instance).instance

        PluginAdapterUtility.setConfig(plugin, pluginConfig)

        plugin.requestHeadersToCopy?.each { hdr -> data.headers[hdr] = request.getHeader(hdr)}

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
            getWebhookWithAuthAsMap(it)
        }
    }

    def saveHook(UserAndRolesAuthContext authContext,def hookData) {
        Webhook hook
        if(hookData.id) {
            hook = Webhook.get(hookData.id)
            if (!hook) return [err: "Webhook not found"]
            if(hookData.roles) {
                if(!rundeckAuthTokenManagerService.updateAuthRoles(hook.authToken,rundeckAuthTokenManagerService.parseAuthRoles(hookData.roles))) return [err:"Could not update token roles"]
            }
        } else {
            int countByNameInProject = Webhook.countByNameAndProject(hookData.name,hookData.project)
            if(countByNameInProject > 0) return [err: "A Webhook by that name already exists in this project"]
            String checkUser = hookData.user ?: authContext.username
            if (!userService.validateUserExists(checkUser)) return [err: "Webhook user '${checkUser}' not found"]
            hook = new Webhook()
            Set<String> roles = hookData.roles ? rundeckAuthTokenManagerService.parseAuthRoles(hookData.roles) : authContext.roles
            hook.authToken = apiService.generateUserToken(authContext,null,checkUser,roles, false, true).token
        }
        hook.name = hookData.name ?: hook.name
        hook.project = hookData.project ?: hook.project
        if(hookData.enabled != null) hook.enabled = hookData.enabled
        if(hookData.eventPlugin && !pluginService.listPlugins(WebhookEventPlugin).any { it.key == hookData.eventPlugin}) return [err:"Plugin does not exist: " + hookData.eventPlugin]
        hook.eventPlugin = hookData.eventPlugin ?: hook.eventPlugin

        Map pluginConfig = [:]
        if(hookData.config) pluginConfig = hookData.config instanceof String ? mapper.readValue(hookData.config, HashMap) : hookData.config

        def (ValidatedPlugin vPlugin, boolean isCustom) = validatePluginConfig(hook.eventPlugin,pluginConfig)
        if(!vPlugin.valid) {
            def errMsg = isCustom ?
                    "Validation errors" :
                    "Invalid plugin configuration: " + vPlugin.report.errors.collect { k, v -> "$k : $v" }.join("\n")

            return [err: errMsg, errors: vPlugin.report.errors]
        }

        hook.pluginConfigurationJson = mapper.writeValueAsString(pluginConfig)

        if(hook.save(true)) {
            return [msg: "Saved webhook"]
        } else {
            return [err: hook.errors.allErrors.collect { messageSource.getMessage(it,null) }.join(",")]
        }
    }

    @PackageScope
    Tuple2<ValidatedPlugin, Boolean> validatePluginConfig(String webhookPlugin, Map pluginConfig) {
        ValidatedPlugin result = pluginService.validatePluginConfig(ServiceNameConstants.WebhookEvent, webhookPlugin, pluginConfig)
        def isCustom = false
        def plugin = pluginService.getPlugin(webhookPlugin,WebhookEventPlugin.class)
        PluginCustomConfig customConfig = PluginAdapterUtility.getCustomConfigAnnotation(plugin)
        if(customConfig && customConfig.validator()) {
            PluginCustomConfigValidator validator = Validator.createCustomPropertyValidator(customConfig)
            if(validator) {
                result.report.errors.putAll(validator.validate(pluginConfig).errors)
                result.valid = result.report.valid
                isCustom = true
            }
        }
        return new Tuple2(result, isCustom)
    }

    def delete(Webhook hook) {
        String authToken = hook.authToken
        String name = hook.name
        try {
            hook.delete()
            rundeckAuthTokenManagerService.deleteToken(authToken)
            return [msg: "Deleted ${name} webhook"]
        } catch(Exception ex) {
            log.error("delete webhook failed",ex)
            return [err: ex.message]
        }
    }

    def importWebhook(hook) {
        if(Webhook.findByAuthToken(hook.apiToken.token)) return [msg:"Webhook with token ${hook.apiToken.token} exists. Skipping..."]
        if(!rundeckAuthTokenManagerService.importWebhookToken(hook.apiToken.token, hook.apiToken.creator, hook.apiToken.user, rundeckAuthTokenManagerService.parseAuthRoles(hook.apiToken.roles))) return [err:"Unable to create webhook api token"]
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

    def getWebhookWithAuth(String id) {
        Webhook hook = Webhook.get(id.toLong())
        getWebhookWithAuthAsMap(hook)
    }

    private Map getWebhookWithAuthAsMap(Webhook hook) {
        AuthenticationToken authToken = rundeckAuthTokenManagerService.getToken(hook.authToken)
        return [id:hook.id, name:hook.name, project: hook.project, enabled: hook.enabled, user:authToken.ownerName, creator:authToken.creator, roles: authToken.authRolesSet().join(","), authToken:hook.authToken, eventPlugin:hook.eventPlugin, config:mapper.readValue(hook.pluginConfigurationJson, HashMap)]
    }

    Webhook getWebhook(Long id) {
        return Webhook.get(id)
    }

    Webhook getWebhookByToken(String token) {
        return Webhook.findByAuthToken(token)
    }
}
