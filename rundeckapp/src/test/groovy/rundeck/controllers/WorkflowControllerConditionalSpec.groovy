package rundeck.controllers

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.data.workflow.ConditionalDefinitionImpl
import org.rundeck.app.data.workflow.ConditionalSetImpl
import org.rundeck.app.data.workflow.ConditionalStep
import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData
import rundeck.CommandExec
import rundeck.PluginStep
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.data.constants.WorkflowStepConstants
import rundeck.services.FrameworkService
import spock.lang.Specification

/**
 * Tests for WorkflowController conditional step validation
 */
class WorkflowControllerConditionalSpec extends Specification implements ControllerUnitTest<WorkflowController>, DataTest {

    def setupSpec() {
        mockDomains ScheduledExecution, Workflow, CommandExec, PluginStep
    }

    def setup() {
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
    }

    def "_validateConditionalStep validates conditional step with valid subSteps"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        WorkflowStepData subStep = new CommandExec(adhocRemoteString: 'echo test')

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [subStep]
        conditionalStep.nodeStep = true

        controller.frameworkService.getStepPluginDescription(WorkflowStepConstants.TYPE_COMMAND) >> [:]
        controller.frameworkService.validateDescription(_, _, _, _, _, _) >> [valid: true]

        when:
        def result = WorkflowController._validateConditionalStep(controller.frameworkService, conditionalStep)

        then:
        result.valid == true
    }

    def "_validateConditionalStep fails when subStep nodeStep differs from conditional step"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def subStep = new CommandExec(adhocRemoteString: 'echo test')

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [subStep]
        conditionalStep.nodeStep = false

        when:
        def result = WorkflowController._validateConditionalStep(controller.frameworkService, conditionalStep)

        then:
        result.valid == false
        result.report.contains('must all be')
    }

    def "_validateConditionalStep fails when plugin not found for subStep"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: '${option.env}', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def pluginStep = new PluginStep()
        pluginStep.type = 'nonexistent-plugin'
        pluginStep.nodeStep = false

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [pluginStep]
        conditionalStep.nodeStep = false

        when:
        def result = WorkflowController._validateConditionalStep(controller.frameworkService, conditionalStep)

        then:
        result.valid == false
        result.report.contains('Plugin not found')
    }

    def "_validateConditionalStep fails when subStep plugin configuration invalid"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def pluginStep = new PluginStep()
        pluginStep.type = 'exec'
        pluginStep.nodeStep = false

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [pluginStep]
        conditionalStep.nodeStep = false

        controller.frameworkService.validateDescription(_, _, _, _, _, _) >> [valid: false, report: 'Invalid config']

        when:
        def result = WorkflowController._validateConditionalStep(controller.frameworkService, conditionalStep)

        then:
        result.valid == false
        result.report == "Substep Workflow Step Plugin not found: exec"
    }

    def "_validateConditionalStep returns valid for non-conditional step"() {
        given:
        def regularStep = new CommandExec(adhocRemoteString: 'echo test')

        when:
        def result = WorkflowController._validateConditionalStep(controller.frameworkService, regularStep)

        then:
        result.valid == true
    }

    def "_validateConditionalStep validates multiple subSteps"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def subStep1 = new CommandExec(adhocRemoteString: 'echo test1')
        def subStep2 = new CommandExec(adhocRemoteString: 'echo test2')

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [subStep1, subStep2]
        conditionalStep.nodeStep = true

        controller.frameworkService.getPluginStepDescription(false, 'exec') >> [:]
        controller.frameworkService.validateDescription(_, _, _, _, _, _) >> [valid: true]

        when:
        def result = WorkflowController._validateConditionalStep(controller.frameworkService, conditionalStep)

        then:
        result.valid == true
    }

    def "_validateConditionalStep fails when any subStep is invalid"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def subStep1 = new CommandExec(adhocRemoteString: 'echo test1')
        def subStep2 = new PluginStep()
        subStep2.type = 'invalid-plugin'
        subStep2.nodeStep = false

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [subStep1, subStep2]
        conditionalStep.nodeStep = false

        controller.frameworkService.getPluginStepDescription(false, 'exec') >> [:]
        controller.frameworkService.getPluginStepDescription(false, 'invalid-plugin') >> null

        when:
        def result = WorkflowController._validateConditionalStep(controller.frameworkService, conditionalStep)

        then:
        result.valid == false
        result.report.contains('Plugin not found')
    }
}

