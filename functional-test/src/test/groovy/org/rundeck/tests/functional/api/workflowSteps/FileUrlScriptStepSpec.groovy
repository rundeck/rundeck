package org.rundeck.tests.functional.api.workflowSteps


import org.rundeck.util.annotations.APITest
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer

@APITest
class FileUrlScriptStepSpec extends BaseContainer{

    public static final String TEST_PROJECT = "core-jsch-fileurl-test"
    public static final String TEST_SSH_ARCHIVE_DIR = "/projects-import/core-jsch-executor-test"
    public static final String NODE_KEY_PASSPHRASE = "testpassphrase123"
    public static final String NODE_USER_PASSWORD  = "testpassword123"
    public static final String USER_VAULT_PASSWORD = "vault123"

    static String localScriptJobId
    static String nodeScriptJobId

    @Override
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
                        "jobUuidOption": "remove"
                ]
        )
        waitingResourceEnabled(TEST_PROJECT, "ssh-node")

        def jobs = JobUtils.getJobsForProject(client, TEST_PROJECT)
        localScriptJobId = jobs.find { it.name == "Run script from URL" }?.id
        nodeScriptJobId = jobs.find { it.name == "Run script through URL and filesystem on node" }?.id

        if (!localScriptJobId || !nodeScriptJobId) {
            throw new IllegalStateException(
                "Required jobs not found in project ${TEST_PROJECT}. " +
                "Found: ${jobs.collect { it.name }}"
            )
        }
    }

    def cleanupSpec(){
        deleteProject(TEST_PROJECT)
    }

    def "test execute script from URL running locally"() {
        given:
        def jobConfig = [
                "loglevel": "DEBUG"
        ]

        when:
        def response = doPost("/job/${localScriptJobId}/executions",jobConfig)
        then:
        verifyAll {
            response != null
            response.successful
        }
        when:

        def json = client.jsonValue(response.body(), Map)

        then:
        def exec= JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                json.id as String,
                client,
                WaitingTime.EXCESSIVE
        )
        String execId = json.id
        def entries = getExecutionOutputLines(execId)
        entries.contains("Hello, World!")

    }

    def "test execute script from URL running on node and run file on node"() {
        given:
        def jobConfig = [
                "loglevel": "DEBUG"
        ]

        when:
        def response = doPost("/job/${nodeScriptJobId}/executions",jobConfig)
        then:
        verifyAll {
            response != null
            response.successful
        }
        when:

        def json = client.jsonValue(response.body(), Map)

        then:
        def exec= JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                json.id as String,
                client,
                WaitingTime.EXCESSIVE
        )
        String execId = json.id
        def entries = getExecutionOutputLines(execId)
        entries.contains("Hello, World!")

    }
}