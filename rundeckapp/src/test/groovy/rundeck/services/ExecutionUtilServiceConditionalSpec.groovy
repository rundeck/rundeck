package rundeck.services

import com.dtolabs.rundeck.core.config.FeatureService
import com.dtolabs.rundeck.core.config.Features
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import grails.testing.web.GrailsWebUnitTest
import org.rundeck.app.data.model.v1.job.workflow.ConditionalSet
import org.rundeck.app.data.workflow.ConditionalDefinitionImpl
import org.rundeck.app.data.workflow.ConditionalSetImpl
import org.rundeck.app.data.workflow.ConditionalStep
import org.rundeck.app.data.workflow.WorkflowDataImpl
import rundeck.CommandExec
import rundeck.Execution
import rundeck.PluginStep
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Specification

/**
 * Tests for ExecutionUtilService conditional workflow logic
 */
class ExecutionUtilServiceConditionalSpec extends Specification implements ServiceUnitTest<ExecutionUtilService>, DataTest, GrailsWebUnitTest {

    def setupSpec() {
        mockDomains Execution, CommandExec, PluginStep, ScheduledExecution, Workflow
    }

    def setup() {
        service.featureService = Mock(FeatureService)
        service.scheduledExecutionService = Mock(ScheduledExecutionService)
    }

    def "consolidateWorkflowSteps returns regular steps when feature flag disabled"() {
        given:
        service.featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> false

        def workflow = new WorkflowDataImpl()
        def step1 = new CommandExec(adhocRemoteString: 'echo step1')
        def step2 = new CommandExec(adhocRemoteString: 'echo step2')
        workflow.steps = [step1, step2]

        when:
        def result = service.createExecutionItemForWorkflow(workflow, 'testProject')

        then:
        result != null
        result.workflow.commands.size() == 2
    }

    def "consolidateWorkflowSteps flattens conditional step when feature flag enabled"() {
        given:
        service.featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]
        condSet.nodeStep = false

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo sub1'),
            new CommandExec(adhocRemoteString: 'echo sub2')
        ]

        def workflow = new WorkflowDataImpl()
        def step1 = new CommandExec(adhocRemoteString: 'echo step1')
        workflow.steps = [step1, conditionalStep]

        when:
        def result = service.createExecutionItemForWorkflow(workflow, 'testProject')

        then:
        result != null
        result.workflow.commands.size() == 3 // step1 + 2 subSteps
        // Verify conditions are attached to sub-steps
        result.workflow.commands[1].conditions != null
        result.workflow.commands[2].conditions != null
    }

    def "consolidateWorkflowSteps ignore conditional steps when feature flag disabled and workflow has conditional steps"() {
        given:
        service.featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> false

        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo sub1')
        ]

        def workflow = new WorkflowDataImpl()
        def step1 = new CommandExec(adhocRemoteString: 'echo step1')
        workflow.steps = [step1, conditionalStep]

        when:
        WorkflowExecutionItem witem = service.createExecutionItemForWorkflow(workflow, 'testProject')

        then:
        !witem.getWorkflow().commands.any {it.conditions}
    }

    def "consolidateWorkflowSteps handles mixed regular and conditional steps"() {
        given:
        service.featureService = Mock(FeatureService){
            featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true
        }

        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo sub1')
        ]

        def workflow = new WorkflowDataImpl()
        def step1 = new CommandExec(adhocRemoteString: 'echo step1')
        def step2 = new CommandExec(adhocRemoteString: 'echo step2')
        workflow.steps = [step1, conditionalStep, step2]

        when:
        def result = service.createExecutionItemForWorkflow(workflow, 'testProject')

        then:
        result != null
        result.workflow.commands.size() == 3 // step1 + 1 subStep + step2
        result.workflow.commands[0].conditions == null
        result.workflow.commands[1].conditions != null
        result.workflow.commands[2].conditions == null
    }

    def "consolidateWorkflowSteps handles conditional step with error handler"() {
        given:
        service.featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def errorHandler = new CommandExec(adhocRemoteString: 'echo error')
        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo sub1', errorHandler: errorHandler)
        ]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [conditionalStep]

        when:
        def result = service.createExecutionItemForWorkflow(workflow, 'testProject')

        then:
        result != null
        result.workflow.commands.size() == 1
        result.workflow.commands[0].conditions != null
        result.workflow.commands[0].failureHandler != null
    }

    def "consolidateWorkflowSteps handles conditional step with multiple subSteps"() {
        given:
        service.featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo sub1'),
            new CommandExec(adhocRemoteString: 'echo sub2'),
            new CommandExec(adhocRemoteString: 'echo sub3')
        ]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [conditionalStep]

        when:
        def result = service.createExecutionItemForWorkflow(workflow, 'testProject')

        then:
        result != null
        result.workflow.commands.size() == 3
        result.workflow.commands.every { it.conditions != null }
        result.workflow.commands.every { it.conditions == condSet }
    }

    def "consolidateWorkflowSteps handles conditional step without subSteps"() {
        given:
        service.featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = []

        def workflow = new WorkflowDataImpl()
        workflow.steps = [conditionalStep]

        when:
        def result = service.createExecutionItemForWorkflow(workflow, 'testProject')

        then:
        result != null
        result.workflow.commands.size() == 0
    }

    def "consolidateWorkflowSteps handles conditional step with null subSteps"() {
        given:
        service.featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = null

        def workflow = new WorkflowDataImpl()
        workflow.steps = [conditionalStep]

        when:
        def result = service.createExecutionItemForWorkflow(workflow, 'testProject')

        then:
        result != null
        result.workflow.commands.size() == 0
    }

    def "itemForWFCmdItem handles ConditionalStep type"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [new CommandExec(adhocRemoteString: 'echo test')]

        when:
        def result = service.itemForWFCmdItem(conditionalStep)

        then:
        // Should handle gracefully or throw appropriate exception
        // Based on implementation, it may log a warning or handle it
        noExceptionThrown()
    }

    def "createExecutionItemForWorkflow throws exception for empty workflow"() {
        given:
        def workflow = new WorkflowDataImpl()
        workflow.steps = []

        when:
        service.createExecutionItemForWorkflow(workflow, 'testProject')

        then:
        thrown(Exception)
    }

    def "consolidateWorkflowSteps preserves step order"() {
        given:
        service.featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo sub1'),
            new CommandExec(adhocRemoteString: 'echo sub2')
        ]

        def workflow = new WorkflowDataImpl()
        def step1 = new CommandExec(adhocRemoteString: 'echo step1')
        def step2 = new CommandExec(adhocRemoteString: 'echo step2')
        workflow.steps = [step1, conditionalStep, step2]

        when:
        def result = service.createExecutionItemForWorkflow(workflow, 'testProject')

        then:
        result != null
        result.workflow.commands.size() == 4
        // Verify order: step1, sub1, sub2, step2
        result.workflow.commands[0].conditions == null
        result.workflow.commands[1].conditions != null
        result.workflow.commands[2].conditions != null
        result.workflow.commands[3].conditions == null
    }
}

