package org.rundeck.tests.functional.api.job

import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.api.storage.KeyStorageApiClient
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer

@APITest
class JobExecSudoSpec extends BaseContainer{

    public static final String TEST_SSH_ARCHIVE_DIR = "/projects-import/core-sshj-executor-test"
    public static final String SUDO_JOB_UUID = "fbf165bd-b5d9-44a7-98b9-fb8aa273ff9d"
    public static final String PROJECT_NAME = "core-sshj-executor-test"
    public static final String NODE_USER_PASSWORD = "testpassword123"

    def setupSpec() {
        setupProjectArchiveDirectoryResource(
                PROJECT_NAME,
                TEST_SSH_ARCHIVE_DIR)
        KeyStorageApiClient keyStorageApiClient = new KeyStorageApiClient(clientProvider)
        keyStorageApiClient.callUploadKey("project/$PROJECT_NAME/ssh-node.pass", "password", NODE_USER_PASSWORD)
    }

    def cleanupSpec(){
        deleteProject(PROJECT_NAME)
    }

    /**
     * This test expects the job to succeed since it has pty enable
     */
    def "SSHJ sudo command with PTY"(){
        setup:
        addExtraProjectConfig(PROJECT_NAME, [
                "project.always-set-pty":"true"
        ])
        when:
        def jobExecResponse = JobUtils.executeJob(SUDO_JOB_UUID, client)
        assert jobExecResponse.successful
        Execution exec = MAPPER.readValue(jobExecResponse.body().string(), Execution.class)
        then:
        JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                exec.id,
                client,
                WaitingTime.EXCESSIVE
        )
    }

    /**
     * This test expects the job to fail with time out since the pty is not enabled
     * and rundeck should get stuck waiting for the sudo password
     */
    def "SSHJ sudo command without PTY"(){
        setup:
        addExtraProjectConfig(PROJECT_NAME, [
                "project.always-set-pty":"false"
        ])
        when:
        def jobExecResponse = JobUtils.executeJob(SUDO_JOB_UUID, client)
        assert jobExecResponse.successful
        Execution exec = MAPPER.readValue(jobExecResponse.body().string(), Execution.class)
        then:
        JobUtils.waitForExecution(
                ExecutionStatus.TIMEDOUT.state,
                exec.id,
                client,
                WaitingTime.EXCESSIVE
        )
    }
}
