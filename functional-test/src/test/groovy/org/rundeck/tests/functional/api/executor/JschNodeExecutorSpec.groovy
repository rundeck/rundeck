package org.rundeck.tests.functional.api.executor


import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.api.responses.execution.ExecutionOutput
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer
import org.rundeck.util.api.responses.execution.RunCommand

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
        def exec= JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                json.id as String,
                client,
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
        def exec= JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                json.id as String,
                client,
                WaitingTime.EXCESSIVE
        )
        then:
        exec.status==ExecutionStatus.SUCCEEDED.state
    }

    def "execute command on remote node using SSH key from key storage"() {
        given: "A project that uses storage key for SSH authentication"
        // Note: Setup of this project with storage key nodes happens in setupSpec()

        and: "a command to execute"
        def execCommand = "whoami"

        when: "we execute the command on a node that uses storage key authentication"

        def filter = "name:ssh-node"
        def parsedResponseBody = client.post(
            "/project/${TEST_PROJECT}/run/command?exec=${execCommand}&filter=" + URLEncoder.encode(filter,null), null, RunCommand)
        String newExecId = parsedResponseBody.execution.id

        and: "wait for it to complete"
        def exec = JobUtils.waitForExecution(
            ExecutionStatus.SUCCEEDED.state,
            newExecId,
            client,
            WaitingTime.EXCESSIVE
        )

        then: "verify the execution succeeded"
        exec.status == ExecutionStatus.SUCCEEDED.state

        and: "fetch the output to verify it worked"
        def output = get("/execution/${newExecId}/output", ExecutionOutput)
        def outputLines = output.entries.findAll { it.log }.collect { it.log }

        and: "verify the output is not empty"
        !outputLines.isEmpty()
    }

    def "execute command with node filtering by tags"() {
        given: "A project with tagged nodes"
        // Note: Setup of this project with tagged nodes happens in setupSpec()

        and: "a command to execute"
        def execCommand = "uname -n"

        when: "we execute the command on nodes filtered by tag"

        def filter = "tags:executor-test"
        def parsedResponseBody = client.post(
            "/project/${TEST_PROJECT}/run/command?exec=${execCommand}&filter=" + URLEncoder.encode(filter,null), null, RunCommand)
        String newExecId = parsedResponseBody.execution.id

        and: "wait for it to complete"
        def exec = JobUtils.waitForExecution(
            ExecutionStatus.SUCCEEDED.state,
            newExecId,
            client,
            WaitingTime.EXCESSIVE
        )

        then: "verify the execution succeeded"
        exec.status == ExecutionStatus.SUCCEEDED.state

        and: "fetch the output and execution details"
        def output = get("/execution/${newExecId}/output", ExecutionOutput)
        def execution = get("/execution/${newExecId}", Execution)
        def nodesList = execution.successfulNodes

        and: "verify we executed on nodes with the 'executor-test' tag"
        nodesList.size() > 0
        nodesList.size() == 4
        output.entries.size() > 0
        output.entries.size() == 4

        and: "verify the output contains the hostname for each node"
        def outputLines = output.entries.findAll { it.log }.collect { it.log }
        !outputLines.isEmpty()
    }
}