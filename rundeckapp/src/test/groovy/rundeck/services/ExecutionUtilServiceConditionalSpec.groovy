package rundeck.services

import com.dtolabs.rundeck.core.config.FeatureService
import com.dtolabs.rundeck.core.config.Features
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.HasParentStepContext
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

        def condDef = ConditionalDefinitionImpl.fromMap([key: '${option.env}', operator: '==', value: 'prod'])
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

        def condDef = ConditionalDefinitionImpl.fromMap([key: '${option.env}', operator: '==', value: 'prod'])
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

        def condDef = ConditionalDefinitionImpl.fromMap([key: '${option.env}', operator: '==', value: 'prod'])
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

        def condDef = ConditionalDefinitionImpl.fromMap([key: '${option.env}', operator: '==', value: 'prod'])
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

        def condDef = ConditionalDefinitionImpl.fromMap([key: '${option.env}', operator: '==', value: 'prod'])
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

        def condDef = ConditionalDefinitionImpl.fromMap([key: '${option.env}', operator: '==', value: 'prod'])
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

        def condDef = ConditionalDefinitionImpl.fromMap([key: '${option.env}', operator: '==', value: 'prod'])
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
        def condDef = ConditionalDefinitionImpl.fromMap([key: '${option.env}', operator: '==', value: 'prod'])
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

    def "conditional sub-steps are marked with parent and sub-step indices for hierarchical stepctx"() {
        given:
        service.featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def condDef = ConditionalDefinitionImpl.fromMap([key: '${option.env}', operator: '==', value: 'prod'])
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
        def step3 = new CommandExec(adhocRemoteString: 'echo step3')
        workflow.steps = [step1, conditionalStep, step3]

        when:
        def result = service.createExecutionItemForWorkflow(workflow, 'testProject')

        then:
        result.workflow.commands.size() == 4
        // step1 is a flat top-level step and must NOT carry hierarchical markers
        !isHierarchical(result.workflow.commands[0])
        // sub1 is the first sub-step of the conditional at logical step 2
        isHierarchical(result.workflow.commands[1])
        ((HasParentStepContext) result.workflow.commands[1]).parentStepNumber == 2
        ((HasParentStepContext) result.workflow.commands[1]).subStepNumber == 1
        // sub2 is the second sub-step of the same conditional
        isHierarchical(result.workflow.commands[2])
        ((HasParentStepContext) result.workflow.commands[2]).parentStepNumber == 2
        ((HasParentStepContext) result.workflow.commands[2]).subStepNumber == 2
        // step3 follows the conditional and reverts to flat top-level
        !isHierarchical(result.workflow.commands[3])
    }

    def "two consecutive conditional steps each get their own parent index"() {
        given:
        service.featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def condDef = ConditionalDefinitionImpl.fromMap([key: '${option.env}', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditional1 = new ConditionalStep()
        conditional1.conditionSet = condSet
        conditional1.subSteps = [new CommandExec(adhocRemoteString: 'echo c1-a')]

        def conditional2 = new ConditionalStep()
        conditional2.conditionSet = condSet
        conditional2.subSteps = [
            new CommandExec(adhocRemoteString: 'echo c2-a'),
            new CommandExec(adhocRemoteString: 'echo c2-b')
        ]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [conditional1, conditional2]

        when:
        def result = service.createExecutionItemForWorkflow(workflow, 'testProject')

        then:
        result.workflow.commands.size() == 3
        // conditional1's only sub-step lives at logical step 1 / sub-step 1
        ((HasParentStepContext) result.workflow.commands[0]).parentStepNumber == 1
        ((HasParentStepContext) result.workflow.commands[0]).subStepNumber == 1
        // conditional2's first sub-step lives at logical step 2 / sub-step 1
        ((HasParentStepContext) result.workflow.commands[1]).parentStepNumber == 2
        ((HasParentStepContext) result.workflow.commands[1]).subStepNumber == 1
        // conditional2's second sub-step lives at logical step 2 / sub-step 2
        ((HasParentStepContext) result.workflow.commands[2]).parentStepNumber == 2
        ((HasParentStepContext) result.workflow.commands[2]).subStepNumber == 2
    }

    def "regular step after a conditional carries the correct logicalStepNumber"() {
        given:
        service.featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def condDef = ConditionalDefinitionImpl.fromMap([key: '${option.env}', operator: '==', value: 'prod'])
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
        def step3 = new CommandExec(adhocRemoteString: 'echo step3')
        workflow.steps = [step1, conditionalStep, step3]

        when:
        def result = service.createExecutionItemForWorkflow(workflow, 'testProject')

        then:
        result.workflow.commands.size() == 4
        // step1 is logical step 1
        (result.workflow.commands[0] as HasParentStepContext).logicalStepNumber == 1
        // sub1 and sub2 are sub-steps of the conditional at logical step 2
        (result.workflow.commands[1] as HasParentStepContext).logicalStepNumber == 2
        (result.workflow.commands[2] as HasParentStepContext).logicalStepNumber == 2
        // step3 is logical step 3 (NOT flat engine step 4)
        (result.workflow.commands[3] as HasParentStepContext).logicalStepNumber == 3
    }

    private static boolean isHierarchical(StepExecutionItem item) {
        if (!(item instanceof HasParentStepContext)) return false
        HasParentStepContext h = (HasParentStepContext) item
        return h.parentStepNumber > 0 && h.subStepNumber > 0
    }

    def "consolidateWorkflowSteps preserves step order"() {
        given:
        service.featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def condDef = ConditionalDefinitionImpl.fromMap([key: '${option.env}', operator: '==', value: 'prod'])
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

    def "consolidateWorkflowSteps flattens 2-level nested conditionals"() {
        given:
        service.featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        // Outer condition: env == prod
        def outerCondDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def outerCondSet = new ConditionalSetImpl()
        outerCondSet.conditionGroups = [[outerCondDef]]

        // Inner condition: region == us-east
        def innerCondDef = ConditionalDefinitionImpl.fromMap([key: 'option.region', operator: '==', value: 'us-east'])
        def innerCondSet = new ConditionalSetImpl()
        innerCondSet.conditionGroups = [[innerCondDef]]

        // Nested conditional structure
        def innerConditional = new ConditionalStep()
        innerConditional.conditionSet = innerCondSet
        innerConditional.subSteps = [
            new CommandExec(adhocRemoteString: 'echo nested-step')
        ]

        def outerConditional = new ConditionalStep()
        outerConditional.conditionSet = outerCondSet
        outerConditional.subSteps = [innerConditional]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [outerConditional]

        when:
        def result = service.createExecutionItemForWorkflow(workflow, 'testProject')

        then:
        result != null
        result.workflow.commands.size() == 1 // flattened to single leaf step
        def cmd = result.workflow.commands[0]
        cmd.conditions != null
        cmd.conditions.conditionGroups.size() == 1 // one OR group
        cmd.conditions.conditionGroups[0].size() == 2 // both parent and child conditions (AND logic)
        cmd.conditions.conditionGroups[0]*.key == ['option.env', 'option.region']
        cmd.conditions.conditionGroups[0]*.value == ['prod', 'us-east']
    }

    def "consolidateWorkflowSteps handles mixed nested and non-nested steps"() {
        given:
        service.featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        // Outer conditional with nested conditional and regular step
        def outerCondDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def outerCondSet = new ConditionalSetImpl()
        outerCondSet.conditionGroups = [[outerCondDef]]

        def innerCondDef = ConditionalDefinitionImpl.fromMap([key: 'option.region', operator: '==', value: 'us-east'])
        def innerCondSet = new ConditionalSetImpl()
        innerCondSet.conditionGroups = [[innerCondDef]]

        def nestedConditional = new ConditionalStep()
        nestedConditional.conditionSet = innerCondSet
        nestedConditional.subSteps = [new CommandExec(adhocRemoteString: 'echo nested')]

        def outerConditional = new ConditionalStep()
        outerConditional.conditionSet = outerCondSet
        outerConditional.subSteps = [
            new CommandExec(adhocRemoteString: 'echo direct'),
            nestedConditional
        ]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [outerConditional]

        when:
        def result = service.createExecutionItemForWorkflow(workflow, 'testProject')

        then:
        result != null
        result.workflow.commands.size() == 2
        // First step: direct substep with only outer condition
        result.workflow.commands[0].conditions.conditionGroups[0].size() == 1
        result.workflow.commands[0].conditions.conditionGroups[0]*.key == ['option.env']
        // Second step: nested substep with combined conditions
        result.workflow.commands[1].conditions.conditionGroups[0].size() == 2
        result.workflow.commands[1].conditions.conditionGroups[0]*.key == ['option.env', 'option.region']
    }

    def "consolidateWorkflowSteps preserves execution order with nested conditionals"() {
        given:
        service.featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def innerCondDef = ConditionalDefinitionImpl.fromMap([key: 'option.region', operator: '==', value: 'us-east'])
        def innerCondSet = new ConditionalSetImpl()
        innerCondSet.conditionGroups = [[innerCondDef]]

        def nestedConditional = new ConditionalStep()
        nestedConditional.conditionSet = innerCondSet
        nestedConditional.subSteps = [
            new CommandExec(adhocRemoteString: 'echo nested1'),
            new CommandExec(adhocRemoteString: 'echo nested2')
        ]

        def outerConditional = new ConditionalStep()
        outerConditional.conditionSet = condSet
        outerConditional.subSteps = [
            new CommandExec(adhocRemoteString: 'echo before'),
            nestedConditional,
            new CommandExec(adhocRemoteString: 'echo after')
        ]

        def workflow = new WorkflowDataImpl()
        def step1 = new CommandExec(adhocRemoteString: 'echo start')
        def step2 = new CommandExec(adhocRemoteString: 'echo end')
        workflow.steps = [step1, outerConditional, step2]

        when:
        def result = service.createExecutionItemForWorkflow(workflow, 'testProject')

        then:
        result != null
        result.workflow.commands.size() == 6
        // Verify order: start, before, nested1, nested2, after, end
        result.workflow.commands[0].conditions == null // start
        result.workflow.commands[1].conditions != null // before (outer condition only)
        result.workflow.commands[2].conditions != null // nested1 (combined conditions)
        result.workflow.commands[3].conditions != null // nested2 (combined conditions)
        result.workflow.commands[4].conditions != null // after (outer condition only)
        result.workflow.commands[5].conditions == null // end
    }

    def "consolidateWorkflowSteps handles Cartesian product for OR groups"() {
        given:
        service.featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

        // Parent with 2 OR groups: (env==prod) OR (env==staging)
        def parentCond1 = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def parentCond2 = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'staging'])
        def parentCondSet = new ConditionalSetImpl()
        parentCondSet.conditionGroups = [[parentCond1], [parentCond2]]

        // Child with 2 OR groups: (region==us-east) OR (region==us-west)
        def childCond1 = ConditionalDefinitionImpl.fromMap([key: 'option.region', operator: '==', value: 'us-east'])
        def childCond2 = ConditionalDefinitionImpl.fromMap([key: 'option.region', operator: '==', value: 'us-west'])
        def childCondSet = new ConditionalSetImpl()
        childCondSet.conditionGroups = [[childCond1], [childCond2]]

        def nestedConditional = new ConditionalStep()
        nestedConditional.conditionSet = childCondSet
        nestedConditional.subSteps = [new CommandExec(adhocRemoteString: 'echo test')]

        def outerConditional = new ConditionalStep()
        outerConditional.conditionSet = parentCondSet
        outerConditional.subSteps = [nestedConditional]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [outerConditional]

        when:
        def result = service.createExecutionItemForWorkflow(workflow, 'testProject')

        then:
        result != null
        result.workflow.commands.size() == 1
        def cmd = result.workflow.commands[0]
        cmd.conditions != null
        // Cartesian product: 2 parent groups × 2 child groups = 4 combined groups
        cmd.conditions.conditionGroups.size() == 4
        // Each group should have 2 conditions (one from parent, one from child)
        cmd.conditions.conditionGroups.every { it.size() == 2 }
    }

    def "consolidateWorkflowSteps throws exception for nesting depth >= 2"() {
        given: "A workflow with 2-level nested conditionals"
        service.featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL) >> true

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
        service.createExecutionItemForWorkflow(workflow, 'testProject')

        then: "An IllegalArgumentException is thrown"
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("Conditional steps cannot be nested more than one level deep")
        ex.message.contains("depth 2")
    }
}

