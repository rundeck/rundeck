package org.rundeck.tests.functional.api.executor

import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.api.responses.execution.ExecutionOutput
import org.rundeck.util.api.responses.execution.RunCommand
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer
import spock.lang.Unroll

@APITest
class JschNodeExecutorSpec extends BaseContainer{

    public static final String TEST_PROJECT = "core-jsch-executor-test"
    public static final String TEST_SSH_ARCHIVE_DIR = "/projects-import/core-jsch-executor-test"
    public static final String LOCAL_KEYS_PATH = "/home/rundeck/privatekeys"
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
        def json = post("/job/${jobId}/executions",jobConfig, Map)
        assert json.id != null
        then:
        def exec= JobUtils.waitForSuccess(
                json.id as String,
                client,
                WaitingTime.EXCESSIVE
        )
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
            "/project/${TEST_PROJECT}/run/command?exec=${execCommand}&filter=" + URLEncoder.encode(filter,'UTF-8'), null, RunCommand)
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

    def "execute command on remote node using #tags"() {
        given: "Node filter specifying SSH authentication mechanism"
        // Note: Setup of this project with storage key nodes happens in setupSpec()

        and: "a command to execute"
        def execCommand = "whoami"

        when: "we execute the command"

        def filter = 'tags:' + tags
        def parsedResponseBody = client.post(
            "/project/${TEST_PROJECT}/run/command?exec=${execCommand}&filter=" + URLEncoder.encode(filter,'UTF-8'), null, RunCommand)
        String newExecId = parsedResponseBody.execution.id

        and: "wait for it to complete"
        def exec = JobUtils.waitForSuccess(
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
        where:
            tags << [
                'auth-method-key',
                'auth-method-password',
                'auth-method-key-agent',
                'auth-method-key-passphrase',
                'auth-method-key-file',
                'auth-method-key-file-passphrase'
            ]
    }


    @Unroll
    def "run job with remote steps on node using #tags"() {
        given: "job that runs basic steps and expects a node filter"
            def jobId = "501de248-31a3-4720-8558-aa0c30ef9cdd"
            def jobConfig = [
                loglevel: 'DEBUG',
                filter: 'tags:' + tags
            ]

        when: "run the job"
            def json = post("/job/${jobId}/run", jobConfig, Map)
        then: "execution id was returned"
            json.id != null
        when: "wait for the job to succeed"

            def exec= JobUtils.waitForSuccess(
                json.id as String,
                client,
                WaitingTime.EXCESSIVE
            )
        then: "verify the number of successful nodes"
            exec.successfulNodes.size() == 1
        where:
            tags << [
                'auth-method-key',
                'auth-method-password',
                'auth-method-key-agent',
                'auth-method-key-passphrase',
                'auth-method-key-file',
                'auth-method-key-file-passphrase'
            ]
    }

    @Unroll
    def "run job with passphrase option on node using #tags"() {
        given: "job that runs basic steps and expects a node filter and passphrase option"
            def jobId = "74ad8672-a497-4880-a90f-739e982ba37f"
            def jobConfig = [
                "loglevel": "DEBUG",
                filter: 'tags:' + tags,
                options:[
                    sshKeyPassphrase: NODE_KEY_PASSPHRASE
                ]
            ]

        when: "run the job"
            def json = post("/job/${jobId}/run", jobConfig, Map)
        then: "execution id was returned"
            json.id != null
        when: "wait for the job to succeed"

            def exec= JobUtils.waitForSuccess(
                json.id as String,
                client,
                WaitingTime.EXCESSIVE
            )
        then: "verify the number of successful nodes"
            exec.successfulNodes.size() == 1
        where:
            tags << [
                'auth-method-key-file-option-passphrase',
                'auth-method-key-option-passphrase'
            ]
    }

    def "execute command with node filtering by tags"() {
        given: "A project with tagged nodes"
        // Note: Setup of this project with tagged nodes happens in setupSpec()

        and: "a command to execute"
        def execCommand = "uname -n"

        when: "we execute the command on nodes filtered by tag"

        def filter = "tags:executor-test"
        def parsedResponseBody = client.post(
            "/project/${TEST_PROJECT}/run/command?exec=${execCommand}&filter=" + URLEncoder.encode(filter,'UTF-8'), null, RunCommand)
        String newExecId = parsedResponseBody.execution.id

        and: "wait for it to complete"
        def exec = JobUtils.waitForSuccess(
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
        nodesList.size() == 6
        output.entries.size() > 0
        output.entries.size() == 6

        and: "verify the output contains the hostname for each node"
        def outputLines = output.entries.findAll { it.log }.collect { it.log }
        !outputLines.isEmpty()
    }

    def "execute basic script on remote nodes"() {
        given: "A project that uses storage key for SSH authentication"
        // Note: Setup of this project with storage key nodes happens in setupSpec()

        and: "a script to execute"
        def scriptPath = getClass().getClassLoader().getResource("test-files/test-dispatch-script.sh").getPath()
        File scriptFile = new File(scriptPath)
        scriptFile.setExecutable(true)

        when: "we execute the script on a node that uses storage key authentication"
        def filter = "name:ssh-node"
        def scriptRunResponse = client.doPostWithFormData(
            "/project/${TEST_PROJECT}/run/script?filter=" + URLEncoder.encode(filter, 'UTF-8'),
            "scriptFile",
            scriptFile
        )
        assert scriptRunResponse.successful
        def runScriptJson = client.jsonValue(scriptRunResponse.body(), Map)
        String newExecId = runScriptJson.execution.id

        and: "wait for it to complete"
        def exec = JobUtils.waitForSuccess(
            newExecId,
            client,
            WaitingTime.EXCESSIVE
        )

        then: "verify the execution succeeded"
        exec.status == ExecutionStatus.SUCCEEDED.state

        and: "fetch the output to verify it worked"
        def output = get("/execution/${newExecId}/output", ExecutionOutput)
        def outputLines = output.entries.findAll { it.log }.collect { it.log }

        and: "verify the output contains expected script output"
        outputLines.find { it.contains("This is test-dispatch-script.sh") }
        outputLines.find { it.contains("On node ssh-node ssh-node") }
        outputLines.find { it.contains("With tags:") }
        outputLines.find { it == ("With args: ") }
    }

    def "execute script with command-line arguments"() {
        given: "A project that uses storage key for SSH authentication"
        // Note: Setup of this project with storage key nodes happens in setupSpec()

        and: "a script to execute with arguments"
        def scriptPath = getClass().getClassLoader().getResource("test-files/test-dispatch-script.sh").getPath()
        File scriptFile = new File(scriptPath)
        scriptFile.setExecutable(true)
        def args = "arg1 arg2"

        when: "we execute the script with arguments on the remote node"
        def filter = "name:ssh-node"
        def scriptRunResponse = client.doPostWithFormData(
            "/project/${TEST_PROJECT}/run/script?filter=" + URLEncoder.encode(filter, 'UTF-8') + "&argString=" + URLEncoder.encode(args, 'UTF-8'),
            "scriptFile",
            scriptFile
        )
        assert scriptRunResponse.successful
        def runScriptJson = client.jsonValue(scriptRunResponse.body(), Map)
        String newExecId = runScriptJson.execution.id

        and: "wait for it to complete"
        def exec = JobUtils.waitForSuccess(
            newExecId,
            client,
            WaitingTime.EXCESSIVE
        )

        then: "verify the execution succeeded"
        exec.status == ExecutionStatus.SUCCEEDED.state

        and: "fetch the output to verify it worked"
        def output = get("/execution/${newExecId}/output", ExecutionOutput)
        def outputLines = output.entries.findAll { it.log }.collect { it.log }

        and: "verify the output contains expected script output with arguments"
        outputLines.find { it.contains("This is test-dispatch-script.sh") }
        outputLines.find { it.equals("With args: arg1 arg2") }
    }

    def "execute script with different line ending formats (DOS/Windows style)"() {
        given: "A project that uses storage key for SSH authentication"
        // Note: Setup of this project with storage key nodes happens in setupSpec()

        and: "a script with DOS/Windows style line endings to execute"
        def scriptPath = getClass().getClassLoader().getResource("test-files/test-dispatch-script-dos.sh").getPath()
        File scriptFile = new File(scriptPath)
        scriptFile.setExecutable(true)

        when: "we execute the DOS-formatted script on the remote node"
        def filter = "name:ssh-node"
        def scriptRunResponse = client.doPostWithFormData(
            "/project/${TEST_PROJECT}/run/script?filter=" + URLEncoder.encode(filter, 'UTF-8'),
            "scriptFile",
            scriptFile
        )
        assert scriptRunResponse.successful
        def runScriptJson = client.jsonValue(scriptRunResponse.body(), Map)
        String newExecId = runScriptJson.execution.id

        and: "wait for it to complete"
        def exec = JobUtils.waitForSuccess(
            newExecId,
            client,
            WaitingTime.EXCESSIVE
        )

        then: "verify the execution succeeded despite DOS/Windows line endings"
        exec.status == ExecutionStatus.SUCCEEDED.state

        and: "fetch the output to verify it worked"
        def output = get("/execution/${newExecId}/output", ExecutionOutput)
        def outputLines = output.entries.findAll { it.log }.collect { it.log }

        and: "verify the output contains expected script output"
        outputLines.find { it.contains("This is test-dispatch-script-dos.sh") }
        outputLines.find { it.contains("On node ssh-node ssh-node") }
        outputLines.find { it.contains("With tags:") }
        outputLines.find { it.equals("With args: ") }
    }

    def "execute script with UTF-8 character encoding"() {
        given: "A project that uses storage key for SSH authentication"
        // Note: Setup of this project with storage key nodes happens in setupSpec()

        and: "a script with UTF-8 encoded characters"
        def scriptPath = getClass().getClassLoader().getResource("test-files/test-dispatch-script-utf8.sh").getPath()
        File scriptFile = new File(scriptPath)
        scriptFile.setExecutable(true)

        when: "we execute the UTF-8 encoded script on the remote node"
        def filter = "name:ssh-node"
        def scriptRunResponse = client.doPostWithFormData(
            "/project/${TEST_PROJECT}/run/script?filter=" + URLEncoder.encode(filter, 'UTF-8'),
            "scriptFile",
            scriptFile
        )
        assert scriptRunResponse.successful
        def runScriptJson = client.jsonValue(scriptRunResponse.body(), Map)
        String newExecId = runScriptJson.execution.id

        and: "wait for it to complete"
        def exec = JobUtils.waitForSuccess(
            newExecId,
            client,
            WaitingTime.EXCESSIVE
        )

        then: "verify the execution succeeded with UTF-8 encoded content"
        exec.status == ExecutionStatus.SUCCEEDED.state

        and: "fetch the output to verify it worked"
        def output = get("/execution/${newExecId}/output", ExecutionOutput)
        def outputLines = output.entries.findAll { it.log }.collect { it.log }

        and: "verify the output contains expected UTF-8 encoded text"
        outputLines.find { it.contains("This is test-dispatch-script-utf8.sh") }
        outputLines.find { it.contains("UTF-8 Text: 你好") }
    }

    def "execute script with node environment variable access"() {
        given: "A project that uses storage key for SSH authentication"
        // Note: Setup of this project with storage key nodes happens in setupSpec()

        and: "a script that accesses node environment variables"
        def scriptPath = getClass().getClassLoader().getResource("test-files/test-dispatch-script-env.sh").getPath()
        File scriptFile = new File(scriptPath)
        scriptFile.setExecutable(true)

        when: "we execute the script on the remote node"
        def filter = "name:ssh-node"
        def scriptRunResponse = client.doPostWithFormData(
            "/project/${TEST_PROJECT}/run/script?filter=" + URLEncoder.encode(filter, 'UTF-8'),
            "scriptFile",
            scriptFile
        )
        assert scriptRunResponse.successful
        def runScriptJson = client.jsonValue(scriptRunResponse.body(), Map)
        String newExecId = runScriptJson.execution.id

        and: "wait for it to complete"
        def exec = JobUtils.waitForSuccess(
            newExecId,
            client,
            WaitingTime.EXCESSIVE
        )

        then: "verify the execution succeeded"
        exec.status == ExecutionStatus.SUCCEEDED.state

        and: "fetch the output to verify node environment variables were accessible"
        def output = get("/execution/${newExecId}/output", ExecutionOutput)
        def outputLines = output.entries.findAll { it.log }.collect { it.log }

        and: "verify the script could access node environment variables like RD_NODE_NAME and RD_NODE_TAGS"
        outputLines.find { it.equals("On node ssh-node") }
        outputLines.find { it.contains("With tags:") && it.contains("executor-test") }
    }
}