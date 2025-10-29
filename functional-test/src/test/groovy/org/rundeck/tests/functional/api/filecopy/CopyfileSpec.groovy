package org.rundeck.tests.functional.api.filecopy

import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.responses.execution.ExecutionOutput
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer

/**
 * Test copyfile plugin behavior
 */
@APITest
class CopyFileSpec extends BaseContainer {
    public static final String TEST_PROJECT = "copyfile-test"
    public static final String TEST_SSH_ARCHIVE_DIR = "/projects-import/$TEST_PROJECT"
    public static final String TEST_NODE = "ssh-node"
    public static final String SERVER_FILES_PATH = "/home/rundeck/copyfile-test-files/files1"
    public static final String NODE_KEY_PASSPHRASE = "testpassphrase123"
    public static final String NODE_USER_PASSWORD = "testpassword123"
    public static final String USER_VAULT_PASSWORD = "vault123"
    public static final List<String> JOB_RESOURCE_FILES = [
        "test-files/copyfile-test-jobs/test-copyfile-job1.yaml",
        "test-files/copyfile-test-jobs/test-copyfile-job2.yaml",
        "test-files/copyfile-test-jobs/test-copyfile-job3.yaml",
    ]


    def setupSpec() {
        setupProject(TEST_PROJECT)
        String keyPath = getClass().getClassLoader().getResource("docker/compose/oss").getPath() + "/keys"

        loadKeysForNodes(keyPath, TEST_PROJECT, NODE_KEY_PASSPHRASE, NODE_USER_PASSWORD, USER_VAULT_PASSWORD)

        setupProjectArchiveDirectory(
            TEST_PROJECT,
            new File(getClass().getResource(TEST_SSH_ARCHIVE_DIR).getPath()),
            [
                "importConfig"      : "true",
                "importACL"         : "true",
                "importNodesSources": "true",
                "jobUuidOption"     : "preserve"
            ]
        )
        JOB_RESOURCE_FILES.each {
            importJobDefinition(it)
        }
        // Wait for node to be available
        waitingResourceEnabled(TEST_PROJECT, TEST_NODE)
    }

    /**
     * Helper method to import a job definition YAML file
     */
    private void importJobDefinition(String yamlFilePath) {
        def jobFile = getClass().getClassLoader().getResource(yamlFilePath).getPath()
        def importResponse = JobUtils.jobImportYamlFile(TEST_PROJECT, jobFile, client)
        assert importResponse.succeeded.size()>0
    }

    /**
     * Test recursive directory copying to remote nodes
     */
    def "test recursive directory copy to remote nodes"() {
        given: "A job for copying files recursively"
            def jobId = "3b09625a-8371-4d6c-9c04-9e8e90084547"

        and: "source and destination directories"
            def destDir = "/tmp/test-files1-${System.currentTimeMillis()}"

        when: "we run the job to copy files recursively"
            def jobConfig = [
                "sourcedir": SERVER_FILES_PATH,
                "destdir"  : destDir.toString()
            ]
            def json = client.logNextRequest().post("/job/${jobId}/run", [options: jobConfig], Map)

        then: "the job execution should be successful"
            verifyAll {
                json.id!=null
            }

        when: "we wait for the execution to succeed"
            def exec = JobUtils.waitForSuccess(
                json.id as String,
                client,
                WaitingTime.EXCESSIVE
            )

            def output1 = get("/execution/${json.id}/output", ExecutionOutput)
            println "Execution Output:\n" + output1.entries.collect { it.log }.join("\n")


        and: "we run a command to list the files on the remote node"
            def commandExec = client.post(
                "/project/${TEST_PROJECT}/run/command?"+ urlParams([filter:TEST_NODE, exec:"find ${destDir} | sort"]),
                null, Map
            )
            def cmdExecId = commandExec.execution.id

            def cmdExec = JobUtils.waitForSuccess(
                cmdExecId as String,
                client,
                WaitingTime.EXCESSIVE
            )

        and: "fetch the command output"
            def output = get("/execution/${cmdExecId}/output", ExecutionOutput)
            def outputLines = output.entries.findAll { it.log }.collect { it.log }

        then: "verify all expected files were copied"
            verifyAll {
                outputLines.contains(destDir.toString())
                outputLines.contains("${destDir}/afile.txt".toString())
                outputLines.contains("${destDir}/btest.txt".toString())
                outputLines.contains("${destDir}/files2".toString())
                outputLines.contains("${destDir}/files2/cfile.xml".toString())
            }

        cleanup: "we remove the temporary directory"
            client.post(
                "/project/${TEST_PROJECT}/run/command?"+ urlParams([filter: TEST_NODE, exec: "rm -rf ${destDir}"]),
                null, Map
            )
    }

    /**
     * Test file copying with simple pattern matching (*.txt)
     */
    def "test file copy with simple pattern matching"() {
        given: "A job for copying files with simple pattern matching"
            def jobId = "3b09625a-8371-4d6c-9c04-9e8e90084548"

        and: "source and destination directories with pattern"
            def destDir = "/tmp/test-files2-${System.currentTimeMillis()}"
            def pattern = "*.txt"

        when: "we run the job to copy files matching a simple pattern"
            def jobConfig = [
                "sourcedir": SERVER_FILES_PATH,
                "destdir"  : destDir.toString(),
                "pattern"  : pattern
            ]

            def json = post("/job/${jobId}/run", [options: jobConfig], Map)

        then: "the job execution should be successful"
            verifyAll {
                json.id!=null
            }

        when: "we wait for the execution to succeed"
            def exec = JobUtils.waitForSuccess(
                json.id as String,
                client,
                WaitingTime.EXCESSIVE
            )
            def output1 = get("/execution/${json.id}/output", ExecutionOutput)
            println "Execution Output:\n" + output1.entries.collect { it.log }.join("\n")


        and: "we run a command to list the files on the remote node"
            def commandExec = client.post(
                "/project/${TEST_PROJECT}/run/command?"+ urlParams([filter:TEST_NODE, exec:"find ${destDir} | sort"]),
                null, Map
            )
            def cmdExecId = commandExec.execution.id

            def cmdExec = JobUtils.waitForSuccess(
                cmdExecId as String,
                client,
                WaitingTime.EXCESSIVE
            )

        and: "fetch the command output"
            def output = get("/execution/${cmdExecId}/output", ExecutionOutput)
            def outputLines = output.entries.findAll { it.log }.collect { it.log }

        then: "verify only text files were copied and subdirectories were not copied"
            verifyAll{
                outputLines.contains(destDir.toString())
                outputLines.contains("${destDir}/afile.txt".toString())
                outputLines.contains("${destDir}/btest.txt".toString())
                !outputLines.contains("${destDir}/files2".toString())
                !outputLines.contains("${destDir}/files2/cfile.xml".toString())
            }

        cleanup: "we remove the temporary directory"
            client.post(
                "/project/${TEST_PROJECT}/run/command?"+ urlParams([filter: TEST_NODE, exec: "rm -rf ${destDir}"]),
                null, Map
            )
    }

    /**
     * Test file copying with recursive pattern matching (**&#47;*file.*)
     */
    def "test file copy with recursive pattern matching"() {
        given: "A job for copying files with recursive pattern matching"
            def jobId = "3b09625a-8371-4d6c-9c04-9e8e90084549"

        and: "source and destination directories with recursive pattern"
            def destDir = "/tmp/test-files3-${System.currentTimeMillis()}"
            def pattern = "**/*file.*"

        when: "we run the job to copy files matching a recursive pattern"
            def jobConfig = [
                "sourcedir": SERVER_FILES_PATH,
                "destdir"  : destDir.toString(),
                "pattern"  : pattern
            ]

            def json = post("/job/${jobId}/run", [options: jobConfig], Map)

        then: "the job execution should be successful"
            verifyAll {
                json.id!=null
            }

        when: "we wait for the execution to succeed"
            def exec = JobUtils.waitForSuccess(
                json.id as String,
                client,
                WaitingTime.EXCESSIVE
            )
            def output1 = get("/execution/${json.id}/output", ExecutionOutput)
            println "Execution Output:\n" + output1.entries.collect { it.log }.join("\n")


        and: "we run a command to list the files on the remote node"
            def commandExec = client.post(
                "/project/${TEST_PROJECT}/run/command?"+ urlParams([filter:TEST_NODE, exec:"find ${destDir} | sort"]),
                null, Map
            )
            def cmdExecId = commandExec.execution.id

            def cmdExec = JobUtils.waitForSuccess(
                cmdExecId as String,
                client,
                WaitingTime.EXCESSIVE
            )

        and: "fetch the command output"
            def output = get("/execution/${cmdExecId}/output", ExecutionOutput)
            def outputLines = output.entries.findAll { it.log }.collect { it.log }

        then: "verify only files matching the pattern were copied in all directories"
            verifyAll {
                outputLines.contains(destDir.toString())
                outputLines.contains("${destDir}/afile.txt".toString())
                outputLines.contains("${destDir}/files2".toString())
                outputLines.contains("${destDir}/files2/cfile.xml".toString())
                !outputLines.contains("${destDir}/btest.txt".toString()) // doesn't match the pattern}
            }
        cleanup: "we remove the temporary directory"
            client.post(
                "/project/${TEST_PROJECT}/run/command?"+ urlParams([filter: TEST_NODE, exec: "rm -rf ${destDir}"]),
                null, Map
            )
    }

    /**
     * Test directory copying with pattern matching (**&#47;files2)
     */
    def "test directory copy with pattern matching"() {
        given: "A job for copying directories with pattern matching"
            def jobId = "3b09625a-8371-4d6c-9c04-9e8e90084549"

        and: "source and destination directories with directory pattern"
            def destDir = "/tmp/test-files4-${System.currentTimeMillis()}"
            def pattern = "**/files2"

        when: "we run the job to copy directories matching a pattern"
            def jobConfig = [
                "sourcedir": SERVER_FILES_PATH,
                "destdir"  : destDir.toString(),
                "pattern"  : pattern
            ]
            def json = post("/job/${jobId}/run", [options: jobConfig], Map)

        then: "the job execution should be successful"
            verifyAll {
                json.id!=null
            }

        when: "we wait for the execution to succeed"
            def exec = JobUtils.waitForSuccess(
                json.id as String,
                client,
                WaitingTime.EXCESSIVE
            )
            def output1 = get("/execution/${json.id}/output", ExecutionOutput)
            println "Execution Output:\n" + output1.entries.collect { it.log }.join("\n")


        and: "we run a command to list the files on the remote node"
            def commandExec = client.post(
                "/project/$TEST_PROJECT/run/command?"+ urlParams([filter: TEST_NODE, exec: "find ${destDir} | sort"]),
                null, Map
            )
            def cmdExecId = commandExec.execution.id

            def cmdExec = JobUtils.waitForSuccess(
                cmdExecId as String,
                client,
                WaitingTime.EXCESSIVE
            )

        and: "fetch the command output"
            def output = get("/execution/${cmdExecId}/output", ExecutionOutput)
            def outputLines = output.entries.findAll { it.log }.collect { it.log }

        then: "verify only the matched directory and its contents were copied"
            verifyAll{
                outputLines.contains(destDir.toString())
                outputLines.contains("${destDir}/files2".toString())
                outputLines.contains("${destDir}/files2/cfile.xml".toString())
                !outputLines.contains("${destDir}/afile.txt".toString())
                !outputLines.contains("${destDir}/btest.txt".toString())
            }

        cleanup: "we remove the temporary directory"
            client.post(
                "/project/${TEST_PROJECT}/run/command?"+ urlParams([filter:TEST_NODE, exec:"rm -rf ${destDir}"]),
                null, Map
            )
    }
}
