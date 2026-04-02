package org.rundeck.tests.functional.api.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer

/**
 * Functional tests for Conditional Workflow Logic
 * Tests the conditional step execution, validation, and export/import functionality
 */
@APITest
class ConditionalWorkflowSpec extends BaseContainer {

    static final String PROJECT_NAME = "ConditionalWorkflowTest"
    private static final ObjectMapper MAPPER = new ObjectMapper()

    def setupSpec() {
        startEnvironment()
        setupProject(PROJECT_NAME)
    }

    def "create job with conditional step using JSON format"() {
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

        then: "job creation succeeds"
            response.code() == 200
            def json = jsonValue(response.body(), Map)
            json.succeeded != null
            json.succeeded.size() == 1
            json.succeeded[0].id != null
    }

    def "create job with conditional step using YAML format"() {
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

        then: "job import succeeds"
            response.code() == 200
            def json = jsonValue(response.body(), Map)
            json.succeeded.size() == 1
            json.failed.size() == 0
    }

    def "reject export of job with conditional step in XML format"() {
        given: "a job with conditional logic"
            def jobDef = [[
                name: "conditional-export-xml-fail",
                project: PROJECT_NAME,
                description: "Export test job XML - should fail",
                loglevel: "INFO",
                sequence: [
                    keepgoing: false,
                    strategy: "sequential",
                    commands: [
                        [
                            type: "conditional",
                            nodeStep: true,
                            conditionGroups: [[
                                [
                                    key: '${option.test}',
                                    operator: "==",
                                    value: "true"
                                ]
                            ]],
                            subSteps: [
                                [
                                    exec: "echo 'test'",
                                    description: "Test step"
                                ]
                            ]
                        ]
                    ]
                ],
                options: [
                    [name: "test", required: true, value: "true"]
                ]
            ]]

            def createResponse = client.doPost("/project/${PROJECT_NAME}/jobs/import?format=json",
                MAPPER.writeValueAsString(jobDef), "application/json")
            assert createResponse.code() == 200
            def jobJson = jsonValue(createResponse.body(), Map)
            def jobId = jobJson.succeeded[0].id

        when: "the job is exported in XML format"
            def exportResponse = doGet("/job/${jobId}?format=xml")

        then: "export fails or returns error"
            exportResponse.code() >= 400 ||
            (exportResponse.code() == 200 && exportResponse.body().string().contains("error"))
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
