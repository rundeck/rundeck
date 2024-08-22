package org.rundeck.tests.functional.api.job

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer
import org.rundeck.util.api.responses.jobs.CreateJobResponse
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.common.jobs.JobUtils
import com.fasterxml.jackson.databind.ObjectMapper

@APITest
class JobSpec extends BaseContainer {

    static final String PROJECT_NAME = "TestJobs"
    private static final def MAPPER = new ObjectMapper()

    def setupSpec() {
        startEnvironment()
        setupProjectArchiveDirectoryResource(PROJECT_NAME, "/projects-import/TestJobs")
    }

    def "Runs workflow steps"() {
        given:
            def r = JobUtils.executeJobWithOptions('9b43e4ab-7ff2-4159-9fc7-7437901914f7', client, ["options":["opt2": "a"]])
            assert r.successful
            Execution execution = MAPPER.readValue(r.body().string(), Execution.class)

            waitForExecutionStatus(execution.id as String)
            String fullLog = JobUtils.getExecutionOutputText(execution.id as String, client)
        expect:
        [
                'hello there',
                'option opt1: testvalue',
                'option opt1: testvalue',
                'option opt2: a',
                'this is script 2, opt1 is testvalue',
                'hello there',
                'this is script 1, opt1 is testvalue',
        ].every({fullLog.contains(it)})
    }

    def "Create a job with multiple steps"() {
        given:
        def jobName = UUID.randomUUID().toString()

        def path = JobUtils.updateJobFileToImport("job-template-common-2.xml",
                PROJECT_NAME,
                ["job-name": jobName,
                 "args": "echo 0",
                 "2-args": "echo 1"])

        when:
        def response = JobUtils.createJob(PROJECT_NAME, new File(path).text, client)

        then:
        response.successful
        def jr = MAPPER.readValue(response.body().string(), CreateJobResponse.class)
        def jobDetails = JobUtils.getJobDetailsById(jr.getSucceeded().get(0).id, MAPPER, client)
        jobDetails.sequence.commands[0].exec == "echo 0"
        jobDetails.sequence.commands[1].exec == "echo 1"
    }

    def "Create a job with a job schedule disabled"() {
        given:
        def jobName = UUID.randomUUID().toString()

        def path = JobUtils.updateJobFileToImport("job-template-common.xml",
                PROJECT_NAME,
                ["job-name": jobName,
                 "schedule-enabled": "false"])

        when:
        def response = JobUtils.createJob(PROJECT_NAME, new File(path).text, client)

        then:
        response.successful
        def jr = MAPPER.readValue(response.body().string(), CreateJobResponse.class)
        def jobDetails = JobUtils.getJobDetailsById(jr.getSucceeded().get(0).id, MAPPER, client)
        !jobDetails.scheduleEnabled
    }

    def "Create a job with a job execution disabled"() {
        given:
        def jobName = UUID.randomUUID().toString()

        def path = JobUtils.updateJobFileToImport("job-template-common.xml",
                PROJECT_NAME,
                ["job-name": jobName,
                 "execution-enabled": "false"])

        when:
        def response = JobUtils.createJob(PROJECT_NAME, new File(path).text, client)

        then:
        response.successful
        def jr = MAPPER.readValue(response.body().string(), CreateJobResponse.class)
        def jobDetails = JobUtils.getJobDetailsById(jr.getSucceeded().get(0).id, MAPPER, client)
        !jobDetails.executionEnabled
    }
}
