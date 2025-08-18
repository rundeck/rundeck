package webhooks


import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.config.Features
import com.dtolabs.rundeck.core.event.Event
import com.dtolabs.rundeck.core.event.EventImpl
import com.dtolabs.rundeck.core.event.EventQueryImpl
import com.dtolabs.rundeck.core.event.EventQueryResult
import com.dtolabs.rundeck.core.event.EventStoreService
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.PluginAdapterUtility
import com.dtolabs.rundeck.core.plugins.configuration.PluginCustomConfigValidator
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree
import com.dtolabs.rundeck.core.webhook.WebhookEventContextImpl
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginCustomConfig
import com.dtolabs.rundeck.plugins.webhook.DefaultWebhookResponder
import com.dtolabs.rundeck.plugins.webhook.WebhookDataImpl
import com.dtolabs.rundeck.plugins.webhook.WebhookEventContext
import com.dtolabs.rundeck.plugins.webhook.WebhookEventPlugin
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.apache.commons.lang.RandomStringUtils
import org.rundeck.app.data.model.v1.authtoken.AuthTokenType
import org.rundeck.app.data.model.v1.authtoken.AuthenticationToken
import org.rundeck.app.data.model.v1.storedevent.StoredEventQuery
import org.rundeck.app.data.model.v1.storedevent.StoredEventQueryType
import org.rundeck.app.data.model.v1.webhook.RdWebhook
import org.rundeck.app.data.model.v1.webhook.dto.SaveWebhookRequest
import org.rundeck.app.data.model.v1.webhook.dto.SaveWebhookResponse
import org.rundeck.app.data.providers.v1.webhook.WebhookDataProvider
import org.rundeck.app.spi.Services
import org.rundeck.app.spi.SimpleServiceProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Propagation
import webhooks.authenticator.AuthorizationHeaderAuthenticator

import javax.servlet.http.HttpServletRequest
import java.util.concurrent.TimeUnit

@Transactional
class WebhookService {
    private static final Logger LOGGER = LoggerFactory.getLogger("org.rundeck.webhook.events")
    private static final ObjectMapper mapper = new ObjectMapper()
    private static final String KEY_STORE_PREFIX = "\${KS:"
    private static final String END_MARKER = "}"
    static final String TOPIC_EVENTS_BASE = 'webhook:events'

    WebhookDataProvider webhookDataProvider

    @Autowired
    EventStoreService gormEventStoreService
    def rundeckPluginRegistry
    def pluginService
    def frameworkService
    def rundeckAuthorizedServicesProvider
    def apiService
    def userService
    def rundeckAuthTokenManagerService
    def storageService
    def featureService

    def processWebhook(String pluginName, String pluginConfigJson, WebhookDataImpl data, UserAndRolesAuthContext authContext, HttpServletRequest request) {
        LOGGER.info("processing '" + data.webhook + "' with plugin '" + pluginName + "' triggered by: '" + authContext.username+ "'")
        Map pluginConfig = pluginConfigJson ? mapper.readValue(pluginConfigJson,HashMap) : [:]
        replaceSecureOpts(authContext,pluginConfig)
        WebhookEventPlugin plugin = pluginService.configurePlugin(pluginName, WebhookEventPlugin.class, frameworkService.getFrameworkPropertyResolver(data.project,pluginConfig),
                                                                  PropertyScope.Instance).instance

        PluginAdapterUtility.setConfig(plugin, pluginConfig)

        plugin.requestHeadersToCopy?.each { hdr -> data.headers[hdr] = request.getHeader(hdr)}

        Services contextServices = rundeckAuthorizedServicesProvider.getServicesWith(authContext)

        if (featureService.featurePresent(Features.EVENT_STORE)) {
            def scopedStore = gormEventStoreService.scoped(
                new Evt(projectName: data.project, subsystem: 'webhooks'),
                new EvtQuery(projectName: data.project, subsystem: 'webhooks')
            )
            contextServices = contextServices.combine(
                    new SimpleServiceProvider([(EventStoreService): new EventStoreServiceTxn(scopedStore)])
            )
        }
        def keyStorageService = storageService.storageTreeWithContext(authContext)
        contextServices = contextServices.combine(new SimpleServiceProvider([(KeyStorageTree): keyStorageService]))

        WebhookEventContext context = new WebhookEventContextImpl(contextServices)

        return plugin.onEvent(context,data) ?: new DefaultWebhookResponder()
    }

    @CompileStatic
    static class EventStoreServiceTxn implements EventStoreService{
        @Delegate
        EventStoreService delegate

        EventStoreServiceTxn(final EventStoreService delegate) {
            this.delegate = delegate
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        @Override
        void storeEvent(final Event event) {
            delegate.storeEvent(event)
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        @Override
        EventQueryResult query(final StoredEventQuery query) {
            return delegate.query(query)
        }
    }

    /**
     * Receives a webhook and delete all the stored event data in DB related to it
     * @param webhook
     */
    def deleteWebhookEventsData(RdWebhook webhook) {
        Long totalAmountOfRowsAffected = deleteEvents(TOPIC_EVENTS_BASE+':*', webhook)
        log.info("${totalAmountOfRowsAffected} events deleted related to the webhook: ${webhook.name}")
    }

    /**
     * Perform the delete querie for webhook events data in DB
     * @param eventTopic : The string with data for the specific event (see webhook's records in stored_events table in DB)
     * @param webhook
     */
    private def deleteEvents(String eventTopic, RdWebhook webhook) {
        EventQueryResult queryResult = gormEventStoreService.query(new EvtQuery(
                queryType: StoredEventQueryType.DELETE,
                projectName: webhook.project,
                subsystem: "webhooks",
                topic: "${eventTopic}:${webhook.uuid}"
        ))
        return queryResult.totalCount
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
        webhookDataProvider.findAllByProject(project).collect {
            getWebhookWithAuthAsMap(it)
        }
    }

    def saveHook(UserAndRolesAuthContext authContext, def hookData) {
        RdWebhook hook = null
        SaveWebhookRequest saveWebhookRequest = mapperSaveRequest(hookData)
        boolean shouldUpdate = hookData.update ?: false
        if(saveWebhookRequest.id) {
            hook = webhookDataProvider.findByUuidAndProject(saveWebhookRequest.uuid,saveWebhookRequest.project)
            if (!hook) return [err: "Webhook not found"]

            shouldUpdate = true
            if(saveWebhookRequest.roles && !hookData.importData) {
                try {
                    rundeckAuthTokenManagerService.updateAuthRoles(authContext, hook.authToken,rundeckAuthTokenManagerService.parseAuthRoles(hookData.roles))
                    validateNulls(hook, saveWebhookRequest)
                } catch (Exception e) {
                    return [err: "Failed to update Auth Token roles: "+e.message]
                }
            }
        } else {
            String checkUser = hookData.user ?: authContext.username
            if (!hookData.importData && !userService.validateUserExists(checkUser)) return [err: "Webhook user '${checkUser}' not found"]
            saveWebhookRequest.setUuid(hookData.uuid ?: UUID.randomUUID().toString())

        }
        String generatedSecureString = null
        if(hookData.useAuth == true && hookData.regenAuth == true) {
            generatedSecureString = RandomStringUtils.random(32, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")
            saveWebhookRequest.setAuthConfigJson(mapper.writeValueAsString(new AuthorizationHeaderAuthenticator.Config(secret:generatedSecureString.sha256())))
        } else if(hookData.useAuth == false) {
            saveWebhookRequest.setAuthConfigJson(null)
        } else if(shouldUpdate && hookData.useAuth == true && hookData.regenAuth == false) {
            saveWebhookRequest.setAuthConfigJson(hook?.authConfigJson)
        }

        if(hookData.enabled != null) saveWebhookRequest.setEnabled(hookData.enabled)
        if(saveWebhookRequest.eventPlugin && !pluginService.listPlugins(WebhookEventPlugin).any { it.key == saveWebhookRequest.eventPlugin}){
            return [err:"Plugin does not exist: " + saveWebhookRequest.eventPlugin]
        }

        Map pluginConfig = [:]
        if(hookData.config) pluginConfig = hookData.config instanceof String ? mapper.readValue(hookData.config, HashMap) : hookData.config

        def (ValidatedPlugin vPlugin, boolean isCustom) = validatePluginConfig(saveWebhookRequest.eventPlugin, pluginConfig)
        if(!vPlugin.valid) {
            def errMsg = isCustom ?
                    "Validation errors: " + vPlugin.report.errors:
                    "Invalid plugin configuration: " + vPlugin.report.errors.collect { k, v -> "$k : $v" }.join("\n")

            return [err: errMsg, errors: vPlugin.report.errors]
        }

        saveWebhookRequest.setPluginConfigurationJson(mapper.writeValueAsString(pluginConfig))
        Set<String> roles = hookData.roles ? rundeckAuthTokenManagerService.parseAuthRoles(hookData.roles) : authContext.roles
        if((!saveWebhookRequest.id || !saveWebhookRequest.authToken) && !hookData.shouldImportToken){
            //create token
            String checkUser = hookData.user ?: authContext.username
            try {
                def at=apiService.generateUserToken(authContext, null, checkUser, roles, false,
                                                            AuthTokenType.WEBHOOK)
                saveWebhookRequest.setAuthToken(at.token)
            } catch (Exception e) {
                return [err: "Failed to create associated Auth Token: "+e.message]
            }
        }
        if(hookData.shouldImportToken) {
            if(!importIsAllowed(hook?.authToken, hookData)){
                throw new Exception("Cannot import webhook: imported auth token does not exist or was changed")
            }
            try {
                rundeckAuthTokenManagerService.importWebhookToken(authContext, hookData.authToken, hookData.user, roles)
            } catch (Exception e) {
                return [err: "Failed importing Webhook Token: "+e.message]
            }
            saveWebhookRequest.authToken = hookData.authToken
        }
        if(hookData.regenUuid){
            saveWebhookRequest.setUuid(UUID.randomUUID().toString())
        }
        SaveWebhookResponse saveWebhookResponse
        if (shouldUpdate) {
            saveWebhookResponse = hookData.regenUuid
                    ? webhookDataProvider.createWebhook(saveWebhookRequest)
                    : webhookDataProvider.updateWebhook(saveWebhookRequest)
        } else {
            saveWebhookResponse = webhookDataProvider.createWebhook(saveWebhookRequest)
        }

        if(saveWebhookResponse.isSaved) {
            def responsePayload = [msg: "Saved webhook", uuid: saveWebhookResponse.webhook.uuid]
            if(generatedSecureString) responsePayload.generatedSecurityString = generatedSecureString
            return responsePayload
        } else {
            if(!saveWebhookResponse.webhook.id && saveWebhookRequest.authToken){
                //delete the created token
                rundeckAuthTokenManagerService.deleteByTokenWithType(saveWebhookResponse.webhook.authToken, AuthTokenType.WEBHOOK)
            }
            return [err: saveWebhookResponse.errors]
        }
    }

    boolean importIsAllowed(String token, Map hookData) {
        if(token == hookData.authToken) return true
        if(!token
            && webhookDataProvider.countByAuthToken(hookData.authToken) == 0
            && !rundeckAuthTokenManagerService.getTokenWithType(
            hookData.authToken,
            AuthTokenType.WEBHOOK
        )) {
            return true
        }
        return false
    }

    void validateNulls(RdWebhook hook, SaveWebhookRequest saveWebhookRequest) {
        if(saveWebhookRequest.uuid == null && hook.uuid != null) saveWebhookRequest.setUuid(hook.uuid)
        if(saveWebhookRequest.name == null && hook.name != null) saveWebhookRequest.setName(hook.name)
        if(saveWebhookRequest.project == null && hook.project != null) saveWebhookRequest.setProject(hook.project)
        if(saveWebhookRequest.eventPlugin == null && hook.eventPlugin != null) saveWebhookRequest.setEventPlugin(hook.eventPlugin)
        if(hook.authToken != null) saveWebhookRequest.setAuthToken(hook.authToken)
    }

    SaveWebhookRequest mapperSaveRequest(def hookData) {
        ObjectMapper requestMapper = new ObjectMapper();
        requestMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        return requestMapper.convertValue(hookData, SaveWebhookRequest.class);
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
        webhookDataProvider.findAllByProject(project).each { webhook ->
            delete(webhook)
        }
    }

    def delete(RdWebhook hook) {
        String authToken = hook.authToken
        String name = hook.name
        try {
            // Deleting all stored debug data for this particular hook from the DB
            deleteWebhookEventsData(hook)
            webhookDataProvider.deleteByUuid(hook.uuid)
            rundeckAuthTokenManagerService.deleteByTokenWithType(authToken, AuthTokenType.WEBHOOK)
            return [msg: "Deleted ${name} webhook"]
        } catch(Exception ex) {
            log.error("delete webhook failed",ex)
            return [err: ex.message]
        }
    }

    def importWebhook(UserAndRolesAuthContext authContext, Map hook, boolean regenAuthTokens,boolean regenUuid) {

        RdWebhook existing = webhookDataProvider.findByUuidAndProject(hook.uuid, hook.project)
        if(existing) {
            hook.id = existing.id
            hook.update = true
        } else {
            hook.id = null
        }
        hook.importData = true

        if(!regenAuthTokens && hook.authToken) {
            hook.shouldImportToken = true
        } else {
            hook.authToken = null
        }
        if(regenUuid){
            hook.regenUuid = true
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
        RdWebhook hook = webhookDataProvider.getWebhook(id.toLong())
        getWebhookWithAuthAsMap(hook)
    }
    def getWebhookForProjectWithAuth(String id, String project) {
        RdWebhook hook = getWebhookWithProject(id.toLong(), project)
        if(!hook){
            return null
        }
        getWebhookWithAuthAsMap(hook)
    }

    private Map getWebhookWithAuthAsMap(RdWebhook hook) {
        AuthenticationToken authToken = rundeckAuthTokenManagerService.getTokenWithType(
            hook.authToken,
                AuthTokenType.WEBHOOK
        )
        return [id:hook.id, uuid:hook.uuid, name:hook.name, project: hook.project, enabled: hook.enabled, user:authToken.ownerName, creator:authToken.creator, roles: authToken.getAuthRolesSet().join(","), authToken:hook.authToken, useAuth: hook.authConfigJson != null, regenAuth: false, eventPlugin:hook.eventPlugin, config:mapper.readValue(hook.pluginConfigurationJson, HashMap)]
    }

    RdWebhook getWebhook(Long id) {
        return webhookDataProvider.getWebhook(id)
    }
    RdWebhook getWebhookWithProject(Long id, String project) {
        return webhookDataProvider.getWebhookWithProject(id,project)
    }

    RdWebhook getWebhookByUuid(String uuid) {
        return webhookDataProvider.getWebhookByUuid(uuid)
    }

    RdWebhook getWebhookByToken(String token) {
        return webhookDataProvider.getWebhookByToken(token)
    }

    class Evt extends EventImpl {}

    class EvtQuery extends EventQueryImpl {}
}
