package org.rundeck.tests.functional.api.executor

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.annotations.APITest
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer

@APITest
class SshjNodeExecutorSpec extends BaseContainer{

    public static final String TEST_PROJECT = "core-sshj-executor-test"
    public static final String TEST_SSH_ARCHIVE_DIR = "/projects-import/core-sshj-executor-test"
    public static final String NODE_KEY_PASSPHRASE = "testpassphrase123"
    public static final String NODE_USER_PASSWORD  = "testpassword123"
    public static final String USER_VAULT_PASSWORD = "vault123"

    @Override
    void startEnvironment() {
        String keyPath = getClass().getClassLoader().getResource("docker/compose/oss").getPath()+"/keys"

        super.startEnvironment()

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
        def jobId = "7c58a292-d987-4864-8882-6f5e5c6a4cb9"
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
        def jobId = "d9f12a60-7b2f-4e92-8b37-b6c9e23f7df7"
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