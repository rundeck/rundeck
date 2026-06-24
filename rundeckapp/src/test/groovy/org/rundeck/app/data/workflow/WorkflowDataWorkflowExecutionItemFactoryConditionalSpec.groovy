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

    def "consolidateWorkflowSteps flattens 2-level nested conditionals"() {
        given:
        featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def outerCondDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def outerCondSet = new ConditionalSetImpl()
        outerCondSet.conditionGroups = [[outerCondDef]]

        def innerCondDef = ConditionalDefinitionImpl.fromMap([key: 'option.region', operator: '==', value: 'us-east'])
        def innerCondSet = new ConditionalSetImpl()
        innerCondSet.conditionGroups = [[innerCondDef]]

        def innerConditionalStep = new ConditionalStep()
        innerConditionalStep.conditionSet = innerCondSet
        innerConditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo nested')
        ]

        def outerConditionalStep = new ConditionalStep()
        outerConditionalStep.conditionSet = outerCondSet
        outerConditionalStep.subSteps = [innerConditionalStep]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [outerConditionalStep]

        when:
        def result = factory.createExecutionItemForWorkflow(workflow)

        then:
        result != null
        result.workflow.commands.size() == 1
        result.workflow.commands[0].conditions != null
        result.workflow.commands[0].conditions.conditionGroups.size() == 1
        result.workflow.commands[0].conditions.conditionGroups[0].size() == 2 // both outer and inner conditions
    }

    def "consolidateWorkflowSteps combines nested conditions with AND logic"() {
        given:
        featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def outerCondDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def outerCondSet = new ConditionalSetImpl()
        outerCondSet.conditionGroups = [[outerCondDef]]

        def innerCondDef = ConditionalDefinitionImpl.fromMap([key: 'option.region', operator: '==', value: 'us-east'])
        def innerCondSet = new ConditionalSetImpl()
        innerCondSet.conditionGroups = [[innerCondDef]]

        def innerConditionalStep = new ConditionalStep()
        innerConditionalStep.conditionSet = innerCondSet
        innerConditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo nested')
        ]

        def outerConditionalStep = new ConditionalStep()
        outerConditionalStep.conditionSet = outerCondSet
        outerConditionalStep.subSteps = [innerConditionalStep]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [outerConditionalStep]

        when:
        def result = factory.createExecutionItemForWorkflow(workflow)

        then:
        result != null
        result.workflow.commands.size() == 1

        // Verify both conditions are present (AND logic)
        def stepItem = result.workflow.commands[0]
        stepItem.conditions != null
        stepItem.conditions.conditionGroups.size() == 1
        stepItem.conditions.conditionGroups[0].size() == 2
        stepItem.conditions.conditionGroups[0]*.key == ['option.env', 'option.region']
    }

    def "consolidateWorkflowSteps preserves step order with nested conditionals"() {
        given:
        featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def outerCondDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def outerCondSet = new ConditionalSetImpl()
        outerCondSet.conditionGroups = [[outerCondDef]]

        def innerCondDef = ConditionalDefinitionImpl.fromMap([key: 'option.region', operator: '==', value: 'us-east'])
        def innerCondSet = new ConditionalSetImpl()
        innerCondSet.conditionGroups = [[innerCondDef]]

        def innerConditionalStep = new ConditionalStep()
        innerConditionalStep.conditionSet = innerCondSet
        innerConditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo nested1'),
            new CommandExec(adhocRemoteString: 'echo nested2')
        ]

        def outerConditionalStep = new ConditionalStep()
        outerConditionalStep.conditionSet = outerCondSet
        outerConditionalStep.subSteps = [innerConditionalStep]

        def workflow = new WorkflowDataImpl()
        def step1 = new CommandExec(adhocRemoteString: 'echo before')
        def step2 = new CommandExec(adhocRemoteString: 'echo after')
        workflow.steps = [step1, outerConditionalStep, step2]

        when:
        def result = factory.createExecutionItemForWorkflow(workflow)

        then:
        result != null
        result.workflow.commands.size() == 4 // before, nested1, nested2, after

        // Verify order and conditions
        result.workflow.commands[0].conditions == null // before
        result.workflow.commands[1].conditions != null // nested1
        result.workflow.commands[2].conditions != null // nested2
        result.workflow.commands[3].conditions == null // after
    }

    def "consolidateWorkflowSteps handles mixed nesting (conditional and non-conditional subSteps)"() {
        given:
        featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def outerCondDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def outerCondSet = new ConditionalSetImpl()
        outerCondSet.conditionGroups = [[outerCondDef]]

        def innerCondDef = ConditionalDefinitionImpl.fromMap([key: 'option.region', operator: '==', value: 'us-east'])
        def innerCondSet = new ConditionalSetImpl()
        innerCondSet.conditionGroups = [[innerCondDef]]

        def innerConditionalStep = new ConditionalStep()
        innerConditionalStep.conditionSet = innerCondSet
        innerConditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo nested conditional')
        ]

        def outerConditionalStep = new ConditionalStep()
        outerConditionalStep.conditionSet = outerCondSet
        outerConditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo regular sub1'),
            innerConditionalStep,
            new CommandExec(adhocRemoteString: 'echo regular sub2')
        ]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [outerConditionalStep]

        when:
        def result = factory.createExecutionItemForWorkflow(workflow)

        then:
        result != null
        result.workflow.commands.size() == 3 // regular sub1, nested conditional, regular sub2

        // All steps should have outer condition
        result.workflow.commands[0].conditions != null
        result.workflow.commands[0].conditions.conditionGroups[0].size() == 1 // only outer condition

        // Middle step should have both conditions
        result.workflow.commands[1].conditions != null
        result.workflow.commands[1].conditions.conditionGroups[0].size() == 2 // outer + inner

        // Last step should have outer condition
        result.workflow.commands[2].conditions != null
        result.workflow.commands[2].conditions.conditionGroups[0].size() == 1 // only outer condition
    }

    def "consolidateWorkflowSteps throws exception for nesting depth >= 2"() {
        given: "A workflow with 2-level nested conditionals"
        featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        // Create level 3 conditional (innermost)
        def level3CondDef = ConditionalDefinitionImpl.fromMap([key: 'option.datacenter', operator: '==', value: 'dc1'])
        def level3CondSet = new ConditionalSetImpl()
        level3CondSet.conditionGroups = [[level3CondDef]]
        def level3Conditional = new ConditionalStep()
        level3Conditional.conditionSet = level3CondSet
        level3Conditional.subSteps = [new CommandExec(adhocRemoteString: 'echo deeply nested')]

        // Create level 2 conditional (middle)
        def level2CondDef = ConditionalDefinitionImpl.fromMap([key: 'option.region', operator: '==', value: 'us-east'])
        def level2CondSet = new ConditionalSetImpl()
        level2CondSet.conditionGroups = [[level2CondDef]]
        def level2Conditional = new ConditionalStep()
        level2Conditional.conditionSet = level2CondSet
        level2Conditional.subSteps = [level3Conditional]

        // Create top-level conditional
        def level1CondDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def level1CondSet = new ConditionalSetImpl()
        level1CondSet.conditionGroups = [[level1CondDef]]
        def level1Conditional = new ConditionalStep()
        level1Conditional.conditionSet = level1CondSet
        level1Conditional.subSteps = [level2Conditional]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [level1Conditional]

        when: "Attempting to create execution item"
        factory.createExecutionItemForWorkflow(workflow)

        then: "An IllegalArgumentException is thrown"
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("Conditional steps cannot be nested more than one level deep")
        ex.message.contains("depth 2")
    }
}

