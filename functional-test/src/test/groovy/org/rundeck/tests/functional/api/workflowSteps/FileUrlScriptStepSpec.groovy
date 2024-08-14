package org.rundeck.tests.functional.api.workflowSteps


import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.responses.execution.ExecutionOutput
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer

import java.util.stream.Collectors

@APITest
class FileUrlScriptStepSpec extends BaseContainer{

    public static final String TEST_PROJECT = "core-jsch-executor-test"
    public static final String TEST_SSH_ARCHIVE_DIR = "/projects-import/core-jsch-executor-test"
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
        waitingResourceEnabled(TEST_PROJECT, "ssh-node")
    }

    def "test execute script from URL running locally"() {
        given:
        def jobId = "1c496173-f9ec-4991-b93f-4761c2cf947e"
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

        then:
        def exec= JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                json.id as String,
                new ObjectMapper(),
                client,
                WaitingTime.MODERATE,
                WaitingTime.EXCESSIVE
        )
        String execId = json.id
        def entries = getExecutionOutput(execId)
        entries.contains("Hello, World!")

    }

    def "test execute script from URL running on node and run file on node"() {
        given:
        def jobId = "c14c992d-4738-44b8-945e-9349ae487b77"
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

        then:
        def exec= JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                json.id as String,
                new ObjectMapper(),
                client,
                WaitingTime.MODERATE,
                WaitingTime.EXCESSIVE
        )
        String execId = json.id
        def entries = getExecutionOutput(execId)
        entries.contains("Hello, World!")

    }
}