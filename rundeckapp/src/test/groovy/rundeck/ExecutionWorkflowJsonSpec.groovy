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

import grails.testing.gorm.DataTest
import org.rundeck.app.data.model.v1.job.workflow.WorkflowData
import spock.lang.Specification

/**
 * Unit tests for Execution workflow JSON storage
 */
class ExecutionWorkflowJsonSpec extends Specification implements DataTest {

    void setupSpec() {
        mockDomains Execution, ScheduledExecution, Workflow, WorkflowStep, PluginStep, JobExec, CommandExec
    }

    def "test getWorkflowData returns workflow when workflow field is set"() {
        given: "An Execution with an old-style workflow"
        def execution = new Execution(
            project: "test-project",
            user: "testuser",
            dateStarted: new Date()
        )
        def workflow = new Workflow(
            keepgoing: true,
            strategy: 'node-first',
            commands: []
        )
        execution.workflow = workflow

        when: "Getting workflow data"
        def result = execution.getWorkflowData()

        then: "Should return the existing workflow object"
        result != null
        result == workflow
        result.keepgoing == true
        result.strategy == 'node-first'
    }

    def "test getWorkflowData deserializes workflowJson when workflow is null"() {
        given: "An Execution with JSON workflow"
        def execution = new Execution(
            project: "test-project",
            user: "testuser",
            dateStarted: new Date()
        )
        execution.workflowJson = '{"keepgoing":false,"strategy":"sequential","commands":[]}'

        when: "Getting workflow data"
        def result = execution.getWorkflowData()

        then: "Should deserialize from JSON"
        result != null
        result instanceof Workflow
        result.keepgoing == false
        result.strategy == 'sequential'
        result.commands != null
    }

    def "test getWorkflowData returns null when both fields are null"() {
        given: "An Execution with no workflow"
        def execution = new Execution(
            project: "test-project",
            user: "testuser",
            dateStarted: new Date()
        )

        when: "Getting workflow data"
        def result = execution.getWorkflowData()

        then: "Should return null"
        result == null
    }

    def "test setWorkflowData nulls old workflow and sets workflowJson"() {
        given: "An Execution with old-style workflow"
        def execution = new Execution(
            project: "test-project",
            user: "testuser",
            dateStarted: new Date()
        )
        def oldWorkflow = new Workflow(keepgoing: false, strategy: 'node-first', commands: [])
        execution.workflow = oldWorkflow

        and: "A new workflow to set"
        def newWorkflow = new Workflow(
            keepgoing: true,
            strategy: 'parallel',
            commands: []
        )

        when: "Setting workflow data"
        execution.setWorkflowData(newWorkflow)

        then: "Old workflow field should be null"
        execution.workflow == null

        and: "workflowJson should be set"
        execution.workflowJson != null
        execution.workflowJson.contains('"keepgoing":true')
        execution.workflowJson.contains('"strategy":"parallel"')
    }

    def "test setWorkflowData with null clears both fields"() {
        given: "An Execution with workflow data"
        def execution = new Execution(
            project: "test-project",
            user: "testuser",
            dateStarted: new Date()
        )
        execution.workflow = new Workflow(keepgoing: true, strategy: 'node-first', commands: [])

        when: "Setting null workflow data"
        execution.setWorkflowData(null)

        then: "Both fields should be null"
        execution.workflow == null
        execution.workflowJson == null
    }

    def "test execution clones workflow from job correctly"() {
        given: "A job with workflow"
        def job = new ScheduledExecution(
            jobName: "test-job",
            project: "test-project"
        )
        def jobWorkflow = new Workflow(
            keepgoing: true,
            strategy: 'sequential',
            threadcount: 2,
            commands: []
        )
        jobWorkflow.addToCommands(new PluginStep(
            type: 'test-plugin',
            nodeStep: true,
            configuration: [key: 'value']
        ))
        job.setWorkflowData(jobWorkflow)

        and: "An execution"
        def execution = new Execution(
            project: "test-project",
            user: "testuser",
            dateStarted: new Date()
        )

        when: "Cloning workflow from job"
        def clonedWorkflow = new Workflow(job.getWorkflowData() as Workflow)
        execution.setWorkflowData(clonedWorkflow)

        then: "Execution should have workflow data"
        execution.getWorkflowData() != null
        def execWorkflow = execution.getWorkflowData() as Workflow
        execWorkflow.keepgoing == true
        execWorkflow.strategy == 'sequential'
        execWorkflow.threadcount == 2
        execWorkflow.commands.size() == 1
    }

    def "test backwards compatibility - prefers old workflow over JSON"() {
        given: "An Execution with both old and new format"
        def execution = new Execution(
            project: "test-project",
            user: "testuser",
            dateStarted: new Date()
        )
        // Old format
        execution.workflow = new Workflow(keepgoing: false, strategy: 'node-first', commands: [])
        // New format (different values)
        execution.workflowJson = '{"keepgoing":true,"strategy":"sequential","commands":[]}'

        when: "Getting workflow data"
        def result = execution.getWorkflowData()

        then: "Should prefer old format for backwards compatibility"
        result == execution.workflow
        result.keepgoing == false
        result.strategy == 'node-first'
    }

    def "test roundtrip serialization preserves execution workflow"() {
        given: "A workflow for execution"
        def originalWorkflow = new Workflow(
            keepgoing: false,
            strategy: 'node-first',
            threadcount: 1,
            commands: []
        )
        def step = new PluginStep(
            type: 'exec-command',
            nodeStep: true,
            configuration: [command: 'echo hello']
        )
        originalWorkflow.addToCommands(step)

        and: "An Execution"
        def execution = new Execution(
            project: "test-project",
            user: "testuser",
            dateStarted: new Date()
        )

        when: "Setting and getting workflow data"
        execution.setWorkflowData(originalWorkflow)
        def deserializedWorkflow = execution.getWorkflowData() as Workflow

        then: "Workflow properties should be preserved"
        deserializedWorkflow != null
        deserializedWorkflow.keepgoing == originalWorkflow.keepgoing
        deserializedWorkflow.strategy == originalWorkflow.strategy
        deserializedWorkflow.commands.size() == 1
        deserializedWorkflow.commands[0].pluginType == 'exec-command'
    }

    def "test workflow with error handler serializes correctly"() {
        given: "A workflow with error handler"
        def workflow = new Workflow(
            keepgoing: true,
            strategy: 'node-first',
            commands: []
        )
        def mainStep = new PluginStep(
            type: 'main-plugin',
            nodeStep: true,
            configuration: [key: 'value']
        )
        def errorHandler = new PluginStep(
            type: 'error-handler-plugin',
            nodeStep: true,
            configuration: [action: 'cleanup']
        )
        mainStep.errorHandler = errorHandler
        workflow.addToCommands(mainStep)

        and: "An Execution"
        def execution = new Execution(
            project: "test-project",
            user: "testuser",
            dateStarted: new Date()
        )

        when: "Setting workflow data"
        execution.setWorkflowData(workflow)

        and: "Getting it back"
        def result = execution.getWorkflowData() as Workflow

        then: "Error handler should be preserved"
        result.commands.size() == 1
        result.commands[0].errorHandler != null
        result.commands[0].errorHandler.pluginType == 'error-handler-plugin'
    }

    def "test workflowJson field is mapped as text type for large workflows"() {
        given: "An Execution"
        def execution = new Execution(
            project: "test-project",
            user: "testuser",
            dateStarted: new Date()
        )
        def largeWorkflow = new Workflow(keepgoing: true, strategy: 'node-first', commands: [])
        // Add many steps to create large JSON
        50.times { i ->
            largeWorkflow.addToCommands(new PluginStep(
                type: "step-${i}",
                nodeStep: true,
                configuration: [data: "large-value-${i}" * 10]
            ))
        }

        when: "Setting workflow data"
        execution.setWorkflowData(largeWorkflow)

        then: "Should handle large text without issues"
        execution.workflowJson != null
        execution.workflowJson.length() > 1000
    }

    def "test getWorkflowJsonMap helper method"() {
        given: "An Execution with workflowJson"
        def execution = new Execution(
            project: "test-project",
            user: "testuser",
            dateStarted: new Date()
        )
        execution.workflowJson = '{"keepgoing":false,"strategy":"sequential","commands":[]}'

        when: "Getting workflow as map"
        def result = execution.getWorkflowJsonMap()

        then: "Should return map representation"
        result != null
        result instanceof Map
        result.keepgoing == false
        result.strategy == 'sequential'
        result.commands instanceof List
    }

    def "test getWorkflowJsonMap returns null when no JSON"() {
        given: "An Execution without workflowJson"
        def execution = new Execution(
            project: "test-project",
            user: "testuser",
            dateStarted: new Date()
        )

        when: "Getting workflow as map"
        def result = execution.getWorkflowJsonMap()

        then: "Should return null"
        result == null
    }

    def "test execution status states with workflow JSON"() {
        given: "An execution with workflow"
        def execution = new Execution(
            project: "test-project",
            user: "testuser",
            dateStarted: new Date(),
            status: 'running'
        )
        def workflow = new Workflow(keepgoing: true, strategy: 'node-first', commands: [])
        execution.setWorkflowData(workflow)

        when: "Changing execution status"
        execution.status = 'succeeded'
        execution.dateCompleted = new Date()

        then: "Workflow JSON should remain intact"
        execution.getWorkflowData() != null
        execution.workflowJson != null
    }

    def "test serialization handles plugin configuration in steps"() {
        given: "A workflow with complex plugin configuration"
        def workflow = new Workflow(keepgoing: true, strategy: 'node-first', commands: [])
        def step = new PluginStep(
            type: 'complex-plugin',
            nodeStep: true,
            configuration: [
                nested: [
                    key1: 'value1',
                    key2: 'value2'
                ],
                list: ['item1', 'item2', 'item3'],
                number: 42,
                boolean: true
            ]
        )
        workflow.addToCommands(step)

        and: "An Execution"
        def execution = new Execution(
            project: "test-project",
            user: "testuser",
            dateStarted: new Date()
        )

        when: "Setting and getting workflow"
        execution.setWorkflowData(workflow)
        def result = execution.getWorkflowData() as Workflow

        then: "Complex configuration should be preserved"
        result.commands[0].configuration.nested instanceof Map
        result.commands[0].configuration.nested.key1 == 'value1'
        result.commands[0].configuration.list instanceof List
        result.commands[0].configuration.list.size() == 3
        result.commands[0].configuration.number == 42
        result.commands[0].configuration.boolean == true
    }
}
