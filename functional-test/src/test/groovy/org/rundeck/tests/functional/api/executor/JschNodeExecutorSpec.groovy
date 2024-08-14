package org.rundeck.tests.functional.api.executor

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.annotations.APITest
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer

@APITest
class JschNodeExecutorSpec extends BaseContainer{

    public static final String TEST_PROJECT = "core-jsch-executor-test"
    public static final String TEST_SSH_ARCHIVE_DIR = "/projects-import/core-jsch-executor-test"
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
        //wait for ansible node to be available
        waitingResourceEnabled(TEST_PROJECT, "ssh-node")
    }

    def "test simple command step using ssh and password authentication"() {
        given:
        def jobId = "c7d2b5e9-3c4b-48c1-88e9-1f93c6a4bcd7"
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
        def jobId = "a5fbe8c2-1a8b-4a6f-ae34-92e5b3d4f8a9"
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