package org.rundeck.tests.functional.api.job

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

/**
 * Regression tests for RUN-4538.
 *
 * Java 20+ (JDK-8295750) changed java.net.URL validation from lazy to eager. When a job option's
 * valuesUrl contains unexpanded ${globals.*} or ${option.*} references, GORM attempts to hydrate
 * the URL field before variable expansion occurs, causing MalformedURLException: Illegal character
 * found in host: '{'. These tests verify the job endpoints remain available regardless of whether
 * global variables are defined in the project.
 */
@APITest
class JobOptionValuesUrlGlobalsSpec extends BaseContainer {

    static final String PROJECT_NO_GLOBALS   = "run-4538-no-globals"
    static final String PROJECT_WITH_GLOBALS = "run-4538-with-globals"

    def setupSpec() {
        startEnvironment()
        setupProject(PROJECT_NO_GLOBALS)
        setupProject(PROJECT_WITH_GLOBALS, [
            "project.globals.api_host"  : "internal-api.example.com",
            "project.globals.env"       : "prod",
            "project.globals.data_path" : "options"
        ])
    }

    def cleanupSpec() {
        deleteProject(PROJECT_NO_GLOBALS)
        deleteProject(PROJECT_WITH_GLOBALS)
    }

    def "job definition endpoint returns 200 when option valuesUrl contains unexpanded global variables and project has no globals"() {
        given: "a job with an option whose valuesUrl references undefined project globals"
        def jobId = importJobYaml(PROJECT_NO_GLOBALS, """
- name: RUN-4538 Remote URL Globals No Vars
  description: "Regression - MalformedURLException with unexpanded globals in valuesUrl (no globals defined)"
  loglevel: INFO
  sequence:
    commands:
    - exec: echo hello
    keepgoing: false
    strategy: node-first
  options:
  - name: DATA
    label: Data
    required: true
    enforced: true
    valuesUrl: 'https://\${globals.api_host}/\${globals.env}/\${globals.data_path}/values.json'
""")

        when: "the job definition is fetched — triggers GORM hydration of URL-typed fields"
        def response = doGet("/job/${jobId}")

        then: "no MalformedURLException — response is 200"
        response.code() == 200
    }

    def "job definition endpoint returns 200 when option valuesUrl contains unexpanded global variables and project has globals defined"() {
        given: "a job with an option whose valuesUrl references defined project globals"
        def jobId = importJobYaml(PROJECT_WITH_GLOBALS, """
- name: RUN-4538 Remote URL Globals With Vars
  description: "Regression - MalformedURLException with unexpanded globals in valuesUrl (globals defined)"
  loglevel: INFO
  sequence:
    commands:
    - exec: echo hello
    keepgoing: false
    strategy: node-first
  options:
  - name: DATA
    label: Data
    required: true
    enforced: true
    valuesUrl: 'https://\${globals.api_host}/\${globals.env}/\${globals.data_path}/values.json'
""")

        when:
        def response = doGet("/job/${jobId}")

        then:
        response.code() == 200
    }

    def "job definition endpoint returns 200 when option valuesUrl contains chained option variable references"() {
        given: "a job with multiple options where the second option URL references the first option's value"
        def jobId = importJobYaml(PROJECT_NO_GLOBALS, """
- name: RUN-4538 Remote URL Chained Option Refs
  description: "Regression - chained option value references in valuesUrl"
  loglevel: INFO
  sequence:
    commands:
    - exec: echo hello
    keepgoing: false
    strategy: node-first
  options:
  - name: ENV
    label: Environment
    required: true
    enforced: true
    valuesUrl: 'https://\${globals.api_host}/\${globals.data_path}/envs.json'
  - name: DATA
    label: Data
    required: true
    enforced: true
    multivalued: true
    delimiter: ','
    valuesUrl: 'https://\${globals.api_host}/\${globals.data_path}/data-\${option.ENV.value}.json'
""")

        when:
        def response = doGet("/job/${jobId}")

        then:
        response.code() == 200
    }

    def "project jobs listing returns 200 when jobs have options with unexpanded global variables in valuesUrl"() {
        given: "a job with a global-variable valuesUrl already imported"
        importJobYaml(PROJECT_NO_GLOBALS, """
- name: RUN-4538 Listing Test
  description: "Regression - job listing must not fail when options have globals in valuesUrl"
  loglevel: INFO
  sequence:
    commands:
    - exec: echo hello
    keepgoing: false
    strategy: node-first
  options:
  - name: DATA
    label: Data
    required: true
    enforced: true
    valuesUrl: 'https://\${globals.api_host}/\${globals.data_path}/values.json'
""")

        when: "all jobs in the project are listed"
        def response = doGet("/project/${PROJECT_NO_GLOBALS}/jobs")

        then: "listing returns 200 — no MalformedURLException during domain hydration"
        response.code() == 200
    }

    def "job import succeeds when option valuesUrl contains variable references"() {
        given: "a job definition with globals and option variable references in valuesUrl"
        def yaml = """
- name: RUN-4538 Import With Variables
  description: "Validation must accept templates with variable references"
  loglevel: INFO
  sequence:
    commands:
    - exec: echo hello
    keepgoing: false
    strategy: node-first
  options:
  - name: ENV
    label: Environment
    required: true
    enforced: true
    valuesUrl: 'https://\${globals.api_host}/\${globals.data_path}/envs.json'
  - name: DATA
    label: Data
    required: true
    enforced: true
    valuesUrl: 'https://\${globals.api_host}/data-\${option.ENV.value}.json'
"""
        when: "the job is imported via API"
        def tempFile = File.createTempFile("run-4538-vars", ".yaml")
        tempFile.text = yaml.stripIndent()
        def response
        try {
            response = client.doPost(
                "/project/${PROJECT_NO_GLOBALS}/jobs/import?fileformat=yaml&dupeOption=update",
                tempFile,
                "application/yaml"
            )
        } finally {
            tempFile.delete()
        }

        then: "import succeeds — variable references in valuesUrl are not rejected"
        def body = new ObjectMapper().readValue(response.body().string(), Map)
        response.code() == 200
        body.succeeded?.size() == 1
        body.failed?.size() == 0
    }

    def "job import fails when option valuesUrl is an invalid URL with no variable references"() {
        given: "a job definition with a plain invalid valuesUrl (no variables)"
        def yaml = """
- name: RUN-4538 Import Invalid URL
  description: "Plain invalid URLs without variables must be rejected"
  loglevel: INFO
  sequence:
    commands:
    - exec: echo hello
    keepgoing: false
    strategy: node-first
  options:
  - name: DATA
    label: Data
    required: true
    enforced: true
    valuesUrl: 'not-a-url'
"""
        when: "the job is imported via API"
        def tempFile = File.createTempFile("run-4538-invalid", ".yaml")
        tempFile.text = yaml.stripIndent()
        def response
        try {
            response = client.doPost(
                "/project/${PROJECT_NO_GLOBALS}/jobs/import?fileformat=yaml&dupeOption=update",
                tempFile,
                "application/yaml"
            )
        } finally {
            tempFile.delete()
        }

        then: "import fails with a validation error for the invalid URL"
        def body = new ObjectMapper().readValue(response.body().string(), Map)
        response.code() == 200
        body.succeeded?.size() == 0
        body.failed?.size() == 1
        body.failed[0].error?.contains('valid URL')
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String importJobYaml(String projectName, String yaml) {
        def tempFile = File.createTempFile("run-4538-job", ".yaml")
        try {
            tempFile.text = yaml.stripIndent()
            try (def response = client.doPost(
                "/project/${projectName}/jobs/import?fileformat=yaml&dupeOption=update",
                tempFile,
                "application/yaml"
            )) {
                def bodyStr = response.body().string()
                if (response.code() != 200) {
                    throw new AssertionError("Job import failed with HTTP ${response.code()}: ${bodyStr}")
                }
                def result = new ObjectMapper().readValue(bodyStr, Map) as Map
                assert result.succeeded?.size() == 1, "Expected 1 succeeded import, got: ${result}"
                return result.succeeded[0].id as String
            }
        } finally {
            tempFile.delete()
        }
    }
}
