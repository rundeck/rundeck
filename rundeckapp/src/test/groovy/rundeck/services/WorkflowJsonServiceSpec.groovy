/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck.services

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import rundeck.*
import rundeck.services.ExecutionService
import spock.lang.Specification

/**
 * Service layer tests for workflow JSON storage functionality
 */
class WorkflowJsonServiceSpec extends Specification implements ServiceUnitTest<ExecutionService>, DataTest {

    void setupSpec() {
        mockDomains Execution, ScheduledExecution, Workflow, WorkflowStep, PluginStep, JobExec, CommandExec
    }

    def "test execution service clones workflow from job using JSON storage"() {
        given: "A job with workflow"
        def job = new ScheduledExecution(
            jobName: "test-job",
            project: "test-project",
            groupPath: "test-group",
            description: "Test job",
            uuid: UUID.randomUUID().toString()
        )
        def workflow = new Workflow(
            keepgoing: true,
            strategy: 'sequential',
            threadcount: 2,
            commands: []
        )
        workflow.addToCommands(new PluginStep(
            type: 'test-plugin',
            nodeStep: true,
            configuration: [key: 'value']
        ))
        job.setWorkflowData(workflow)

        when: "Getting workflow data from job"
        def workflowData = job.getWorkflowData()

        then: "Should retrieve workflow successfully"
        workflowData != null
        workflowData.keepgoing == true
        workflowData.strategy == 'sequential'
        workflowData.commands.size() == 1

        when: "Cloning for execution"
        def clonedWorkflow = new Workflow(workflowData as Workflow)

        then: "Cloned workflow should have same properties"
        clonedWorkflow.keepgoing == true
        clonedWorkflow.strategy == 'sequential'
        clonedWorkflow.commands.size() == 1
        clonedWorkflow.commands[0].pluginType == 'test-plugin'
    }

    def "test execution inherits workflow from job via JSON"() {
        given: "A job with JSON workflow"
        def job = new ScheduledExecution(
            jobName: "test-job",
            project: "test-project",
            groupPath: "test-group",
            uuid: UUID.randomUUID().toString()
        )
        def jobWorkflow = new Workflow(
            keepgoing: false,
            strategy: 'node-first',
            commands: []
        )
        jobWorkflow.addToCommands(new PluginStep(
            type: 'exec-plugin',
            nodeStep: true,
            configuration: [command: 'echo test']
        ))
        job.setWorkflowData(jobWorkflow)

        and: "An execution"
        def execution = new Execution(
            project: "test-project",
            user: "testuser",
            dateStarted: new Date(),
            status: 'running',
            uuid: UUID.randomUUID().toString()
        )

        when: "Setting execution workflow from job"
        def workflowData = job.getWorkflowData()
        def executionWorkflow = new Workflow(workflowData as Workflow)
        execution.setWorkflowData(executionWorkflow)

        then: "Execution should have workflow from job"
        execution.getWorkflowData() != null
        def execWf = execution.getWorkflowData()
        execWf.keepgoing == false
        execWf.strategy == 'node-first'
        execWf.commands.size() == 1
        execWf.commands[0].pluginType == 'exec-plugin'
    }

    def "test workflow update nulls old workflow and uses JSON"() {
        given: "A job with old-style workflow"
        def job = new ScheduledExecution(
            jobName: "test-job",
            project: "test-project",
            uuid: UUID.randomUUID().toString()
        )
        def oldWorkflow = new Workflow(
            keepgoing: true,
            strategy: 'sequential',
            commands: []
        )
        job.workflow = oldWorkflow

        when: "Updating to new JSON format"
        def newWorkflow = new Workflow(
            keepgoing: false,
            strategy: 'parallel',
            commands: []
        )
        newWorkflow.addToCommands(new PluginStep(
            type: 'new-plugin',
            nodeStep: true,
            configuration: [:]
        ))
        job.setWorkflowData(newWorkflow)

        then: "Old workflow should be nulled"
        job.workflow == null

        and: "New workflow should be in JSON"
        job.workflowJson != null
        job.workflowJson.contains('"strategy":"parallel"')

        and: "Should retrieve correctly"
        def retrieved = job.getWorkflowData()
        retrieved.strategy == 'parallel'
        retrieved.commands.size() == 1
    }

    def "test workflow with multiple steps serializes all steps"() {
        given: "A workflow with multiple steps"
        def workflow = new Workflow(
            keepgoing: true,
            strategy: 'node-first',
            commands: []
        )
        3.times { i ->
            workflow.addToCommands(new PluginStep(
                type: "plugin-${i}",
                nodeStep: true,
                configuration: [index: i]
            ))
        }

        def job = new ScheduledExecution(
            jobName: "multi-step-job",
            project: "test-project",
            uuid: UUID.randomUUID().toString()
        )

        when: "Setting workflow data"
        job.setWorkflowData(workflow)

        then: "All steps should be in JSON"
        job.workflowJson.contains('"type":"plugin-0"')
        job.workflowJson.contains('"type":"plugin-1"')
        job.workflowJson.contains('"type":"plugin-2"')

        and: "Should deserialize all steps"
        def retrieved = job.getWorkflowData()
        retrieved.commands.size() == 3
        retrieved.commands[0].pluginType == 'plugin-0'
        retrieved.commands[1].pluginType == 'plugin-1'
        retrieved.commands[2].pluginType == 'plugin-2'
    }

    def "test workflow with error handler persists correctly"() {
        given: "A workflow with error handler"
        def workflow = new Workflow(
            keepgoing: true,
            strategy: 'node-first',
            commands: []
        )
        def mainStep = new PluginStep(
            type: 'main-plugin',
            nodeStep: true,
            configuration: [action: 'run']
        )
        def errorHandler = new PluginStep(
            type: 'error-plugin',
            nodeStep: true,
            configuration: [action: 'cleanup']
        )
        mainStep.errorHandler = errorHandler
        workflow.addToCommands(mainStep)

        def job = new ScheduledExecution(
            jobName: "error-handler-job",
            project: "test-project",
            uuid: UUID.randomUUID().toString()
        )

        when: "Setting workflow data"
        job.setWorkflowData(workflow)

        then: "Error handler should be in JSON"
        job.workflowJson.contains('"errorhandler"')

        and: "Should deserialize with error handler"
        def retrieved = job.getWorkflowData()
        retrieved.commands[0].errorHandler != null
        retrieved.commands[0].errorHandler.pluginType == 'error-plugin'
    }

    def "test missed execution creates workflow with JSON"() {
        given: "Parameters for missed execution"
        def job = new ScheduledExecution(
            jobName: "scheduled-job",
            project: "test-project",
            user: "testuser",
            uuid: UUID.randomUUID().toString()
        )
        def misfireTime = new Date()
        def serverUUID = "server-123"

        when: "Creating missed execution"
        def missed = new Execution(
            scheduledExecution: job,
            dateStarted: misfireTime,
            dateCompleted: new Date(),
            project: job.project,
            user: job.user,
            serverNodeUUID: serverUUID,
            status: 'missed',
            uuid: UUID.randomUUID().toString()
        )
        missed.setWorkflowData(new Workflow())

        then: "Missed execution should have workflow in JSON"
        missed.workflowJson != null
        missed.workflow == null

        and: "Should be retrievable"
        missed.getWorkflowData() != null
    }

    def "test workflow strategy configuration persists"() {
        given: "A workflow with strategy configuration"
        def workflow = new Workflow(
            keepgoing: true,
            strategy: 'custom-strategy',
            commands: []
        )
        workflow.pluginConfigMap = [
            WorkflowStrategy: [
                'custom-strategy': [
                    maxRetries: 3,
                    timeout: 60
                ]
            ]
        ]
        workflow.addToCommands(new PluginStep(
            type: 'test-plugin',
            nodeStep: true,
            configuration: [:]
        ))

        def job = new ScheduledExecution(
            jobName: "strategy-job",
            project: "test-project",
            uuid: UUID.randomUUID().toString()
        )

        when: "Setting workflow data"
        job.setWorkflowData(workflow)

        then: "Strategy config should be in JSON"
        job.workflowJson.contains('"WorkflowStrategy"')
        job.workflowJson.contains('"custom-strategy"')

        and: "Should deserialize with strategy config"
        def retrieved = job.getWorkflowData()
        retrieved.pluginConfigMap != null
        retrieved.pluginConfigMap.WorkflowStrategy != null
        retrieved.pluginConfigMap.WorkflowStrategy['custom-strategy'].maxRetries == 3
    }

    def "test job workflow update preserves other job properties"() {
        given: "A job with various properties"
        def job = new ScheduledExecution(
            jobName: "complex-job",
            project: "test-project",
            groupPath: "group/subgroup",
            description: "Complex job description",
            uuid: UUID.randomUUID().toString(),
            scheduled: true,
            scheduleEnabled: true,
            executionEnabled: true,
            multipleExecutions: false
        )
        def workflow1 = new Workflow(keepgoing: true, strategy: 'sequential', commands: [])
        job.setWorkflowData(workflow1)

        when: "Updating only the workflow"
        def workflow2 = new Workflow(keepgoing: false, strategy: 'parallel', commands: [])
        workflow2.addToCommands(new PluginStep(
            type: 'new-plugin',
            nodeStep: true,
            configuration: [:]
        ))
        job.setWorkflowData(workflow2)

        then: "Other properties should remain unchanged"
        job.jobName == "complex-job"
        job.description == "Complex job description"
        job.scheduled == true
        job.scheduleEnabled == true

        and: "Workflow should be updated"
        job.getWorkflowData().strategy == 'parallel'
        job.getWorkflowData().commands.size() == 1
    }

    def "test execution workflow independent of job workflow"() {
        given: "A job with workflow"
        def job = new ScheduledExecution(
            jobName: "test-job",
            project: "test-project",
            uuid: UUID.randomUUID().toString()
        )
        def jobWorkflow = new Workflow(
            keepgoing: true,
            strategy: 'sequential',
            commands: []
        )
        jobWorkflow.addToCommands(new PluginStep(
            type: 'job-plugin',
            nodeStep: true,
            configuration: [source: 'job']
        ))
        job.setWorkflowData(jobWorkflow)

        and: "An execution cloned from job"
        def execution = new Execution(
            project: "test-project",
            user: "testuser",
            dateStarted: new Date(),
            uuid: UUID.randomUUID().toString()
        )
        def execWorkflow = new Workflow(job.getWorkflowData() as Workflow)
        execution.setWorkflowData(execWorkflow)

        when: "Modifying job workflow"
        def updatedJobWorkflow = new Workflow(
            keepgoing: false,
            strategy: 'parallel',
            commands: []
        )
        updatedJobWorkflow.addToCommands(new PluginStep(
            type: 'updated-plugin',
            nodeStep: true,
            configuration: [source: 'updated']
        ))
        job.setWorkflowData(updatedJobWorkflow)

        then: "Execution workflow should remain unchanged"
        def execWf = execution.getWorkflowData()
        execWf.strategy == 'sequential'
        execWf.commands[0].pluginType == 'job-plugin'
        execWf.commands[0].configuration.source == 'job'

        and: "Job workflow should be updated"
        def jobWf = job.getWorkflowData()
        jobWf.strategy == 'parallel'
        jobWf.commands[0].pluginType == 'updated-plugin'
    }
}
