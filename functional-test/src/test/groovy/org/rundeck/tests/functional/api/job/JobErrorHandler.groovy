package org.rundeck.tests.functional.api.job

import org.rundeck.util.annotations.APITest
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer

@APITest
class JobErrorHandler extends BaseContainer{

    public static final String TEST_PROJECT = "error-handler-project"
    public static final String ARCHIVE_DIR = "/projects-import/error-handler-project"

    def setupSpec() {
        setupProjectArchiveDirectory(
                TEST_PROJECT,
                new File(getClass().getResource(ARCHIVE_DIR).getPath()),
                [
                        "importConfig": "true",
                        "importACL": "true",
                        "importNodesSources": "true",
                        "jobUuidOption": "preserve"
                ]
        )
    }

    def "when a workflow step job is referenced and it doesn't exist if an error handler is present should be triggered"(){
        given:
        def jobId = "f9b34a63-ff95-41f4-9646-87d90b35fc3d"
        def jobConfig = [
                "loglevel": "INFO"
        ]
        when:
        def response = doPost("/job/${jobId}/executions",jobConfig)
        then:
        verifyAll {
            response != null
            response.successful
        }
        when:
        def json = client.jsonValue(response.body(), Map)
        String execId = json.id
        JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                execId,
                client,
                WaitingTime.EXCESSIVE
        )
        def logs = getAllLogs(execId){}
        then:
        logs.toString().contains("Job [Job to delete] not found by name, project")
        logs.toString().contains("this is the error handler and should keep going if there is a failure WORKFLOW-STEP")
    }

    def "when a node step job is referenced and it doesn't exist if an error handler is present should be triggered"(){
        given:
        def jobId = "f9b34a63-ff95-41f4-9646-87d90b35fc3d"
        def jobConfig = [
                "loglevel": "INFO"
        ]
        when:
        def response = doPost("/job/${jobId}/executions",jobConfig)
        then:
        verifyAll {
            response != null
            response.successful
        }
        when:
        def json = client.jsonValue(response.body(), Map)
        String execId = json.id
        JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                execId,
                client,
                WaitingTime.EXCESSIVE
        )
        def logs = getAllLogs(execId){}
        then:
        logs.toString().contains("Job [Job to delete] not found by name, project")
        logs.toString().contains("this is the error handler and should keep going if there is a failure NODE-STEP")
    }
}
