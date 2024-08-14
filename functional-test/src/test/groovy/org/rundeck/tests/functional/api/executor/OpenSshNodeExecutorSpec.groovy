package org.rundeck.tests.functional.api.executor

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.annotations.APITest
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer

@APITest
class OpenSshNodeExecutorSpec extends BaseContainer{

    public static final String TEST_PROJECT = "openssh-executor-test"
    public static final String TEST_SSH_ARCHIVE_DIR = "/projects-import/openssh-executor-test"
    public static final String NODE_KEY_PASSPHRASE = "testpassphrase123"
    public static final String NODE_USER_PASSWORD  = "testpassword123"
    public static final String USER_VAULT_PASSWORD = "vault123"

    def setupSpec() {
        String keyPath = getClass().getClassLoader().getResource("docker/compose/oss").getPath()+"/keys"

        loadKeysForNodes(keyPath, TEST_PROJECT, NODE_KEY_PASSPHRASE, NODE_USER_PASSWORD, USER_VAULT_PASSWORD)

        setupProjectArchiveDirectory(
                TEST_PROJECT,
                new File(getClass().getResource(TEST_SSH_ARCHIVE_DIR).getPath()),
                [
                        "importConfig": "true",
                        "importACL": "true",
                        "importNodesSources": "true",
                        "jobUuidOption": "preserve"
                ]
        )
        //wait for node to be available
        waitingResourceEnabled(TEST_PROJECT, "ssh-node")
    }

    def "test simple command step using ssh and password authentication"() {
        given:
        def jobId = "9532efbb-bd36-49c6-bd3a-203177ea876b"
        def jobConfig = [
                "loglevel": "DEBUG"
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
        def exec= JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                json.id as String,
                new ObjectMapper(),
                client,
                WaitingTime.MODERATE,
                WaitingTime.EXCESSIVE
        )
        then:
        exec.status==ExecutionStatus.SUCCEEDED.state

    }

    def "test simple script step using ssh and password authentication"() {
        given:
        def jobId = "881725be-0266-47e5-b2ae-64e978c42ce3"
        def jobConfig = [
                "loglevel": "DEBUG"
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
        def exec= JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                json.id as String,
                new ObjectMapper(),
                client,
                WaitingTime.MODERATE,
                WaitingTime.EXCESSIVE
        )
        then:
        exec.status==ExecutionStatus.SUCCEEDED.state

    }
}