package org.rundeck.app.data.workflow

import rundeck.services.feature.FeatureService
import com.dtolabs.rundeck.core.config.Features
import com.dtolabs.rundeck.core.execution.BaseExecutionItem
import com.dtolabs.rundeck.core.execution.PluginStepExecutionItemImpl
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import org.rundeck.app.data.model.v1.job.workflow.ConditionalSet
import rundeck.CommandExec
import rundeck.PluginStep
import spock.lang.Specification

/**
 * Tests for WorkflowDataWorkflowExecutionItemFactory conditional workflow logic
 */
class WorkflowDataWorkflowExecutionItemFactoryConditionalSpec extends Specification {

    FeatureService featureService
    def factory

    def setup() {
        featureService = Mock(FeatureService)
        factory = new WorkflowDataWorkflowExecutionItemFactory(featureService: featureService)
    }

    def "consolidateWorkflowSteps returns regular steps when feature flag disabled"() {
        given:
        featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> false

        def workflow = new WorkflowDataImpl()
        def step1 = new CommandExec(adhocRemoteString: 'echo step1')
        def step2 = new CommandExec(adhocRemoteString: 'echo step2')
        workflow.steps = [step1, step2]

        when:
        def result = factory.createExecutionItemForWorkflow(workflow)

        then:
        result != null
        result.workflow.commands.size() == 2
    }

    def "consolidateWorkflowSteps flattens conditional step when feature flag enabled"() {
        given:
        featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

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
        def result = factory.createExecutionItemForWorkflow(workflow)

        then:
        result != null
        result.workflow.commands.size() == 3 // step1 + 2 subSteps
        
        // Verify conditions are attached to sub-steps
        def subStep1 = result.workflow.commands[1]
        def subStep2 = result.workflow.commands[2]
        subStep1.conditions != null
        subStep2.conditions != null
        subStep1.conditions.conditionGroups.size() == 1
    }

    def "consolidateWorkflowSteps throws exception when feature flag disabled and workflow has conditional steps"() {
        given:
        featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> false

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
        factory.createExecutionItemForWorkflow(workflow)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "ConditionalStep should be flattened before calling itemForWFCmdItem"
    }

    def "consolidateWorkflowSteps attaches conditions to BaseExecutionItem"() {
        given:
        featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo sub1')
        ]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [conditionalStep]

        when:
        def result = factory.createExecutionItemForWorkflow(workflow)

        then:
        result != null
        result.workflow.commands.size() == 1
        def stepItem = result.workflow.commands[0]
        stepItem.conditions != null
        if (stepItem instanceof BaseExecutionItem) {
            assert ((BaseExecutionItem) stepItem).conditions != null
        }
    }

    def "consolidateWorkflowSteps attaches conditions to PluginStepExecutionItemImpl"() {
        given:
        featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def pluginStep = new PluginStep()
        pluginStep.type = 'exec'
        pluginStep.nodeStep = false
        pluginStep.configuration = [exec: 'echo test']

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [pluginStep]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [conditionalStep]

        when:
        def result = factory.createExecutionItemForWorkflow(workflow)

        then:
        result != null
        result.workflow.commands.size() == 1
        def stepItem = result.workflow.commands[0]
        stepItem.conditions != null
        if (stepItem instanceof PluginStepExecutionItemImpl) {
            assert ((PluginStepExecutionItemImpl) stepItem).conditions != null
        }
    }

    def "consolidateWorkflowSteps handles conditional step with error handler"() {
        given:
        featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

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
        def result = factory.createExecutionItemForWorkflow(workflow)

        then:
        result != null
        result.workflow.commands.size() == 1
        result.workflow.commands[0].conditions != null
        result.workflow.commands[0].failureHandler != null
    }

    def "consolidateWorkflowSteps preserves step order"() {
        given:
        featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

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
        def result = factory.createExecutionItemForWorkflow(workflow)

        then:
        result != null
        result.workflow.commands.size() == 4
        // Verify order: step1, sub1, sub2, step2
        result.workflow.commands[0].conditions == null
        result.workflow.commands[1].conditions != null
        result.workflow.commands[2].conditions != null
        result.workflow.commands[3].conditions == null
    }

    def "consolidateWorkflowSteps handles empty subSteps"() {
        given:
        featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = []

        def workflow = new WorkflowDataImpl()
        workflow.steps = [conditionalStep]

        when:
        def result = factory.createExecutionItemForWorkflow(workflow)

        then:
        result != null
        result.workflow.commands.size() == 0
    }

    def "consolidateWorkflowSteps handles null subSteps"() {
        given:
        featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = null

        def workflow = new WorkflowDataImpl()
        workflow.steps = [conditionalStep]

        when:
        def result = factory.createExecutionItemForWorkflow(workflow)

        then:
        result != null
        result.workflow.commands.size() == 0
    }

    def "itemForWFCmdItem throws exception for ConditionalStep"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [new CommandExec(adhocRemoteString: 'echo test')]

        when:
        WorkflowDataWorkflowExecutionItemFactory.itemForWFCmdItem(conditionalStep)

        then:
        thrown(IllegalArgumentException)
    }

    def "createExecutionItemForWorkflow throws exception for empty workflow"() {
        given:
        def workflow = new WorkflowDataImpl()
        workflow.steps = []

        when:
        factory.createExecutionItemForWorkflow(workflow)

        then:
        thrown(Exception)
    }

    def "consolidateWorkflowSteps converts ConditionalSet from data model"() {
        given:
        featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def dataModelCondSet = new ConditionalSetImpl()
        dataModelCondSet.conditionGroups = [[condDef]]
        dataModelCondSet.nodeStep = false

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = dataModelCondSet
        conditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo sub1')
        ]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [conditionalStep]

        when:
        def result = factory.createExecutionItemForWorkflow(workflow)

        then:
        result != null
        result.workflow.commands.size() == 1
        result.workflow.commands[0].conditions != null
        result.workflow.commands[0].conditions.conditionGroups.size() == 1
    }
}

