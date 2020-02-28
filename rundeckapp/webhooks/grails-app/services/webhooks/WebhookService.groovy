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
            if(hookData.roles && !hookData.importData) {
                try {
                    rundeckAuthTokenManagerService.updateAuthRoles(authContext, hook.authToken,rundeckAuthTokenManagerService.parseAuthRoles(hookData.roles))
                } catch (Exception e) {
                    return [err: "Failed to update Auth Token roles: "+e.message]
                }
            }
        } else {
            int countByNameInProject = Webhook.countByNameAndProject(hookData.name,hookData.project)
            if(countByNameInProject > 0) return [err: "A Webhook by that name already exists in this project"]
            String checkUser = hookData.user ?: authContext.username
            if (!hookData.importData && !userService.validateUserExists(checkUser)) return [err: "Webhook user '${checkUser}' not found"]
            hook = new Webhook()
            hook.uuid = UUID.randomUUID().toString()
        }
        hook.uuid = hookData.uuid ?: hook.uuid
        hook.name = hookData.name ?: hook.name
        hook.project = hookData.project ?: hook.project
        if(hookData.enabled != null) hook.enabled = hookData.enabled
        if(hookData.eventPlugin && !pluginService.listPlugins(WebhookEventPlugin).any { it.key == hookData.eventPlugin}){
            hook.discard()
            return [err:"Plugin does not exist: " + hookData.eventPlugin]
        }
        hook.eventPlugin = hookData.eventPlugin ?: hook.eventPlugin

        Map pluginConfig = [:]
        if(hookData.config) pluginConfig = hookData.config instanceof String ? mapper.readValue(hookData.config, HashMap) : hookData.config

        def (ValidatedPlugin vPlugin, boolean isCustom) = validatePluginConfig(hook.eventPlugin,pluginConfig)
        if(!vPlugin.valid) {
            def errMsg = isCustom ?
                    "Validation errors" :
                    "Invalid plugin configuration: " + vPlugin.report.errors.collect { k, v -> "$k : $v" }.join("\n")
            hook.discard()

            return [err: errMsg, errors: vPlugin.report.errors]
        }

        hook.pluginConfigurationJson = mapper.writeValueAsString(pluginConfig)
        Set<String> roles = hookData.roles ? rundeckAuthTokenManagerService.parseAuthRoles(hookData.roles) : authContext.roles
        if((!hook.id || !hook.authToken) && !hookData.shouldImportToken){
            //create token
            String checkUser = hookData.user ?: authContext.username
            try {
                def at=apiService.generateUserToken(authContext, null, checkUser, roles, false, true)
                hook.authToken = at.token
            } catch (Exception e) {
                hook.discard()
                return [err: "Failed to create associated Auth Token: "+e.message]
            }
        }
        if(hookData.shouldImportToken) {
            if(!importIsAllowed(hook,hookData)){
                throw new Exception("Cannot import webhook: auth token already in use")
            }
            try {
                rundeckAuthTokenManagerService.importWebhookToken(authContext, hookData.authToken, hookData.user, roles)
            } catch (Exception e) {
                hook.discard()
                return [err: "Failed importing Webhook Token: "+e.message]
            }
            hook.authToken = hookData.authToken
        }

        if(hook.save(true)) {
            return [msg: "Saved webhook"]
        } else {
            if(!hook.id && hook.authToken){
                //delete the created token
                rundeckAuthTokenManagerService.deleteToken(hook.authToken)
            }
            return [err: hook.errors.allErrors.collect { messageSource.getMessage(it,null) }.join(",")]
        }
    }

    boolean importIsAllowed(Webhook hook, Map hookData) {
        if(hook.authToken == hookData.authToken) return true
        if(!hook.authToken && Webhook.countByAuthToken(hookData.authToken) == 0 && !rundeckAuthTokenManagerService.getToken(hookData.authToken)) return true
        return false
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

    def deleteWebhooksForProject(String project) {
        Webhook.findAllByProject(project).each { webhook ->
            delete(webhook)
        }
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

    def importWebhook(UserAndRolesAuthContext authContext, Map hook, boolean regenAuthTokens) {

        Webhook existing = Webhook.findByUuidAndProject(hook.uuid, hook.project)
        if(existing) hook.id = existing.id
        hook.importData = true

        if(!regenAuthTokens && hook.authToken) {
            hook.shouldImportToken = true
        } else {
            hook.authToken = null
        }

        try {
            def msg = saveHook(authContext, hook)
            if(msg.err) {
                log.error("Failed to import webhook. Error: " + msg.err)
                return [err:"Unable to import webhoook ${hook.name}. Error:"+msg.err]
            }
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
        return [id:hook.id, uuid:hook.uuid, name:hook.name, project: hook.project, enabled: hook.enabled, user:authToken.ownerName, creator:authToken.creator, roles: authToken.authRolesSet().join(","), authToken:hook.authToken, eventPlugin:hook.eventPlugin, config:mapper.readValue(hook.pluginConfigurationJson, HashMap)]
    }

    Webhook getWebhook(Long id) {
        return Webhook.get(id)
    }

    Webhook getWebhookByUuid(String uuid) {
        return Webhook.findByUuid(uuid)
    }

    Webhook getWebhookByToken(String token) {
        return Webhook.findByAuthToken(token)
    }
}
