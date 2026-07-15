package org.rundeck.app.components

import grails.testing.gorm.DataTest
import org.rundeck.app.components.jobs.ImportedJob
import org.rundeck.app.data.workflow.ConditionalDefinitionImpl
import org.rundeck.app.data.workflow.ConditionalSetImpl
import org.rundeck.app.data.workflow.ConditionalStep
import org.rundeck.app.data.workflow.WorkflowDataImpl
import rundeck.CommandExec
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Specification

/**
 * Tests for RundeckJobDefinitionManager conditional step export validation
 */
class RundeckJobDefinitionManagerConditionalSpec extends Specification implements DataTest {

    def setupSpec() {
        mockDomains ScheduledExecution, Workflow, CommandExec
    }

    def rundeckJobDefinitionManager

    def setup() {
        rundeckJobDefinitionManager = new RundeckJobDefinitionManager()
    }

    def "validateJobForExport returns valid for job without conditional steps"() {
        given:
        def workflow = new WorkflowDataImpl()
        workflow.steps = [
            new CommandExec(adhocRemoteString: 'echo test1'),
            new CommandExec(adhocRemoteString: 'echo test2')
        ]

        def job = new ScheduledExecution()
        job.setWorkflowData(workflow)

        when:
        def result = rundeckJobDefinitionManager.validateJobForExport(job, 'xml')

        then:
        result.isValid() == true
    }

    def "validateJobForExport returns invalid for job with conditional steps in XML format"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo test')
        ]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [conditionalStep]

        def job = new ScheduledExecution()
        job.setWorkflowData(workflow)

        when:
        def result = rundeckJobDefinitionManager.validateJobForExport(job, 'xml')

        then:
        result.isValid() == false
        result.errors != null
    }

    def "validateJobForExport returns valid for job with conditional steps in JSON format"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo test')
        ]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [conditionalStep]

        def job = new ScheduledExecution()
        job.setWorkflowData(workflow)

        when:
        def result = rundeckJobDefinitionManager.validateJobForExport(job, 'json')

        then:
        result.isValid() == true
    }

    def "validateJobForExport returns valid for job with conditional steps in YAML format"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo test')
        ]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [conditionalStep]

        def job = new ScheduledExecution()
        job.setWorkflowData(workflow)

        when:
        def result = rundeckJobDefinitionManager.validateJobForExport(job, 'yaml')

        then:
        result.isValid() == true
    }

    def "validateJobForExport handles mixed regular and conditional steps"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: 'option.env', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo test')
        ]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [
            new CommandExec(adhocRemoteString: 'echo step1'),
            conditionalStep,
            new CommandExec(adhocRemoteString: 'echo step2')
        ]

        def job = new ScheduledExecution()
        job.setWorkflowData(workflow)

        when:
        def xmlResult = rundeckJobDefinitionManager.validateJobForExport(job, 'xml')
        def jsonResult = rundeckJobDefinitionManager.validateJobForExport(job, 'json')

        then:
        xmlResult.isValid() == false
        jsonResult.isValid() == true
    }

    def "validateJobForExport handles job with null workflow"() {
        given:
        def job = new ScheduledExecution()
        job.setWorkflowData(null)

        when:
        def result = rundeckJobDefinitionManager.validateJobForExport(job, 'xml')

        then:
        result.isValid() == true
    }

    def "validateJobForExport handles job with empty workflow"() {
        given:
        def workflow = new WorkflowDataImpl()
        workflow.steps = []

        def job = new ScheduledExecution()
        job.setWorkflowData(workflow)

        when:
        def result = rundeckJobDefinitionManager.validateJobForExport(job, 'xml')

        then:
        result.isValid() == true
    }
}

