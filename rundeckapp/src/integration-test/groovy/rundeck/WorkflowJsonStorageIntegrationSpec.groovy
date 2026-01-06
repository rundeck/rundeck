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

package rundeck

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

/**
 * Integration tests for workflow JSON storage with database persistence
 */
@Integration
@Rollback
class WorkflowJsonStorageIntegrationSpec extends Specification {

    def "test persist and retrieve ScheduledExecution with JSON workflow"() {
        given: "A ScheduledExecution with workflow"
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

        def job = new ScheduledExecution(
            jobName: "test-job-${System.currentTimeMillis()}",
            project: "test-project",
            groupPath: "test-group",
            description: "Test job",
            uuid: UUID.randomUUID().toString()
        )
        job.setWorkflowData(workflow)

        when: "Saving the job"
        job.save(flush: true)
        def savedId = job.id

        then: "Job should be saved successfully"
        savedId != null

        when: "Retrieving the job from database"
        def retrieved = ScheduledExecution.get(savedId)

        then: "Job should have workflow data"
        retrieved != null
        retrieved.workflowJson != null
        retrieved.workflow == null // Old format should be null

        and: "Workflow data should be retrievable"
        def retrievedWorkflow = retrieved.getWorkflowData()
        retrievedWorkflow != null
        retrievedWorkflow.keepgoing == true
        retrievedWorkflow.strategy == 'sequential'
        retrievedWorkflow.threadcount == 2
        retrievedWorkflow.commands.size() == 1
        retrievedWorkflow.commands[0].pluginType == 'test-plugin'
    }

    def "test persist and retrieve Execution with JSON workflow"() {
        given: "An Execution with workflow"
        def workflow = new Workflow(
            keepgoing: false,
            strategy: 'node-first',
            commands: []
        )
        workflow.addToCommands(new PluginStep(
            type: 'exec-plugin',
            nodeStep: true,
            configuration: [command: 'echo test']
        ))

        def execution = new Execution(
            project: "test-project",
            user: "testuser",
            dateStarted: new Date(),
            status: 'running',
            uuid: UUID.randomUUID().toString()
        )
        execution.setWorkflowData(workflow)

        when: "Saving the execution"
        execution.save(flush: true)
        def savedId = execution.id

        then: "Execution should be saved successfully"
        savedId != null

        when: "Retrieving the execution from database"
        def retrieved = Execution.get(savedId)

        then: "Execution should have workflow data"
        retrieved != null
        retrieved.workflowJson != null
        retrieved.workflow == null // Old format should be null

        and: "Workflow data should be retrievable"
        def retrievedWorkflow = retrieved.getWorkflowData()
        retrievedWorkflow != null
        retrievedWorkflow.keepgoing == false
        retrievedWorkflow.strategy == 'node-first'
        retrievedWorkflow.commands.size() == 1
    }

    def "test update existing job workflow using JSON storage"() {
        given: "An existing job"
        def workflow1 = new Workflow(
            keepgoing: true,
            strategy: 'sequential',
            commands: []
        )
        workflow1.addToCommands(new PluginStep(
            type: 'original-plugin',
            nodeStep: true,
            configuration: [key: 'original']
        ))

        def job = new ScheduledExecution(
            jobName: "test-job-${System.currentTimeMillis()}",
            project: "test-project",
            groupPath: "test-group",
            description: "Test job",
            uuid: UUID.randomUUID().toString()
        )
        job.setWorkflowData(workflow1)
        job.save(flush: true)
        def savedId = job.id

        when: "Updating the workflow"
        def retrieved = ScheduledExecution.get(savedId)
        def workflow2 = new Workflow(
            keepgoing: false,
            strategy: 'parallel',
            commands: []
        )
        workflow2.addToCommands(new PluginStep(
            type: 'updated-plugin',
            nodeStep: false,
            configuration: [key: 'updated']
        ))
        retrieved.setWorkflowData(workflow2)
        retrieved.save(flush: true)

        then: "Workflow should be updated"
        def updated = ScheduledExecution.get(savedId)
        def updatedWorkflow = updated.getWorkflowData()
        updatedWorkflow.keepgoing == false
        updatedWorkflow.strategy == 'parallel'
        updatedWorkflow.commands[0].pluginType == 'updated-plugin'
        updatedWorkflow.commands[0].configuration.key == 'updated'
    }

    def "test workflow with multiple steps persists correctly"() {
        given: "A workflow with multiple steps"
        def workflow = new Workflow(
            keepgoing: true,
            strategy: 'node-first',
            commands: []
        )
        workflow.addToCommands(new PluginStep(
            type: 'step-1',
            nodeStep: true,
            configuration: [step: '1']
        ))
        workflow.addToCommands(new PluginStep(
            type: 'step-2',
            nodeStep: true,
            configuration: [step: '2']
        ))
        workflow.addToCommands(new PluginStep(
            type: 'step-3',
            nodeStep: false,
            configuration: [step: '3']
        ))

        def job = new ScheduledExecution(
            jobName: "multi-step-job-${System.currentTimeMillis()}",
            project: "test-project",
            groupPath: "test-group",
            description: "Multi-step job",
            uuid: UUID.randomUUID().toString()
        )
        job.setWorkflowData(workflow)

        when: "Saving and retrieving"
        job.save(flush: true)
        def retrieved = ScheduledExecution.get(job.id)

        then: "All steps should be preserved"
        def retrievedWorkflow = retrieved.getWorkflowData()
        retrievedWorkflow.commands.size() == 3
        retrievedWorkflow.commands[0].pluginType == 'step-1'
        retrievedWorkflow.commands[1].pluginType == 'step-2'
        retrievedWorkflow.commands[2].pluginType == 'step-3'
        retrievedWorkflow.commands[2].nodeStep == false
    }

    def "test workflow with error handlers persists correctly"() {
        given: "A workflow with error handlers"
        def workflow = new Workflow(
            keepgoing: true,
            strategy: 'node-first',
            commands: []
        )
        def mainStep = new PluginStep(
            type: 'main-step',
            nodeStep: true,
            configuration: [action: 'run']
        )
        def errorHandler = new PluginStep(
            type: 'error-handler',
            nodeStep: true,
            configuration: [action: 'cleanup']
        )
        mainStep.errorHandler = errorHandler
        workflow.addToCommands(mainStep)

        def job = new ScheduledExecution(
            jobName: "error-handler-job-${System.currentTimeMillis()}",
            project: "test-project",
            groupPath: "test-group",
            description: "Job with error handler",
            uuid: UUID.randomUUID().toString()
        )
        job.setWorkflowData(workflow)

        when: "Saving and retrieving"
        job.save(flush: true)
        def retrieved = ScheduledExecution.get(job.id)

        then: "Error handler should be preserved"
        def retrievedWorkflow = retrieved.getWorkflowData()
        retrievedWorkflow.commands.size() == 1
        retrievedWorkflow.commands[0].errorHandler != null
        retrievedWorkflow.commands[0].errorHandler.pluginType == 'error-handler'
        retrievedWorkflow.commands[0].errorHandler.configuration.action == 'cleanup'
    }

    def "test backwards compatibility - reading old format workflows"() {
        given: "A job with old-style workflow (direct domain relationship)"
        def oldWorkflow = new Workflow(
            keepgoing: true,
            strategy: 'sequential',
            commands: []
        )
        oldWorkflow.addToCommands(new PluginStep(
            type: 'old-plugin',
            nodeStep: true,
            configuration: [mode: 'legacy']
        ))
        oldWorkflow.save(flush: true)

        def job = new ScheduledExecution(
            jobName: "legacy-job-${System.currentTimeMillis()}",
            project: "test-project",
            groupPath: "test-group",
            description: "Legacy job",
            uuid: UUID.randomUUID().toString(),
            workflow: oldWorkflow
        )
        job.save(flush: true)
        def savedId = job.id

        when: "Retrieving the job"
        def retrieved = ScheduledExecution.get(savedId)

        then: "Should read from old workflow field"
        def retrievedWorkflow = retrieved.getWorkflowData()
        retrievedWorkflow != null
        retrievedWorkflow.keepgoing == true
        retrievedWorkflow.strategy == 'sequential'
        retrievedWorkflow.commands[0].pluginType == 'old-plugin'
        retrievedWorkflow.commands[0].configuration.mode == 'legacy'
    }

    def "test migration from old to new format"() {
        given: "A job with old-style workflow"
        def oldWorkflow = new Workflow(
            keepgoing: false,
            strategy: 'node-first',
            commands: []
        )
        oldWorkflow.addToCommands(new PluginStep(
            type: 'migrate-plugin',
            nodeStep: true,
            configuration: [version: 'old']
        ))
        oldWorkflow.save(flush: true)

        def job = new ScheduledExecution(
            jobName: "migrate-job-${System.currentTimeMillis()}",
            project: "test-project",
            groupPath: "test-group",
            description: "Job to migrate",
            uuid: UUID.randomUUID().toString(),
            workflow: oldWorkflow
        )
        job.save(flush: true)
        def savedId = job.id

        when: "Reading and re-saving with new format"
        def retrieved = ScheduledExecution.get(savedId)
        def workflowData = retrieved.getWorkflowData()
        retrieved.setWorkflowData(workflowData)
        retrieved.save(flush: true)

        then: "Should now use JSON format"
        def migrated = ScheduledExecution.get(savedId)
        migrated.workflowJson != null
        migrated.workflow == null

        and: "Data should be preserved"
        def migratedWorkflow = migrated.getWorkflowData()
        migratedWorkflow.keepgoing == false
        migratedWorkflow.strategy == 'node-first'
        migratedWorkflow.commands[0].pluginType == 'migrate-plugin'
    }

    def "test large workflow with many steps persists correctly"() {
        given: "A large workflow"
        def workflow = new Workflow(
            keepgoing: true,
            strategy: 'parallel',
            threadcount: 4,
            commands: []
        )
        50.times { i ->
            workflow.addToCommands(new PluginStep(
                type: "plugin-${i}",
                nodeStep: i % 2 == 0,
                configuration: [
                    index: i,
                    data: "value-${i}",
                    nested: [key: "nested-${i}"]
                ]
            ))
        }

        def job = new ScheduledExecution(
            jobName: "large-workflow-job-${System.currentTimeMillis()}",
            project: "test-project",
            groupPath: "test-group",
            description: "Job with large workflow",
            uuid: UUID.randomUUID().toString()
        )
        job.setWorkflowData(workflow)

        when: "Saving and retrieving"
        job.save(flush: true)
        def retrieved = ScheduledExecution.get(job.id)

        then: "All steps should be preserved"
        def retrievedWorkflow = retrieved.getWorkflowData()
        retrievedWorkflow.commands.size() == 50
        retrievedWorkflow.threadcount == 4
        retrievedWorkflow.commands[0].configuration.index == 0
        retrievedWorkflow.commands[49].configuration.index == 49
        retrievedWorkflow.commands[25].configuration.nested.key == "nested-25"
    }

    def "test workflow with plugin configuration map persists correctly"() {
        given: "A workflow with plugin configuration"
        def workflow = new Workflow(
            keepgoing: true,
            strategy: 'custom-strategy',
            commands: []
        )
        workflow.pluginConfigMap = [
            WorkflowStrategy: [
                'custom-strategy': [
                    setting1: 'value1',
                    setting2: 'value2'
                ]
            ],
            LogFilter: [
                [type: 'filter1', config: [key: 'val1']],
                [type: 'filter2', config: [key: 'val2']]
            ]
        ]
        workflow.addToCommands(new PluginStep(
            type: 'test-plugin',
            nodeStep: true,
            configuration: [:]
        ))

        def job = new ScheduledExecution(
            jobName: "plugin-config-job-${System.currentTimeMillis()}",
            project: "test-project",
            groupPath: "test-group",
            description: "Job with plugin config",
            uuid: UUID.randomUUID().toString()
        )
        job.setWorkflowData(workflow)

        when: "Saving and retrieving"
        job.save(flush: true)
        def retrieved = ScheduledExecution.get(job.id)

        then: "Plugin configuration should be preserved"
        def retrievedWorkflow = retrieved.getWorkflowData()
        retrievedWorkflow.pluginConfigMap != null
        retrievedWorkflow.pluginConfigMap.WorkflowStrategy != null
        retrievedWorkflow.pluginConfigMap.WorkflowStrategy['custom-strategy'] != null
        retrievedWorkflow.pluginConfigMap.WorkflowStrategy['custom-strategy'].setting1 == 'value1'
        retrievedWorkflow.pluginConfigMap.LogFilter != null
        retrievedWorkflow.pluginConfigMap.LogFilter.size() == 2
    }

    def "test concurrent workflow updates do not conflict"() {
        given: "A job with workflow"
        def workflow = new Workflow(
            keepgoing: true,
            strategy: 'sequential',
            commands: []
        )
        workflow.addToCommands(new PluginStep(
            type: 'test-plugin',
            nodeStep: true,
            configuration: [version: '1']
        ))

        def job = new ScheduledExecution(
            jobName: "concurrent-job-${System.currentTimeMillis()}",
            project: "test-project",
            groupPath: "test-group",
            description: "Concurrent test job",
            uuid: UUID.randomUUID().toString()
        )
        job.setWorkflowData(workflow)
        job.save(flush: true)
        def savedId = job.id

        when: "Multiple updates to workflow"
        5.times { i ->
            def retrieved = ScheduledExecution.get(savedId)
            def updatedWorkflow = new Workflow(
                keepgoing: true,
                strategy: 'sequential',
                commands: []
            )
            updatedWorkflow.addToCommands(new PluginStep(
                type: 'test-plugin',
                nodeStep: true,
                configuration: [version: "${i + 2}"]
            ))
            retrieved.setWorkflowData(updatedWorkflow)
            retrieved.save(flush: true)
        }

        then: "Final version should be persisted"
        def finalVersion = ScheduledExecution.get(savedId)
        def finalWorkflow = finalVersion.getWorkflowData()
        finalWorkflow.commands[0].configuration.version == '6'
    }
}
