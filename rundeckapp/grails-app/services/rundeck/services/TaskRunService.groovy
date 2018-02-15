package rundeck.services

import com.dtolabs.rundeck.app.support.task.TaskCreate
import com.dtolabs.rundeck.app.support.task.TaskUpdate
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.server.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.server.plugins.DescribedPlugin
import com.dtolabs.rundeck.server.plugins.ValidatedPlugin
import grails.transaction.Transactional
import groovy.transform.ToString
import org.rundeck.core.tasks.TaskAction
import org.rundeck.core.tasks.TaskActionHandler
import org.rundeck.core.tasks.TaskActionInvoker
import org.rundeck.core.tasks.TaskTrigger
import org.rundeck.core.tasks.TaskTriggerHandler
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.TaskEvent
import rundeck.TaskRep

import java.time.ZoneId

@Transactional
class TaskRunService implements ApplicationContextAware, TaskActionInvoker<RDTaskContext> {

    def pluginService
    def frameworkService
    ApplicationContext applicationContext
    private Map<String, TaskTriggerHandler<RDTaskContext>> triggerRegistrationMap = [:]
    private Map<String, TaskActionHandler<RDTaskContext>> actRegistrationMap = [:]
    /**
     * handle defined triggers needing system startup hook
     */
    void init() {
        log.info("Startup: initializing trigger Service")
        def serverNodeUuid = frameworkService.serverUUID
        Map<String, TaskTriggerHandler<RDTaskContext>> startupHandlers = taskTriggerHandlerMap?.findAll {
            it.value.onStartup()
        }
        log.debug("Startup: TriggerService: startupHandlers: ${startupHandlers}")
        def tasks = listEnabledTasksForMember(serverNodeUuid)
        //TODO: passive mode behavior
        log.debug("Startup: TaskRunService: tasks: ${tasks}")
        tasks.each { TaskRep task ->
            def validation=validateTask(task)
            if(validation.error) {
                log.warn("Validation failed for Task ${task.uuid} at startup: " + validation.validation)
                return
            }
            def trigger = triggerFor(task)
            RDTaskContext taskContext = contextForTask(task)
            TaskTriggerHandler<RDTaskContext> bean = startupHandlers.find {
                it.value.handlesTrigger(trigger, taskContext)
            }?.value
            if (bean) {
                def action = actionFor(task)
                bean.registerTriggerForAction(
                        task.uuid,
                        taskContext,
                        trigger,
                        action,
                        this
                )
                triggerRegistrationMap[task.uuid] = bean
                log.warn("Startup: registered trigger handler for: ${task.uuid} with triggerType: ${task.triggerType}")
            } else {
                log.warn("Startup: No TaskTriggerHandler instance found to handle this task: ${task.uuid} with triggerType: ${task.triggerType}")
            }
        }

    }

    public RDTaskContext contextForTask(TaskRep task) {
        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForUserAndRolesAndProject(
                task.authUser,
                task.authRoleList.split(',').toList(),
                task.project
        )
        new RDTaskContext(clusterContextInfo + [project: task.project, authContext: authContext])
    }

    public Map<String, DescribedPlugin<TaskTrigger>> getTaskTriggerPluginDescriptions() {
        pluginService.listPlugins(TaskTrigger)
    }

    public Map<String, DescribedPlugin<TaskAction>> getTaskActionPluginDescriptions() {
        pluginService.listPlugins(TaskAction)
    }

    public DescribedPlugin<TaskAction> getTaskActionPlugin(String provider) {
        pluginService.getPluginDescriptor(provider, TaskAction)
    }

    public ConfiguredPlugin<TaskAction> getConfiguredActionPlugin(String provider, Map config) {
        //TODO: project scope
        pluginService.configurePlugin(provider, config, TaskAction)
    }

    public ValidatedPlugin getValidatedActionPlugin(String provider, Map config) {
        //TODO: project scope
        pluginService.validatePluginConfig(provider, TaskAction, config)
    }

    public DescribedPlugin<TaskTrigger> getTaskTriggerPlugin(String provider) {
        pluginService.getPluginDescriptor(provider, TaskTrigger)
    }

    public ConfiguredPlugin<TaskTrigger> getConfiguredTriggerPlugin(String provider, Map config) {
        //TODO: project scope
        pluginService.configurePlugin(provider, config, TaskTrigger)
    }

    public ValidatedPlugin getValidatedTriggerPlugin(String provider, Map config) {
        //TODO: project scope
        pluginService.validatePluginConfig(provider, TaskTrigger, config)
    }
    /**
     * Map of installed trigger handlers
     * @return
     */
    public Map<String, TaskTriggerHandler<RDTaskContext>> getTaskTriggerHandlerMap() {
        return pluginService.listPlugins(TaskTriggerHandler).collectEntries { [it.key, it.value.instance] }
    }

    /**
     * Map of installed trigger handlers
     * @return
     */
    public Map<String, TaskActionHandler<RDTaskContext>> getTriggerActionHandlerMap() {
        pluginService.listPlugins(TaskActionHandler).collectEntries { [it.key, it.value.instance] }

    }

    public Map getClusterContextInfo() {
        [
                clusterModeEnabled: frameworkService.isClusterModeEnabled(),
                serverNodeUUID    : frameworkService.serverUUID
        ]
    }

    public List<TaskRep> listEnabledTasks() {
        TaskRep.findAllByEnabled(true)
    }

    public List<TaskRep> listEnabledTasksForMember(String serverNodeUuid) {
        if (serverNodeUuid) {
            TaskRep.findAllByEnabledAndServerNodeUuid(true, serverNodeUuid)
        } else {
            listEnabledTasks()
        }
    }

    public List<TaskRep> listEnabledTasksByProject(String project) {
        TaskRep.findAllByEnabledAndProject(true, project)
    }

    TaskTrigger triggerFor(TaskRep rep) {
        def condition = getConfiguredTriggerPlugin(rep.triggerType, rep.triggerConfig)
        if (!condition) {
            throw new IllegalArgumentException("Unknown condition type: ${rep.triggerType}")
        }
        return condition.instance
    }

    TaskAction actionFor(TaskRep rep) {
        def action = getConfiguredActionPlugin(rep.actionType, rep.actionConfig)
        if (!action) {
            throw new IllegalArgumentException("Unknown action type: ${rep.actionType}")
        }
        return action.instance
    }

    ValidatedPlugin validateActionFor(TaskRep rep) {
        rep.actionType ? getValidatedActionPlugin(rep.actionType, rep.actionConfig) : null
    }

    ValidatedPlugin validateTriggerFor(TaskRep rep) {
        rep.triggerType ? getValidatedTriggerPlugin(rep.triggerType, rep.triggerConfig) : null
    }

    private def registerTask(TaskRep task, boolean enabled) {

        def trigger = triggerFor(task)
        def action = actionFor(task)
        def taskContext = contextForTask(task)
        TaskTriggerHandler condHandler = getTriggerHandlerForTask(task, trigger, taskContext)

        if (!condHandler) {
            log.warn("No TaskTriggerHandler instance found to handle this task: ${task.uuid} with triggerType: ${task.triggerType}")
            return
        }

        if (enabled) {
            condHandler.registerTriggerForAction(
                    task.uuid,
                    taskContext,
                    trigger,
                    action,
                    this
            )
            triggerRegistrationMap[task.uuid] = condHandler

        } else {

            condHandler.deregisterTriggerForAction(
                    task.uuid,
                    taskContext,
                    trigger,
                    action,
                    this
            )
            triggerRegistrationMap.remove task.uuid
        }
    }

    public TaskTriggerHandler<RDTaskContext> getTriggerHandlerForTask(TaskRep trigger, TaskTrigger condition, RDTaskContext triggerContext) {
        triggerRegistrationMap[trigger.uuid] ?: taskTriggerHandlerMap.find {
            it.value.handlesTrigger(condition, triggerContext)
        }?.value
    }

    public TaskActionHandler<RDTaskContext> getActionHandlerForTask(TaskRep trigger, TaskAction action, RDTaskContext triggerContext) {
        actRegistrationMap[trigger.uuid] ?: triggerActionHandlerMap.find {
            it.value.handlesAction(action, triggerContext)
        }?.value
    }

    def createTask(
            UserAndRolesAuthContext authContext,
            TaskCreate input,
            Map triggerMap,
            Map actionMap,
            Map userData
    ) {
        def rep = new TaskRep(
                uuid: UUID.randomUUID().toString(),
                name: input.name,
                description: input.description,
                project: input.project,
                triggerType: input.triggerType,
                triggerConfig: triggerMap,
                actionType: input.actionType,
                actionConfig: actionMap,
                userData: userData,
                enabled: input.enabled,
                userCreated: authContext.username,
                userModified: authContext.username,
                authUser: authContext.username,
                authRoleList: authContext.roles.join(','),
                serverNodeUuid: frameworkService.serverUUID
        )
        def result = validateTask(rep)
        if (result.error) {
            return result
        }
        rep.save(flush: true)
        registerTask rep, rep.enabled
        return [error: false, task: rep]
    }

    Map validateTask(TaskRep rep) {
        rep.validate()
        def validation = [:]
        //validate plugin config

        ValidatedPlugin actionValidate = validateActionFor(rep)
        if (!actionValidate && rep.actionType) {
            //plugin provider not found
            rep.errors.rejectValue(
                    'actionType',
                    'plugin.not.found.0',
                    [rep.actionType].toArray(),
                    'Plugin not found: {0}'
            )
        } else if (actionValidate && !actionValidate.valid) {
            validation[ServiceNameConstants.TaskAction] = actionValidate.report.errors
        }

        ValidatedPlugin triggerValidate = validateTriggerFor(rep)
        if (!triggerValidate && rep.triggerType) {
            //plugin provider not found
            rep.errors.rejectValue(
                    'triggerType',
                    'plugin.not.found.0',
                    [rep.triggerType].toArray(),
                    'Plugin not found: {0}'
            )
        } else if (triggerValidate && !triggerValidate.valid) {
            validation[ServiceNameConstants.TaskTrigger] = triggerValidate.report.errors
        }
        if (rep.hasErrors() || validation) {
            return [error: true, task: rep, validation: validation]
        }
        return [error: false, task: rep]
    }

    Map updateTask(
            UserAndRolesAuthContext authContext,
            TaskRep task,
            TaskUpdate input,
            Map triggerDataMap,
            Map actionDataMap,
            Map userDataMap
    ) {
        task.with {
            name = input.name
            description = input.description
            triggerType = input.triggerType
            triggerConfig = triggerDataMap
            actionType = input.actionType
            actionConfig = actionDataMap
            userData = userDataMap
            enabled = input.enabled
            userModified = authContext.username
            authUser = authContext.username
            authRoleList = authContext.roles.join(',')
            serverNodeUuid = frameworkService.serverUUID
        }
        def result = validateTask(task)
        if (result.error) {
            task.discard()

            return result
        }
        task.save(flush: true)
        registerTask task, task.enabled

        return [error: false, task: task]
    }

    boolean deleteTask(TaskRep task) {
        registerTask task, false
        task.delete(flush: true)
        true
    }

    /**
     * Submit a condition to indicate a trigger
     * @param taskId
     * @param triggerMap
     */
    void taskTriggerFired(String taskId, RDTaskContext contextInfo, Map triggerMap) {
        def task = TaskRep.findByUuid(taskId)

        def event = new TaskEvent(
                eventDataMap: triggerMap,
                timeZone: ZoneId.systemDefault().toString(),
                eventType: 'fired',
                taskRep: task
        )
        event.save(flush: true)

        def action = actionFor(task)
        def trigger = triggerFor(task)

        TaskActionHandler actHandler = getActionHandlerForTask(task, action, contextInfo)
        //TODO: on executor

        try {
            def result = actHandler.performTaskAction(taskId, contextInfo, triggerMap, trigger, action)
            def event2 = new TaskEvent(
                    eventDataMap: result,
                    timeZone: ZoneId.systemDefault().toString(),
                    eventType: 'result',
                    taskRep: task,
                    associatedId: result?.associatedId,
                    associatedType: result?.associatedType,
                    associatedEvent: event
            )
            event2.save(flush: true)
        } catch (Throwable t) {
            log.error("Failed to run task action for $taskId: $t.message", t)
            def event2 = new TaskEvent(
                    eventDataMap: [error: t.message],
                    timeZone: ZoneId.systemDefault().toString(),
                    eventType: 'error',
                    taskRep: task,
                    associatedEvent: event
            )
            event2.save(flush: true)
        }
    }
}

@ToString(includeFields = true, includeNames = true)
class RDTaskContext {
    String project
    String serverNodeUUID
    boolean clusterModeEnabled
    UserAndRolesAuthContext authContext
}

