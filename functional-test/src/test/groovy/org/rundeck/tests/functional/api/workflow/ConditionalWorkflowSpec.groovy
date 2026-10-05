package org.rundeck.tests.functional.api.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.annotations.APITest
import org.rundeck.util.annotations.ExcludePro
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer

/**
 * Functional tests for Conditional Workflow Logic in the open source build.
 * Conditional steps require a workflow strategy that supports them, which only the
 * Enterprise build provides, so the open source build rejects jobs that contain them.
 * Excluded from the Enterprise run, where conditional jobs are accepted (covered by
 * the Enterprise ConditionalWorkflowProSpec).
 */
@APITest
@ExcludePro
class ConditionalWorkflowSpec extends BaseContainer {

    static final String PROJECT_NAME = "ConditionalWorkflowTest"
    private static final ObjectMapper MAPPER = new ObjectMapper()

    def setupSpec() {
        startEnvironment()
        setupProject(PROJECT_NAME)
    }

    def "reject job with conditional step using JSON format"() {
        given: "a job definition with conditional logic in JSON format"
            def jobDef = [[
                name: "conditional-job-json",
                project: PROJECT_NAME,
                description: "Test job with conditional logic",
                loglevel: "INFO",
                sequence: [
                    keepgoing: false,
                    strategy: "sequential",
                    commands: [
                        [
                            exec: "echo 'Setting test variable'",
                            description: "Step 1: Set variable"
                        ],
                        [
                            type: "conditional",
                            nodeStep: true,
                            conditionGroups: [[
                                [
                                    key: '${option.env}',
                                    operator: "==",
                                    value: "production"
                                ]
                            ]],
                            subSteps: [
                                [
                                    exec: "echo 'Running in production mode'",
                                    description: "Production step"
                                ]
                            ]
                        ],
                        [
                            exec: "echo 'Final step'",
                            description: "Step 3: Final"
                        ]
                    ]
                ],
                options: [
                    [
                        name: "env",
                        description: "Environment",
                        required: true,
                        value: "production"
                    ]
                ]
            ]]

        when: "the job is imported via API"
            def response = client.doPost("/project/${PROJECT_NAME}/jobs/import?format=json",
                MAPPER.writeValueAsString(jobDef), "application/json")

        then: "job import is rejected because no workflow strategy supports conditional steps"
            response.code() == 200
            def json = jsonValue(response.body(), Map)
            json.succeeded.size() == 0
            json.failed.size() == 1
            json.failed[0].error.contains("does not support conditional steps")
    }

    def "reject job with conditional step using YAML format"() {
        given: "a job definition with conditional logic in YAML"
            def yamlContent = """
- name: conditional-job-yaml
  project: ${PROJECT_NAME}
  description: Test job with conditional logic
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: sequential
    commands:
      - exec: echo 'Step 1'
        description: Initial step
      - type: conditional
        nodeStep: true
        conditionGroups:
          - - key: '\${option.deployType}'
              operator: '=='
              value: 'full'
        subSteps:
          - exec: echo 'Full deployment selected'
            description: Full deployment step
      - exec: echo 'Final step'
        description: Final step
  options:
    - name: deployType
      description: Deployment type
      required: true
      value: full
"""

        when: "the job is imported via API"
            def response = client.doPost("/project/${PROJECT_NAME}/jobs/import?format=yaml", yamlContent, "application/yaml")

        then: "job import is rejected because no workflow strategy supports conditional steps"
            response.code() == 200
            def json = jsonValue(response.body(), Map)
            json.succeeded.size() == 0
            json.failed.size() == 1
            json.failed[0].error.contains("does not support conditional steps")
    }

    /**
     * Helper method to extract output lines from execution
     */
    List<String> getExecutionOutputLines(String execId) {
        def output = get("/execution/${execId}/output", Map)
        def entries = output.entries ?: []
        return entries.collect { it.log as String }
    }
}
