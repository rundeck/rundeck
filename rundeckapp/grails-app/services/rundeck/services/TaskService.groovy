package rundeck.services

import com.dtolabs.rundeck.app.support.task.TaskCreate
import com.dtolabs.rundeck.app.support.task.TaskUpdate
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.jobs.JobService
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.server.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.server.plugins.DescribedPlugin
import com.dtolabs.rundeck.server.plugins.ValidatedPlugin
import grails.transaction.Transactional
import groovy.transform.ToString
import org.rundeck.core.tasks.*
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.TaskEvent
import rundeck.TaskRep

import java.time.ZoneId

@Transactional
class TaskService implements ApplicationContextAware, TaskActionInvoker<RDTaskContext> {

    def pluginService
    def frameworkService
    def storageService
    def jobStateService
    ApplicationContext applicationContext
    private Map<String, TaskTriggerHandler<RDTaskContext>> triggerRegistrationMap = [:]
    private Map<String, TaskConditionHandler<RDTaskContext>> conditionRegistrationMap = [:]
    private Map<String, TaskActionHandler<RDTaskContext>> actRegistrationMap = [:]
    /**
     * handle defined triggers needing system startup hook
     */
    void init() {
        log.info("Startup: initializing Task Service")
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
                def conditions = conditionsFor(task)
                try {
                    bean.registerTriggerForAction(
                        taskContext,
                        trigger,
                        conditions,
                        action,
                        this
                    )
                    triggerRegistrationMap[task.uuid] = bean
                    log.info(
                        "Startup: registered trigger handler for: ${task.uuid} with triggerType: ${task.triggerType}"
                    )
                } catch (TriggerException e) {
                    log.error(
                        "FAILED: register trigger handler for: ${task.uuid} with triggerType: ${task.triggerType}",
                        e
                    )
                    createTaskEvent(
                        [error: e.message],
                        task,
                        'warn:trigger:register',
                        null,
                        null,
                        null
                    )
                }
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
        new RDTaskContext(
            project: task.project,
            authContext: authContext,
            taskId: task.uuid,
            clusterModeEnabled: frameworkService.isClusterModeEnabled(),
            serverNodeUUID: frameworkService.serverUUID,
            framework: frameworkService.rundeckFramework,
            storageTree: storageService.storageTreeWithContext(authContext),
            jobService: jobStateService.jobServiceWithAuthContext(authContext)
        )
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

    public ConfiguredPlugin<TaskCondition> getConfiguredConditionPlugin(String provider, Map config) {
        //TODO: project scope
        pluginService.configurePlugin(provider, config, TaskCondition)
    }

    public ValidatedPlugin getValidatedActionPlugin(String provider, Map config) {
        //TODO: project scope
        pluginService.validatePluginConfig(provider, TaskAction, config)
    }

    public ValidatedPlugin getValidatedConditionPlugin(String provider, Map config) {
        //TODO: project scope
        pluginService.validatePluginConfig(provider, TaskCondition, config)
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
        pluginService.listPlugins(TaskTriggerHandler).collectEntries { [it.key, it.value.instance] }
    }

    /**
     * Map of installed trigger handlers
     * @return
     */
    public Map<String, TaskConditionHandler<RDTaskContext>> getTaskConditionHandlerMap() {
        pluginService.listPlugins(TaskConditionHandler).collectEntries { [it.key, it.value.instance] }
    }

    /**
     * Map of installed trigger handlers
     * @return
     */
    public Map<String, TaskActionHandler<RDTaskContext>> getTriggerActionHandlerMap() {
        pluginService.listPlugins(TaskActionHandler).collectEntries { [it.key, it.value.instance] }

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

    List<TaskCondition> conditionsFor(TaskRep rep) {
        def configList = rep.getConditionList()
        def list = configList?.collect { Map condMap ->
            getConfiguredConditionPlugin(condMap.type, condMap.config).instance
        } ?: []
        def missing = list.findIndexOf { !it }
        if (missing >= 0) {
            throw new IllegalArgumentException("Unknown condition type: ${configList[missing].type}")
        }
        list
    }

    ValidatedPlugin validateActionFor(TaskRep rep) {
        rep.actionType ? getValidatedActionPlugin(rep.actionType, rep.actionConfig) : null
    }

    List<ValidatedPlugin> validateConditionsFor(TaskRep rep) {
        List conditions = rep.getConditionList()
        conditions.collect { Map condMap ->
            condMap.type ?
            getValidatedConditionPlugin(condMap.type, condMap.config) :
            new ValidatedPlugin(valid: false, report: Validator.errorReport('type', 'missing'))
        }
    }

    ValidatedPlugin validateTriggerFor(TaskRep rep) {
        rep.triggerType ? getValidatedTriggerPlugin(rep.triggerType, rep.triggerConfig) : null
    }

    /**
     * Handle registration of the task trigger
     * @param task
     * @param enabled true if it should be registered, false to be deregistered
     * @return true if successfully registered or degistered
     */
    private boolean processTaskRegistration(TaskRep task, boolean enabled) {

        def trigger = triggerFor(task)
        def taskContext = contextForTask(task)
        TaskTriggerHandler triggerHandler = getTriggerHandlerForTask(task, trigger, taskContext)

        if (!triggerHandler) {
            log.warn("No TaskTriggerHandler instance found to handle this task: ${task.uuid} with triggerType: ${task.triggerType}")
            return
        }

        def action = actionFor(task)

        def conditions = conditionsFor(task)

        if (enabled) {
            try {
                triggerHandler.registerTriggerForAction(
                    taskContext,
                    trigger,
                    conditions,
                    action,
                    this
                )
                triggerRegistrationMap[task.uuid] = triggerHandler
            } catch (TriggerException e) {
                log.error("FAILED: register trigger handler for: ${task.uuid} with triggerType: ${task.triggerType}", e)
                createTaskEvent(
                    [error: e.message],
                    task,
                    'warn:trigger:register',
                    null,
                    null,
                    null
                )
                return false
            }
        } else {
            try {
                triggerHandler.deregisterTriggerForAction(
                    taskContext,
                    trigger,
                    conditions,
                    action,
                    this
                )
                triggerRegistrationMap.remove task.uuid
            } catch (TriggerException e) {
                log.error(
                    "FAILED: deregister trigger handler for: ${task.uuid} with triggerType: ${task.triggerType}",
                    e
                )
                createTaskEvent(
                    [error: e.message],
                    task,
                    'warn:trigger:deregister',
                    null,
                    null,
                    null
                )
                return false
            }
        }
        true
    }

    public TaskTriggerHandler<RDTaskContext> getTriggerHandlerForTask(
        TaskRep task,
        TaskTrigger trigger,
        RDTaskContext taskContext
    ) {
        triggerRegistrationMap[task.uuid] ?: taskTriggerHandlerMap.find {
            it.value.handlesTrigger(trigger, taskContext)
        }?.value
    }

    public TaskConditionHandler<RDTaskContext> getConditionHandlerForCondition(
        TaskRep trigger,
        TaskCondition condition,
        RDTaskContext taskContext
    ) {
        conditionRegistrationMap[trigger.uuid] ?: taskConditionHandlerMap.find {
            it.value.handlesCondition(condition, taskContext)
        }?.value
    }


    public TaskActionHandler<RDTaskContext> getActionHandlerForTask(
        TaskRep trigger,
        TaskAction action,
        RDTaskContext taskContext
    ) {
        actRegistrationMap[trigger.uuid] ?: triggerActionHandlerMap.find {
            it.value.handlesAction(action, taskContext)
        }?.value
    }

    def createTask(
        UserAndRolesAuthContext authContext,
        TaskCreate input,
        Map triggerMap,
        List conditionList,
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
            conditionList: conditionList,
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
        boolean regresult = processTaskRegistration rep, rep.enabled
        return [error: false, task: rep, registration: regresult]
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
            validation[TaskPluginTypes.TaskAction] = actionValidate.report.errors
        }

        List<ValidatedPlugin> conditionValidate = validateConditionsFor(rep)
        if (!conditionValidate && rep.conditionList) {
            //plugin provider not found
            rep.errors.rejectValue(
                'conditionList',
                'plugin.not.found.0',
                [rep.actionType].toArray(),
                'Plugin not found: {0}'
            )
        } else if (conditionValidate && !conditionValidate.every { it && it.valid }) {
            validation[TaskPluginTypes.TaskCondition] = conditionValidate.report.errors
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
            validation[TaskPluginTypes.TaskTrigger] = triggerValidate.report.errors
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
        List condList,
        Map actionDataMap,
        Map userDataMap
    ) {
        task.with {
            name = input.name
            description = input.description
            triggerType = input.triggerType
            triggerConfig = triggerDataMap
            conditionList = condList
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
        boolean regresult = processTaskRegistration task, task.enabled

        return [error: false, task: task, registration: regresult]
    }

    boolean deleteTask(TaskRep task) {
        processTaskRegistration task, false
        task.delete(flush: true)
        true
    }

    /**
     * Submit a condition to indicate a trigger
     * @param taskId
     * @param triggerMap
     */
    void taskTriggerFired(RDTaskContext contextInfo, Map triggerMap) {
        def task = TaskRep.findByUuid(contextInfo.taskId)

        def event = createTaskEvent(
            triggerMap,
            task,
            'fired',
            null,
            null,
            null
        )

        def action = actionFor(task)
        def trigger = triggerFor(task)
        def conditions = conditionsFor(task)
        boolean conditionsMet = true
        int index = 0
        Map conditionData = null
        for (TaskCondition condition : conditions) {
            TaskConditionHandler handler = getConditionHandlerForCondition(task, condition, contextInfo)
            def condition1 = handler.checkCondition(contextInfo, task.userData?:[:], triggerMap, trigger, condition)
            log.error("Condition check met? ${condition1.conditionMet}: $condition1.conditionData")
            if (!condition1.conditionMet) {
                conditionData = condition1.conditionData
                conditionsMet = false
                break
            }
            index++
        }

        if (!conditionsMet) {
            createTaskEvent(
                [
                    message  : 'Trigger was fired, but a condition was not met',
                    condition: [
                        index: index,
                        data : conditionData ?: [:]
                    ]
                ],
                task,
                'condition:notmet',
                null,
                null,
                event
            )
            return
        }

        TaskActionHandler actHandler = getActionHandlerForTask(task, action, contextInfo)
        //TODO: on executor

        try {
            def result = actHandler.performTaskAction(
                    contextInfo,
                    [trigger: triggerMap, task: task.userData],
                    trigger,
                    action
            )
            createTaskEvent(
                result,
                task,
                'result',
                result?.associatedId,
                result?.associatedType,
                event
            )
        } catch (Throwable t) {
            log.error("Failed to run task action for ${contextInfo.taskId}: $t.message", t)
            createTaskEvent(
                [error: t.message],
                task,
                'error:action:perform',
                null,
                null,
                event
            )
        }
    }

    public TaskEvent createTaskEvent(
        Map triggerMap, TaskRep task, String eventType, associatedId, associatedType, associatedEvent
    ) {
        new TaskEvent(
            eventDataMap: triggerMap,
            timeZone: ZoneId.systemDefault().toString(),
            eventType: eventType,
            taskRep: task,

            associatedId: associatedId,
            associatedType: associatedType,
            associatedEvent: associatedEvent
        ).save(flush: true)
    }

}

@ToString(includeFields = true, includeNames = true)
class RDTaskContext implements TaskContext {
    String taskId
    String project
    String serverNodeUUID
    boolean clusterModeEnabled
    UserAndRolesAuthContext authContext
    IFramework framework
    StorageTree storageTree
    JobService jobService
}

