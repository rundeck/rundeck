package org.rundeck.tests.functional.api.workflowSteps

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.annotations.APITest
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer

/**
 * Test script and command steps should produce the correct exitCode into the output context
 */
@APITest
class StepExecutionContextSpec extends BaseContainer {
    public static final String TEST_PROJECT = "test-step-execution-context"
    public static final String TEST_ARCHIVE_DIR = "/projects-import/step-execution-context"

    def setupSpec() {
        startEnvironment()
        setupProjectArchiveDirectory(
            TEST_PROJECT,
            new File(getClass().getResource(TEST_ARCHIVE_DIR).getPath()),
            [
                "importJobs"   : "true",
                "jobUuidOption": "preserve"
            ]
        )
    }

    def "test Job with script step should define correct exitCode"() {
        given:
            def jobId = "1266c6ed-5cb7-4736-9e64-7f902f1090fa"
            def jobConfig = [
                "loglevel": "DEBUG"
            ]

        when:
            def json = post("/job/${jobId}/executions", jobConfig, Map)
        then:
            json.id != null
        when:
            def exec = JobUtils.waitForExecutionToBe(
                ExecutionStatus.FAILED.state,
                json.id as String,
                new ObjectMapper(),
                client,
                WaitingTime.MODERATE.milliSeconds,
                WaitingTime.EXCESSIVE.milliSeconds / 1000 as int
            )
        then:
            exec.status == 'failed'
        when:
            String execId = json.id
            def entries = getExecutionOutput(execId)
        then:
            entries.containsAll(
                "STEP 1: will succeed",
                "STEP 2: will fail with code 99",
                "Result: 99",
                "Failed: NonZeroResultCode: Result code was 99",
                "STEP 3: ExitCode from Step1: 0",
                "STEP 4: ExitCode from Step2: 99"
            )
    }
    def "test Job with command step should define correct exitCode"() {
        given:
            def jobId = "aeb9ce8c-c78b-4414-9948-26bfd134303c"
            def jobConfig = [
                "loglevel": "DEBUG"
            ]

        when:
            def json = post("/job/${jobId}/executions", jobConfig, Map)
        then:
            json.id != null
        when:
            def exec = JobUtils.waitForExecutionToBe(
                ExecutionStatus.FAILED.state,
                json.id as String,
                new ObjectMapper(),
                client,
                WaitingTime.MODERATE.milliSeconds,
                WaitingTime.EXCESSIVE.milliSeconds / 1000 as int
            )
        then:
            exec.status == 'failed'
        when:
            String execId = json.id
            def entries = getExecutionOutput(execId)
        then:
            entries.containsAll(
                "STEP 1: will succeed",
                "STEP 2: will fail with code 77",
                "Result: 77",
                "Failed: NonZeroResultCode: Result code was 77",
                "STEP 3: ExitCode from Step1: 0",
                "STEP 4: ExitCode from Step2: 77"
            )
    }
}
