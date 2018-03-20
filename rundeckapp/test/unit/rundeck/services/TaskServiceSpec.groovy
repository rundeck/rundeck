package rundeck.services

import com.dtolabs.rundeck.server.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.server.plugins.DescribedPlugin
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.rundeck.core.tasks.ActionFailed
import org.rundeck.core.tasks.ConditionCheck
import org.rundeck.core.tasks.TaskAction
import org.rundeck.core.tasks.TaskActionHandler
import org.rundeck.core.tasks.TaskCondition
import org.rundeck.core.tasks.TaskConditionHandler
import org.rundeck.core.tasks.TaskTrigger
import rundeck.TaskEvent
import rundeck.TaskRep
import spock.lang.Specification
import spock.lang.Unroll

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(TaskService)

@Mock([TaskRep, TaskEvent])
class TaskServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "trigger fired no conditions"() {
        given:
        TaskRep rep = new TaskRep(
            uuid: UUID.randomUUID().toString(),
            serverNodeUuid: UUID.randomUUID().toString(),
            triggerType: 'atrigger',
            triggerConfig: [trigger: 'data'],
            project: 'testProject',
            actionType: 'anaction',
            actionConfig: [action: 'data'],
            userCreated: 'bob',
            userModified: 'bob',
            authUser: 'bob',
            authRoleList: 'a,b,c'

        )
        if (!rep.validate()) {
            rep.errors.allErrors.each { println it }
        }
        rep = rep.save()
        def context = new RDTaskContext(
            taskId: rep.uuid,
            )
        def triggerData = [data: 'value']
        service.pluginService = Mock(PluginService)
        def mockAction = Mock(TaskAction)
        def mockTrigger = Mock(TaskTrigger)
        def mockActionHandler = Mock(TaskActionHandler) {
            handlesAction(mockAction, _) >> true
            1 * performTaskAction(context, triggerData, _, mockTrigger, mockAction, service) >> [result: 'data']
            0 * _(*_)
        }

        when:
        service.taskTriggerFired(context, triggerData)

        then:
        1 * service.pluginService.listPlugins(TaskActionHandler) >> [test: new DescribedPlugin<TaskActionHandler>(
            mockActionHandler,
            null,
            'test',
            null
        )]
        1 * service.pluginService.configurePlugin('anaction', [action: 'data'], TaskAction, 'testProject') >>
        new ConfiguredPlugin<TaskAction>(
            mockAction,
            [config: 'data']
        )
        1 * service.pluginService.configurePlugin('atrigger', [trigger: 'data'], TaskTrigger, 'testProject') >>
        new ConfiguredPlugin<TaskTrigger>(
            mockTrigger,
            [config: 'data']
        )
        0 * service.pluginService._(*_)
        TaskEvent.count() == 2
        TaskEvent.list().find { it.eventType == 'fired' } != null
        TaskEvent.list().find { it.eventType == 'result' } != null
        TaskEvent.list().find { it.eventType == 'result' }.eventDataMap == [result: 'data']
    }

    void "trigger fired successful conditions"() {
        given:
        TaskRep rep = new TaskRep(
            uuid: UUID.randomUUID().toString(),
            serverNodeUuid: UUID.randomUUID().toString(),
            triggerType: 'atrigger',
            triggerConfig: [trigger: 'data'],
            project: 'testProject',
            actionType: 'anaction',
            actionConfig: [action: 'data'],
            conditionList: [[type: 'condA', config: [a: 'b']], [type: 'condB', config: [c: 'd']]],
            userCreated: 'bob',
            userModified: 'bob',
            authUser: 'bob',
            authRoleList: 'a,b,c'

        )
        if (!rep.validate()) {
            rep.errors.allErrors.each { println it }
        }
        rep = rep.save()
        def context = new RDTaskContext(
            taskId: rep.uuid,
            )
        def triggerData = [data: 'value']
        service.pluginService = Mock(PluginService)
        def mockAction = Mock(TaskAction)
        def mockTrigger = Mock(TaskTrigger)
        def mockConditionA = Mock(TaskCondition)
        def mockConditionB = Mock(TaskCondition)
        def mockActionHandler = Mock(TaskActionHandler) {
            handlesAction(mockAction, _) >> true
            1 * performTaskAction(context, triggerData, _, mockTrigger, mockAction, service) >> [result: 'data']
            0 * _(*_)
        }
        def mockConditionHandler = Mock(TaskConditionHandler) {
            handlesCondition(mockConditionA, _) >> true
            handlesCondition(mockConditionB, _) >> true
            1 * checkCondition(_, _, _, mockTrigger, mockConditionA) >> ConditionCheck.result(true, [a: 'bb'])
            1 * checkCondition(_, _, _, mockTrigger, mockConditionB) >> ConditionCheck.result(true, [c: 'dd'])
            0 * _(*_)
        }

        when:
        service.taskTriggerFired(context, triggerData)

        then:
        1 * service.pluginService.listPlugins(TaskActionHandler) >> [test: new DescribedPlugin<TaskActionHandler>(
            mockActionHandler,
            null,
            'test',
            null
        )]
        2 * service.pluginService.listPlugins(TaskConditionHandler) >> [test: new DescribedPlugin<TaskConditionHandler>(
            mockConditionHandler,
            null,
            'test',
            null
        )]
        1 * service.pluginService.configurePlugin('anaction', [action: 'data'], TaskAction, 'testProject') >>
        new ConfiguredPlugin<TaskAction>(
            mockAction,
            [config: 'data']
        )
        1 * service.pluginService.configurePlugin('atrigger', [trigger: 'data'], TaskTrigger, 'testProject') >>
        new ConfiguredPlugin<TaskTrigger>(
            mockTrigger,
            [config: 'data']
        )
        1 * service.pluginService.configurePlugin('condA', [a: 'b'], TaskCondition, 'testProject') >>
        new ConfiguredPlugin<TaskCondition>(
            mockConditionA,
            [config: 'data']
        )
        1 * service.pluginService.configurePlugin('condB', [c: 'd'], TaskCondition, 'testProject') >>
        new ConfiguredPlugin<TaskCondition>(
            mockConditionB,
            [config: 'data']
        )
        0 * service.pluginService._(*_)
        TaskEvent.count() == 2
        TaskEvent.list().find { it.eventType == 'fired' } != null
        TaskEvent.list().find { it.eventType == 'result' } != null
        TaskEvent.list().find { it.eventType == 'result' }.eventDataMap == [result: 'data']
    }

    void "trigger fired condition plugin load failure"() {
        given:
        TaskRep rep = new TaskRep(
            uuid: UUID.randomUUID().toString(),
            serverNodeUuid: UUID.randomUUID().toString(),
            triggerType: 'atrigger',
            triggerConfig: [trigger: 'data'],
            project: 'testProject',
            actionType: 'anaction',
            actionConfig: [action: 'data'],
            conditionList: [[type: 'condA', config: [a: 'b']]],
            userCreated: 'bob',
            userModified: 'bob',
            authUser: 'bob',
            authRoleList: 'a,b,c'

        )
        if (!rep.validate()) {
            rep.errors.allErrors.each { println it }
        }
        rep = rep.save()
        def context = new RDTaskContext(
            taskId: rep.uuid,
            )
        def triggerData = [data: 'value']
        service.pluginService = Mock(PluginService)
        def mockAction = Mock(TaskAction)
        def mockTrigger = Mock(TaskTrigger)
        def mockConditionA = Mock(TaskCondition)

        def mockConditionHandler = Mock(TaskConditionHandler) {
            handlesCondition(mockConditionA, _) >> false
            0 * _(*_)
        }

        when:
        service.taskTriggerFired(context, triggerData)

        then:
        0 * service.pluginService.listPlugins(TaskActionHandler)
        1 * service.pluginService.listPlugins(TaskConditionHandler) >> [test: new DescribedPlugin<TaskConditionHandler>(
            mockConditionHandler,
            null,
            'test',
            null
        )]
        1 * service.pluginService.configurePlugin('anaction', [action: 'data'], TaskAction, 'testProject') >>
        new ConfiguredPlugin<TaskAction>(
            mockAction,
            [config: 'data']
        )
        1 * service.pluginService.configurePlugin('atrigger', [trigger: 'data'], TaskTrigger, 'testProject') >>
        new ConfiguredPlugin<TaskTrigger>(
            mockTrigger,
            [config: 'data']
        )
        1 * service.pluginService.configurePlugin('condA', [a: 'b'], TaskCondition, 'testProject') >>
        new ConfiguredPlugin<TaskCondition>(
            mockConditionA,
            [config: 'data']
        )
        0 * service.pluginService._(*_)
//        TaskEvent.count() == 2
        TaskEvent.list().find { it.eventType == 'fired' } != null
        TaskEvent.list().find { it.eventType == 'result' } == null
        TaskEvent.list().find { it.eventType == 'error:condition:check' } != null
        TaskEvent.list().
            find {
                it.eventType == 'error:condition:check'
            }.eventDataMap.error ==~ /^Handler not found for condition\[0]:.+$/
    }

    @Unroll
    void "trigger fired failed condition"() {
        given:
        TaskRep rep = new TaskRep(
            uuid: UUID.randomUUID().toString(),
            serverNodeUuid: UUID.randomUUID().toString(),
            triggerType: 'atrigger',
            triggerConfig: [trigger: 'data'],
            project: 'testProject',
            actionType: 'anaction',
            actionConfig: [action: 'data'],
            conditionList: [[type: 'condA', config: [a: 'b']], [type: 'condB', config: [c: 'd']]],
            userCreated: 'bob',
            userModified: 'bob',
            authUser: 'bob',
            authRoleList: 'a,b,c'

        )
        if (!rep.validate()) {
            rep.errors.allErrors.each { println it }
        }
        rep = rep.save()
        def context = new RDTaskContext(
            taskId: rep.uuid,
            )
        def triggerData = [data: 'value']
        service.pluginService = Mock(PluginService)
        def mockAction = Mock(TaskAction)
        def mockTrigger = Mock(TaskTrigger)
        def mockConditionA = Mock(TaskCondition)
        def mockConditionB = Mock(TaskCondition)

        def mockConditionHandler = Mock(TaskConditionHandler) {
            handlesCondition(mockConditionA, _) >> true
            handlesCondition(mockConditionB, _) >> true
            1 * checkCondition(_, _, _, mockTrigger, mockConditionA) >> ConditionCheck.result(condAResult, [a: 'bb'])
            (condAResult ? 1 : 0) * checkCondition(_, _, _, mockTrigger, mockConditionB) >>
            ConditionCheck.result(condBResult, [c: 'dd'])
            0 * _(*_)
        }

        when:
        service.taskTriggerFired(context, triggerData)

        then:
        0 * service.pluginService.listPlugins(TaskActionHandler)
        (condAResult ? 2 : 1) * service.pluginService.listPlugins(TaskConditionHandler) >>
        [test: new DescribedPlugin<TaskConditionHandler>(
            mockConditionHandler,
            null,
            'test',
            null
        )]
        1 * service.pluginService.configurePlugin('anaction', [action: 'data'], TaskAction, 'testProject') >>
        new ConfiguredPlugin<TaskAction>(
            mockAction,
            [config: 'data']
        )
        1 * service.pluginService.configurePlugin('atrigger', [trigger: 'data'], TaskTrigger, 'testProject') >>
        new ConfiguredPlugin<TaskTrigger>(
            mockTrigger,
            [config: 'data']
        )
        1 * service.pluginService.configurePlugin('condA', [a: 'b'], TaskCondition, 'testProject') >>
        new ConfiguredPlugin<TaskCondition>(
            mockConditionA,
            [config: 'data']
        )
        1 * service.pluginService.configurePlugin('condB', [c: 'd'], TaskCondition, 'testProject') >>
        new ConfiguredPlugin<TaskCondition>(
            mockConditionB,
            [config: 'data']
        )
        0 * service.pluginService._(*_)
        TaskEvent.count() == 2
        TaskEvent.list().find { it.eventType == 'fired' } != null
        TaskEvent.list().find { it.eventType == 'result' } == null
        TaskEvent.list().find { it.eventType == 'condition:notmet' } != null
        TaskEvent.list().find { it.eventType == 'condition:notmet' }.eventDataMap != null
        TaskEvent.list().find { it.eventType == 'condition:notmet' }.eventDataMap.condition.index == (
            condAResult ? 1 :
            0
        )

        where:
        condAResult | condBResult
        false       | true
        true        | false
    }

    void "trigger fired with failure"() {
        given:
        TaskRep rep = new TaskRep(
            uuid: UUID.randomUUID().toString(),
            serverNodeUuid: UUID.randomUUID().toString(),
            triggerType: 'atrigger',
            triggerConfig: [trigger: 'data'],
            project: 'testProject',
            actionType: 'anaction',
            actionConfig: [action: 'data'],
            userCreated: 'bob',
            userModified: 'bob',
            authUser: 'bob',
            authRoleList: 'a,b,c'

        )
        if (!rep.validate()) {
            rep.errors.allErrors.each { println it }
        }
        rep = rep.save()
        def context = new RDTaskContext(
            taskId: rep.uuid,
            )
        def triggerData = [data: 'value']
        service.pluginService = Mock(PluginService)
        def mockAction = Mock(TaskAction)
        def mockTrigger = Mock(TaskTrigger)
        def mockActionHandler = Mock(TaskActionHandler) {
            handlesAction(mockAction, _) >> true
            1 * performTaskAction(context, triggerData, _, mockTrigger, mockAction, service) >> {
                throw new ActionFailed('test failure')
            }
            0 * _(*_)
        }

        when:
        service.taskTriggerFired(context, triggerData)

        then:
        1 * service.pluginService.listPlugins(TaskActionHandler) >> [test: new DescribedPlugin<TaskActionHandler>(
            mockActionHandler,
            null,
            'test',
            null
        )]
        1 * service.pluginService.configurePlugin('anaction', [action: 'data'], TaskAction, 'testProject') >>
        new ConfiguredPlugin<TaskAction>(
            mockAction,
            [config: 'data']
        )
        1 * service.pluginService.configurePlugin('atrigger', [trigger: 'data'], TaskTrigger, 'testProject') >>
        new ConfiguredPlugin<TaskTrigger>(
            mockTrigger,
            [config: 'data']
        )
        0 * service.pluginService._(*_)
        TaskEvent.count() == 2
        TaskEvent.list().find { it.eventType == 'fired' } != null
        TaskEvent.list().find { it.eventType == 'result' } == null
        TaskEvent.list().find { it.eventType == 'error:action:perform' } != null
        TaskEvent.list().find { it.eventType == 'error:action:perform' }.eventDataMap == [error: 'test failure']
    }
}
