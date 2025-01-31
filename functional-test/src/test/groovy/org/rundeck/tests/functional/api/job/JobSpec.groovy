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

            waitForExecutionFinish(execution.id)
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

    def "Run job with multiselect all selected option"(){
        given:
        def r = JobUtils.executeJobWithOptions('213b0213-cb72-417e-9707-1371eb44dfe3', client, ["options":[:]])
        assert r.successful
        Execution execution = MAPPER.readValue(r.body().string(), Execution.class)

        waitForExecutionFinish(execution.id)
        String fullLog = JobUtils.getExecutionOutputText(execution.id as String, client)
        expect:
        fullLog.contains('var1,var2,var3,var4')
    }

    def "Run job with multiselect all selected option with custom value"(){
        given:
        def r = JobUtils.executeJobWithOptions('213b0213-cb72-417e-9707-1371eb44dfe3', client, ["options":['test':'var2']])
        assert r.successful
        Execution execution = MAPPER.readValue(r.body().string(), Execution.class)

        waitForExecutionFinish(execution.id)
        String fullLog = JobUtils.getExecutionOutputText(execution.id as String, client)
        expect:
        fullLog.contains('var2')
    }

    def "Create the same job twice fails"() {
        given:
        def jobName = UUID.randomUUID().toString()

        def path = JobUtils.updateJobFileToImport("job-template-common-2.xml",
                PROJECT_NAME,
                ["job-name": jobName,
                "uuid": jobName])

        // Create a job
        JobUtils.createJob(PROJECT_NAME, new File(path).text, client)

        when:
        // Create the same job again
        JobUtils.createJob(PROJECT_NAME, new File(path).text, client)

        then:
        def e = thrown(IllegalArgumentException)
        assert e.message.contains("Some jobs failed on import")
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
        def jr = JobUtils.createJob(PROJECT_NAME, new File(path).text, client)

        then:
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
        def jr = JobUtils.createJob(PROJECT_NAME, new File(path).text, client)

        then:
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
        def jr = JobUtils.createJob(PROJECT_NAME, new File(path).text, client)

        then:
        def jobDetails = JobUtils.getJobDetailsById(jr.getSucceeded().get(0).id, MAPPER, client)
        !jobDetails.executionEnabled
    }
}
