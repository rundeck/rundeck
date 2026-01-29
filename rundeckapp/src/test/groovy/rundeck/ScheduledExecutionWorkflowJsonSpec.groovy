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
 * Unit tests for ScheduledExecution workflow JSON storage
 */
class ScheduledExecutionWorkflowJsonSpec extends Specification implements DataTest {

    void setupSpec() {
        mockDomains ScheduledExecution, Workflow, WorkflowStep, PluginStep, JobExec, CommandExec
    }

    def "test getWorkflowData returns workflow when workflow field is set"() {
        given: "A ScheduledExecution with an old-style workflow"
        def se = new ScheduledExecution(
            jobName: "test-job",
            project: "test-project"
        )
        def workflow = new Workflow(
            keepgoing: true,
            strategy: 'node-first',
            commands: []
        )
        se.workflow = workflow

        when: "Getting workflow data"
        def result = se.getWorkflowData()

        then: "Should return the existing workflow object"
        result != null
        result == workflow
        result.keepgoing == true
        result.strategy == 'node-first'
    }

    def "test getWorkflowData deserializes workflowJson when workflow is null"() {
        given: "A ScheduledExecution with JSON workflow"
        def se = new ScheduledExecution(
            jobName: "test-job",
            project: "test-project"
        )
        se.workflowJson = '{"keepgoing":true,"strategy":"sequential","commands":[]}'

        when: "Getting workflow data"
        def result = se.getWorkflowData()

        then: "Should deserialize from JSON"
        result != null
        result instanceof Workflow
        result.keepgoing == true
        result.strategy == 'sequential'
        result.commands != null
    }

    def "test getWorkflowData returns null when both fields are null"() {
        given: "A ScheduledExecution with no workflow"
        def se = new ScheduledExecution(
            jobName: "test-job",
            project: "test-project"
        )

        when: "Getting workflow data"
        def result = se.getWorkflowData()

        then: "Should return null"
        result == null
    }

    def "test setWorkflowData nulls old workflow and sets workflowJson"() {
        given: "A ScheduledExecution with old-style workflow"
        def se = new ScheduledExecution(
            jobName: "test-job",
            project: "test-project"
        )
        def oldWorkflow = new Workflow(keepgoing: false, strategy: 'node-first', commands: [])
        se.workflow = oldWorkflow

        and: "A new workflow to set"
        def newWorkflow = new Workflow(
            keepgoing: true,
            strategy: 'sequential',
            commands: []
        )

        when: "Setting workflow data"
        se.setWorkflowData(newWorkflow)

        then: "Old workflow field should be null"
        se.workflow == null

        and: "workflowJson should be set"
        se.workflowJson != null
        se.workflowJson.contains('"keepgoing":true')
        se.workflowJson.contains('"strategy":"sequential"')
    }

    def "test setWorkflowData with null clears both fields"() {
        given: "A ScheduledExecution with workflow data"
        def se = new ScheduledExecution(
            jobName: "test-job",
            project: "test-project"
        )
        se.workflow = new Workflow(keepgoing: true, strategy: 'node-first', commands: [])

        when: "Setting null workflow data"
        se.setWorkflowData(null)

        then: "Both fields should be null"
        se.workflow == null
        se.workflowJson == null
    }

    def "test setWorkflowData serializes workflow with steps"() {
        given: "A workflow with steps"
        def workflow = new Workflow(
            keepgoing: true,
            strategy: 'node-first',
            commands: []
        )
        def step = new PluginStep(
            type: 'test-plugin',
            nodeStep: true,
            configuration: [key: 'value']
        )
        workflow.addToCommands(step)

        and: "A ScheduledExecution"
        def se = new ScheduledExecution(
            jobName: "test-job",
            project: "test-project"
        )

        when: "Setting workflow data"
        se.setWorkflowData(workflow)

        then: "workflowJson should contain step information"
        se.workflowJson != null
        se.workflowJson.contains('"commands"')
        se.workflowJson.contains('"type":"test-plugin"')
    }

    def "test getWorkflowData deserializes workflow with steps"() {
        given: "JSON with workflow steps"
        def se = new ScheduledExecution(
            jobName: "test-job",
            project: "test-project"
        )
        se.workflowJson = '''
        {
            "keepgoing":true,
            "strategy":"node-first",
            "commands":[
                {
                    "type":"test-plugin",
                    "nodeStep":true,
                    "configuration":{"key":"value"}
                }
            ]
        }
        '''

        when: "Getting workflow data"
        def result = se.getWorkflowData()

        then: "Should deserialize with steps"
        result != null
        result.commands != null
        result.commands.size() == 1
        result.commands[0].pluginType == 'test-plugin'
    }

    def "test backwards compatibility - prefers old workflow over JSON"() {
        given: "A ScheduledExecution with both old and new format"
        def se = new ScheduledExecution(
            jobName: "test-job",
            project: "test-project"
        )
        // Old format
        se.workflow = new Workflow(keepgoing: false, strategy: 'node-first', commands: [])
        // New format (different values)
        se.workflowJson = '{"keepgoing":true,"strategy":"sequential","commands":[]}'

        when: "Getting workflow data"
        def result = se.getWorkflowData()

        then: "Should prefer old format for backwards compatibility"
        result == se.workflow
        result.keepgoing == false
        result.strategy == 'node-first'
    }

    def "test roundtrip serialization preserves workflow data"() {
        given: "A complex workflow"
        def originalWorkflow = new Workflow(
            keepgoing: true,
            strategy: 'parallel',
            threadcount: 3,
            commands: []
        )
        def step1 = new PluginStep(
            type: 'test-plugin',
            nodeStep: true,
            configuration: [param1: 'value1', param2: 'value2']
        )
        def step2 = new PluginStep(
            type: 'another-plugin',
            nodeStep: false,
            configuration: [setting: 'data']
        )
        originalWorkflow.addToCommands(step1)
        originalWorkflow.addToCommands(step2)

        and: "A ScheduledExecution"
        def se = new ScheduledExecution(
            jobName: "test-job",
            project: "test-project"
        )

        when: "Setting and getting workflow data"
        se.setWorkflowData(originalWorkflow)
        def deserializedWorkflow = se.getWorkflowData() as Workflow

        then: "Workflow properties should be preserved"
        deserializedWorkflow != null
        deserializedWorkflow.keepgoing == originalWorkflow.keepgoing
        deserializedWorkflow.strategy == originalWorkflow.strategy
        deserializedWorkflow.threadcount == originalWorkflow.threadcount

        and: "Steps should be preserved"
        deserializedWorkflow.commands.size() == 2
        deserializedWorkflow.commands[0].pluginType == 'test-plugin'
        deserializedWorkflow.commands[1].pluginType == 'another-plugin'
    }

    def "test workflowJson field is mapped as text type"() {
        given: "A ScheduledExecution with large workflow JSON"
        def se = new ScheduledExecution(
            jobName: "test-job",
            project: "test-project"
        )
        def largeWorkflow = new Workflow(keepgoing: true, strategy: 'node-first', commands: [])
        // Add many steps to create large JSON
        100.times { i ->
            largeWorkflow.addToCommands(new PluginStep(
                type: "plugin-${i}",
                nodeStep: true,
                configuration: [data: "value-${i}"]
            ))
        }

        when: "Setting workflow data"
        se.setWorkflowData(largeWorkflow)

        then: "Should handle large text without issues"
        se.workflowJson != null
        se.workflowJson.length() > 1000 // Should be quite large
    }

    def "test getWorkflowJsonMap helper method"() {
        given: "A ScheduledExecution with workflowJson"
        def se = new ScheduledExecution(
            jobName: "test-job",
            project: "test-project"
        )
        se.workflowJson = '{"keepgoing":true,"strategy":"node-first","commands":[]}'

        when: "Getting workflow as map"
        def result = se.getWorkflowJsonMap()

        then: "Should return map representation"
        result != null
        result instanceof Map
        result.keepgoing == true
        result.strategy == 'node-first'
        result.commands instanceof List
    }

    def "test getWorkflowJsonMap returns null when no JSON"() {
        given: "A ScheduledExecution without workflowJson"
        def se = new ScheduledExecution(
            jobName: "test-job",
            project: "test-project"
        )

        when: "Getting workflow as map"
        def result = se.getWorkflowJsonMap()

        then: "Should return null"
        result == null
    }

    def "test setWorkflowData handles WorkflowData interface implementations"() {
        given: "A mock WorkflowData implementation"
        def workflowData = Mock(WorkflowData) {
            getKeepgoing() >> true
            getStrategy() >> 'sequential'
            getThreadcount() >> 2
            getSteps() >> []
            getPluginConfigMap() >> [:]
        }
        workflowData.respondsTo('toMap') >> false

        and: "A ScheduledExecution"
        def se = new ScheduledExecution(
            jobName: "test-job",
            project: "test-project"
        )

        when: "Setting workflow data"
        se.setWorkflowData(workflowData)

        then: "Should serialize successfully"
        se.workflowJson != null
        se.workflowJson.contains('"keepgoing":true')
        se.workflowJson.contains('"strategy":"sequential"')
    }
}
