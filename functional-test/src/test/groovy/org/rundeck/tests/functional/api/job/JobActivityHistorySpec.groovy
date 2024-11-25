package org.rundeck.tests.functional.api.job

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class JobActivityHistorySpec extends BaseContainer {
    def setupSpec() {
        startEnvironment()

    }

    def "Retrieve job Activity history from Job List Page "() {
        given: "A Job exists in the project with execution history"
        def jobId = "3957f735-5269-4c85-bc57-f123598886f6"

        when: "The activity history for the job is requested"

        def response = doGet("/job/${jobId}/executions")
        def expectedKeys = [
                'id', 'status', 'date-started', 'date-ended', 'user', 'project'
        ]
        then: "The response should contain  a list of execution details for the Job"
        verifyAll {
            response.successful
            response.code() == 200
            def json = jsonValue(response.body(), Map)
            json.executions.every { execution ->
                def executionMap = execution as Map
                println "Execution Details: ${executionMap}"
                expectedKeys.every { key ->
                    executionMap.containsKey(key)

                }
            }
        }
    }

    def "handle invalid job ID"() {
        given: "An invalid job ID"
        def jobId = "invalid-job-id"

        when: "Requesting the activity history for the job"
        def response = doGet("/job/${jobId}/executions")

        then: "The response should contain an error message"
        verifyAll {
            response.code() == 404
            def json = jsonValue(response.body(), Map)
            json.message == "Job ID does not exist: ${jobId}"
            println("The error message: ${json.message}")
        }
    }
}