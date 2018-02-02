package rundeck.services

import com.dtolabs.rundeck.app.support.trigger.TriggerCreate
import com.dtolabs.rundeck.app.support.trigger.TriggerUpdate
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.server.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.server.plugins.DescribedPlugin
import com.dtolabs.rundeck.server.plugins.trigger.action.JobRunTriggerAction
import com.dtolabs.rundeck.server.plugins.trigger.condition.ScheduleTriggerCondition
import grails.transaction.Transactional
import groovy.transform.ToString
import org.rundeck.core.triggers.*
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.TriggerFiredEvent
import rundeck.TriggerRep

import java.time.ZoneId

@Transactional
class TriggerService implements ApplicationContextAware, TriggerActionInvoker<RDTriggerContext> {

    def pluginService
    def frameworkService
    ApplicationContext applicationContext
    private Map<String, TriggerConditionHandler<RDTriggerContext>> condRegistrationMap = [:]
    private Map<String, TriggerActionHandler<RDTriggerContext>> actRegistrationMap = [:]
    /**
     * handle defined triggers needing system startup hook
     */
    void init() {
        log.info("Startup: initializing trigger Service")
        Map<String, TriggerConditionHandler<RDTriggerContext>> startupHandlers = triggerConditionHandlerMap?.findAll {
            it.value.onStartup()
        }
        log.debug("Startup: TriggerService: startupHandlers: ${startupHandlers}")
        def triggers = listEnabledTriggers()
        log.debug("Startup: TriggerService: triggers: ${triggers}")
        triggers.each { TriggerRep trigger ->
            def condition = conditionFor(trigger)
            RDTriggerContext triggerContext = contextForTrigger(trigger)
            TriggerConditionHandler<RDTriggerContext> bean = startupHandlers.find {
                it.value.handlesConditionChecks(condition, triggerContext)
            }?.value
            if (bean) {
                def action = actionFor(trigger)
                bean.registerConditionChecksForAction(
                        trigger.uuid,
                        triggerContext,
                        condition,
                        action,
                        this
                )
                condRegistrationMap[trigger.uuid] = bean
                log.warn("Startup: registered condition handler for: ${trigger.uuid} with conditionType: ${trigger.conditionType}")
            } else {
                log.warn("Startup: No TriggerConditionHandler instance found to handle this trigger: ${trigger.uuid} with conditionType: ${trigger.conditionType}")
            }
        }

    }

    public RDTriggerContext contextForTrigger(TriggerRep trigger) {
        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForUserAndRolesAndProject(
                trigger.authUser,
                trigger.authRoleList.split(',').toList(),
                trigger.project
        )
        new RDTriggerContext(clusterContextInfo + [project: trigger.project, authContext: authContext])
    }

    public Map<String, DescribedPlugin<Condition>> getTriggerConditionPluginDescriptions() {
        pluginService.listPlugins(Condition)
    }

    public Map<String, DescribedPlugin<Action>> getTriggerActionPluginDescriptions() {
        pluginService.listPlugins(Action)
    }

    public DescribedPlugin<Action> getTriggerActionPlugin(String provider) {
        pluginService.getPluginDescriptor(provider, Action)
    }

    public ConfiguredPlugin<Action> getConfiguredActionPlugin(String provider, Map config) {
        //TODO: project scope
        pluginService.configurePlugin(provider, config, Action)
    }

    public DescribedPlugin<Condition> getTriggerConditionPlugin(String provider) {
        pluginService.getPluginDescriptor(provider, Condition)
    }

    public ConfiguredPlugin<Condition> getConfiguredConditionPlugin(String provider, Map config) {
        //TODO: project scope
        pluginService.configurePlugin(provider, config, Condition)
    }
    /**
     * Map of installed trigger handlers
     * @return
     */
    public Map<String, TriggerConditionHandler<RDTriggerContext>> getTriggerConditionHandlerMap() {
        def plugins = pluginService.listPlugins(TriggerConditionHandler).collectEntries { [it.key, it.value.instance] }


        plugins
//        //TODO: plugins
//        applicationContext.getBeansOfType(
//                TriggerConditionHandler
//        )?.findAll {
//            !it.key.endsWith('Profiled')
//        }
    }

    /**
     * Map of installed trigger handlers
     * @return
     */
    public Map<String, TriggerActionHandler<RDTriggerContext>> getTriggerActionHandlerMap() {
        pluginService.listPlugins(TriggerActionHandler).collectEntries { [it.key, it.value.instance] }
//        //TODO: plugins
//        applicationContext.getBeansOfType(
//                TriggerActionHandler
//        )?.findAll {
//            !it.key.endsWith('Profiled')
//        }
    }

    public Map getClusterContextInfo() {
        [
                clusterModeEnabled: frameworkService.isClusterModeEnabled(),
                serverNodeUUID    : frameworkService.serverUUID
        ]
    }

    public List<TriggerRep> listEnabledTriggers() {
        TriggerRep.findAllByEnabled(true)
    }

    public List<TriggerRep> listEnabledTriggersByProject(String project) {
        TriggerRep.findAllByEnabledAndProject(true, project)
    }

    Condition conditionFor(TriggerRep rep) {
        def condition = getConfiguredConditionPlugin(rep.conditionType, rep.conditionConfig)
        if (!condition) {
            throw new IllegalArgumentException("Unknown condition type: ${rep.conditionType}")
        }
        return condition.instance
    }

    Action actionFor(TriggerRep rep) {
        def action = getConfiguredActionPlugin(rep.actionType, rep.actionConfig)
        if (!action) {
            throw new IllegalArgumentException("Unknown action type: ${rep.conditionType}")
        }
        return action.instance
    }

    private def registerTrigger(TriggerRep trigger, boolean enabled) {

        def condition = conditionFor(trigger)
        def action = actionFor(trigger)
        def triggerContext = contextForTrigger(trigger)
        TriggerConditionHandler condHandler = getConditionHandlerForTrigger(trigger, condition, triggerContext)

        if (!condHandler) {
            log.warn("No TriggerConditionHandler instance found to handle this trigger: ${trigger.uuid} with conditionType: ${trigger.conditionType}")
            return
        }

        if (enabled) {
            condHandler.registerConditionChecksForAction(
                    trigger.uuid,
                    triggerContext,
                    condition,
                    action,
                    this
            )
            condRegistrationMap[trigger.uuid] = condHandler

        } else {

            condHandler.deregisterConditionChecksForAction(
                    trigger.uuid,
                    triggerContext,
                    condition,
                    action,
                    this
            )
            condRegistrationMap.remove trigger.uuid
        }
    }

    public TriggerConditionHandler<RDTriggerContext> getConditionHandlerForTrigger(TriggerRep trigger, Condition condition, RDTriggerContext triggerContext) {
        condRegistrationMap[trigger.uuid] ?: triggerConditionHandlerMap.find {
            it.value.handlesConditionChecks(condition, triggerContext)
        }?.value
    }

    public TriggerActionHandler<RDTriggerContext> getActionHandlerForTrigger(TriggerRep trigger, Action action, RDTriggerContext triggerContext) {
        actRegistrationMap[trigger.uuid] ?: triggerActionHandlerMap.find {
            it.value.handlesAction(action, triggerContext)
        }?.value
    }

    TriggerRep createTrigger(UserAndRolesAuthContext authContext, TriggerCreate input, Map conditionMap, Map actionMap, Map userData) {
        def rep = new TriggerRep(
                uuid: UUID.randomUUID().toString(),
                name: input.name,
                description: input.description,
                project: input.project,
                conditionType: input.conditionType,
                conditionConfig: conditionMap,
                actionType: input.actionType,
                actionConfig: actionMap,
                userData: userData,
                enabled: input.enabled,
                userCreated: authContext.username,
                userModified: authContext.username,
                authUser: authContext.username,
                authRoleList: authContext.roles.join(','),
        )

        rep.save(flush: true)
        registerTrigger rep, rep.enabled
        return rep
    }

    TriggerRep updateTrigger(UserAndRolesAuthContext authContext, TriggerRep trigger, TriggerUpdate input, Map conditionDataMap, Map actionDataMap, Map userDataMap) {
        trigger.with {
            name = input.name
            description = input.description
            conditionType = input.conditionType
            conditionConfig = conditionDataMap
            actionType = input.actionType
            actionConfig = actionDataMap
            userData = userDataMap
            enabled = input.enabled
            userModified = authContext.username
            authUser = authContext.username
            authRoleList = authContext.roles.join(',')
        }
        trigger.save(flush: true)
        registerTrigger trigger, trigger.enabled
        return trigger
    }

    boolean deleteTrigger(TriggerRep trigger) {
        registerTrigger trigger, false
        trigger.delete(flush: true)
        true
    }

    /**
     * Submit a condition to indicate a trigger
     * @param triggerId
     * @param conditionMap
     */
    void triggerConditionMet(String triggerId, RDTriggerContext contextInfo, Map conditionMap) {
        def trigger = TriggerRep.findByUuid(triggerId)

        def event = new TriggerFiredEvent(
                conditionMap: conditionMap,
                timeZone: ZoneId.systemDefault().toString(),
                state: 'fired',
                triggerRep: trigger
        )
        event.save(flush: true)
        //TODO: save event

        def action = actionFor(trigger)
        def condition = conditionFor(trigger)

        TriggerActionHandler actHandler = getActionHandlerForTrigger(trigger, action, contextInfo)
        //TODO: on executor

        try {
            actHandler.performTriggerAction(triggerId, contextInfo, conditionMap, condition, action)
        } catch (Throwable t) {
            log.error("Failed to run trigger action for $triggerId: $t.message", t)
            def event2 = new TriggerFiredEvent(
                    conditionMap: conditionMap,
                    timeZone: ZoneId.systemDefault().toString(),
                    state: 'failed',
                    triggerRep: trigger
            )
            event2.save(flush: true)
        }
    }


    Trigger createTrigger(TriggerRep triggerRep) {
        Condition conditionRep = conditionFor(triggerRep)
        Action actionRep = actionFor(triggerRep)
        new TriggerImpl(
                name: triggerRep.name,
                description: triggerRep.description,
                id: triggerRep.uuid,
                userData: triggerRep.userData,
                condition: conditionRep,
                action: actionRep
        )
    }
}

class TriggerImpl implements Trigger {
    String name
    String description
    String id
    Map userData
    Condition condition
    Action action
}


@ToString(includeFields = true, includeNames = true)
class RDTriggerContext {
    String project
    String serverNodeUUID
    boolean clusterModeEnabled
    UserAndRolesAuthContext authContext
}

