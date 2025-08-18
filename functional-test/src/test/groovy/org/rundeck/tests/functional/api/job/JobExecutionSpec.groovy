package org.rundeck.tests.functional.api.job

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.rundeck.util.api.responses.common.ErrorResponse
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.api.responses.execution.ExecutionOutput
import org.rundeck.util.api.responses.execution.RunCommand
import org.rundeck.util.api.responses.system.SystemInfo
import org.rundeck.util.annotations.APITest
import org.rundeck.util.annotations.ExcludePro
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.FileHelpers
import org.rundeck.util.common.execution.ExecutionUtils
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.container.BaseContainer
import spock.lang.Shared
import spock.lang.Stepwise

import java.nio.file.Files
import java.time.Duration
import java.util.stream.Collectors
import java.nio.file.Paths
import java.text.SimpleDateFormat

@APITest
@Stepwise
class JobExecutionSpec extends BaseContainer {

    private static final ObjectMapper MAPPER = new ObjectMapper()

    @Shared String jobId
    @Shared String jobId2
    @Shared String jobId3
    @Shared int execId
    @Shared int execId2
    @Shared int execId3

    def setupSpec() {
        startEnvironment()
        setupProject()
        def pathFile = JobUtils.updateJobFileToImport("job-template-common.xml", PROJECT_NAME, ["job-name": "test job", "job-group-name": "test/api/executions", "job-description-name": "Test the /job/ID/executions API endpoint", "args": "echo testing /job/ID/executions result", "uuid": UUID.randomUUID().toString()])
        jobId = JobUtils.jobImportFile(PROJECT_NAME,pathFile,client).succeeded[0].id
        def pathFile2 = JobUtils.updateJobFileToImport("job-template-common.xml", PROJECT_NAME, ["job-name": "test job", "job-group-name": "test/api/executions 2", "job-description-name": "Test the /job/ID/executions API endpoint", "args": "/bin/false this should fail", "uuid": UUID.randomUUID().toString()])
        jobId2 = JobUtils.jobImportFile(PROJECT_NAME,pathFile2,client).succeeded[0].id
        def pathFile3 = JobUtils.updateJobFileToImport("job-template-common.xml", PROJECT_NAME, ["job-name": "test job", "job-group-name": "test/api/executions 3", "job-description-name": "Test the /job/ID/executions API endpoint", "args": "echo this job will be killed...", "2-args": "sleep 240", "uuid": UUID.randomUUID().toString()])
        jobId3 = JobUtils.jobImportFile(PROJECT_NAME,pathFile3,client).succeeded[0].id
    }

    def "job/jobId/executions should succeed with 0 results"() {
        when:
            def response = doGet("/job/${jobId}/executions")
        then:
            verifyAll {
                response.successful
                response.code() == 200
                def json = jsonValue(response.body())
                json.executions.size() == 0
            }
    }

    def "job/id/run should succeed"() {
        when:
            def jobRun = JobUtils.executeJobWithArgs(jobId, client, "-opt2 a")
            execId = jsonValue(jobRun.body()).id as Integer
        then:
            verifyAll {
                execId > 0
            }
    }

    def "job/id/executions should succeed with 1 results"() {
        when:
        List<Execution> execs =  waitFor(ExecutionUtils.Retrievers.executionsForJobId(client, jobId),
                { it.size() == 1 })
        then:
        execs[0].id == execId as String
    }

    def "job/id/executions?status=succeeded should succeed with 1 results"() {
        when:
        List<Execution> execs =  waitFor(ExecutionUtils.Retrievers.executionsForJobId(client, jobId, "status=succeeded"),
                {it.size() == 1 })
        then:
        execs[0].id == execId as String
    }

    def "run again job/id/run should succeed"() {
        when:
            def jobRun = JobUtils.executeJobWithArgs(jobId, client, "-opt2 a")
            execId2 = jsonValue(jobRun.body()).id as Integer
        then:
            verifyAll {
                execId2 > 0
            }
        then:
        assert waitFor(ExecutionUtils.Retrievers.executionsForJobId(client, jobId),
                    { it.size() == 2 })

    }

    def "job/id/executions max param"() {
        when:
            def response = doGet("/job/${jobId}/executions?max=1")
        then:
        verifyAll {
            response.successful
            response.code() == 200
            def json = jsonValue(response.body())
            json.executions.size() == 1
        }
    }

    def "job/id/executions offset param"() {
        when:
            def response = doGet("/job/${jobId}/executions?max=1&offset=1")
        then:
            verifyAll {
                response.successful
                response.code() == 200
                def json = jsonValue(response.body())
                json.executions.size() == 1
            }
    }

    def "job/id/executions arbitrary status param"() {
        when:
            def response = doGet("/job/${jobId}/executions?status=some_status")
        then:
            verifyAll {
                response.successful
                response.code() == 200
                def json = jsonValue(response.body())
                json.executions.size() == 0
            }
    }

    def "job/id/executions invalid id param"() {
        when:
            def response = doGet("/job/fake/executions")
        then:
            verifyAll {
                response.code() == 404
                def json = jsonValue(response.body())
                json.message == "Job ID does not exist: fake"
            }
    }

    def "job/id/executions?status=failed with 1 results"() {
        when:
            def jobRun = JobUtils.executeJobWithArgs(jobId2, client, "-opt2 a")
            execId2 = jsonValue(jobRun.body()).id as Integer
        then:
            verifyAll {
                execId2 > 0
            }
        when:
        List<Execution> execs =  waitFor(ExecutionUtils.Retrievers.executionsForJobId(client, jobId2, "status=failed"),
                { it.size() == 1 })
        then:
        execs[0].id == execId2 as String
    }

    def "job/id/executions?status=running with 1 results"() {
        when:
            def jobRun = JobUtils.executeJobWithArgs(jobId3, client, "-opt2 a")
            execId3 = jsonValue(jobRun.body()).id as Integer
        then:
            def response = doGet("/job/${jobId3}/executions?status=running")
            verifyAll {
                response.successful
                response.code() == 200
                def json = jsonValue(response.body())
                json.executions.size() == 1
                json.executions[0].id == execId3
            }
    }

    def "job/jobid/executions?status=aborted with 1 results"() {
        when:
            doPost("/execution/${execId3}/abort")
        List<Execution> execs =  waitFor(ExecutionUtils.Retrievers.executionsForJobId(client, jobId3, "status=aborted"),
                { it.size() == 1 })
        then:
        execs[0].id == execId3 as String

    }

    def "test-job-long-run"(){
        given:
        def projectName = "test-job-long-run"
        def client = getClient()
        Object projectJsonMap = [
                "name": projectName.toString(),
                "description": "test-job-long-run",
                "config": [
                        "test.property": "test value",
                        "project.execution.history.cleanup.enabled": "true",
                        "project.execution.history.cleanup.retention.days": "1",
                        "project.execution.history.cleanup.batch": "500",
                        "project.execution.history.cleanup.retention.minimum": "0",
                        "project.execution.history.cleanup.schedule": "0 0/1 * 1/1 * ? *"
                ]
        ]

        def responseProject = createSampleProject(projectJsonMap)
        assert responseProject.successful

        def longRunXml = (String project, String stepArgs) -> {
            return "<joblist>\n" +
                    "   <job>\n" +
                    "      <name>Long Run Job</name>\n" +
                    "      <group>api-test/job-run</group>\n" +
                    "      <uuid>db9a5f0d</uuid>\n" +
                    "      <description></description>\n" +
                    "      <loglevel>INFO</loglevel>\n" +
                    "      <context>\n" +
                    "          <project>${project}</project>\n" +
                    "      </context>\n" +
                    "      <dispatch>\n" +
                    "        <threadcount>1</threadcount>\n" +
                    "        <keepgoing>true</keepgoing>\n" +
                    "      </dispatch>\n" +
                    "      <sequence>\n" +
                    "        <command>\n" +
                    "        <exec>${stepArgs}</exec>\n" +
                    "        </command>\n" +
                    "      </sequence>\n" +
                    "   </job>\n" +
                    "   <job>\n" +
                    "      <name>Long Run Wrapper</name>\n" +
                    "      <group>api-test/job-run</group>\n" +
                    "      <uuid>r2d2</uuid>\n" +
                    "      <description/>\n" +
                    "      <executionEnabled>true</executionEnabled>\n" +
                    "      <loglevel>INFO</loglevel>\n" +
                    "       <context>\n" +
                    "          <project>${project}</project>\n" +
                    "      </context>\n" +
                    "      <nodeFilterEditable>false</nodeFilterEditable>\n" +
                    "      <sequence keepgoing=\"false\" strategy=\"parallel\">\n" +
                    "         <command>\n" +
                    "             <jobref name=\"Long Run Job\">\n" +
                    "               <uuid>db9a5f0d</uuid>\n" +
                    "             </jobref>\n" +
                    "         </command>\n" +
                    "         <command>\n" +
                    "             <jobref name=\"Long Run Job\" nodeStep=\"true\">\n" +
                    "               <uuid>db9a5f0d</uuid>\n" +
                    "             </jobref>\n" +
                    "         </command>\n" +
                    "      </sequence>\n" +
                    "   </job>\n" +
                    "</joblist>"
        }

        def longRunJobArgs = "sleep 12" // As the original test states
        def testXml = longRunXml(projectName, longRunJobArgs)
        def jobCreatedResponse = JobUtils.createJob(projectName, testXml, client)

        when: "Job and referenced job created"
        def jobId = jobCreatedResponse.succeeded[0]?.id
        def refJobId = jobCreatedResponse.succeeded[1]?.id

        then:
        jobId != null
        refJobId != null

        when: "run job test"
        def jobRun = JobUtils.executeJob(jobId, client)
        assert jobRun.successful

        Execution exec = MAPPER.readValue(jobRun.body().string(), Execution.class)

        Execution JobExecutionStatus = JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                exec.id as String,
                client,
                WaitingTime.EXCESSIVE
        )

        then:
        JobExecutionStatus.status == ExecutionStatus.SUCCEEDED.state

        when: "run job test"
        def referencedJobRun = JobUtils.executeJob(refJobId, client)
        assert referencedJobRun.successful

        Execution refExec = MAPPER.readValue(referencedJobRun.body().string(), Execution.class)

        Execution refJobExecutionStatus = JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                refExec.id as String,
                client,
                WaitingTime.EXCESSIVE
        )

        then:
        refJobExecutionStatus.status == ExecutionStatus.SUCCEEDED.state

    }

    def "test-job-retry"(){
        given:
        def projectName = "test-job-retry"
        def client = getClient()
        Object projectJsonMap = [
                "name": projectName.toString(),
                "description": "test-job-retry",
                "config": [
                        "test.property": "test value",
                        "project.execution.history.cleanup.enabled": "true",
                        "project.execution.history.cleanup.retention.days": "1",
                        "project.execution.history.cleanup.batch": "500",
                        "project.execution.history.cleanup.retention.minimum": "0",
                        "project.execution.history.cleanup.schedule": "0 0/1 * 1/1 * ? *"
                ]
        ]

        def responseProject = createSampleProject(projectJsonMap)
        assert responseProject.successful

        def jobRetry = (String args) -> {
            return "<joblist>\n" +
                    "   <job>\n" +
                    "      <name>cli job</name>\n" +
                    "      <group>api-test/job-run-timeout-retry</group>\n" +
                    "      <description></description>\n" +
                    "      <loglevel>INFO</loglevel>\n" +
                    "      <retry>2</retry>\n" +
                    "      <dispatch>\n" +
                    "        <threadcount>1</threadcount>\n" +
                    "        <keepgoing>true</keepgoing>\n" +
                    "      </dispatch>\n" +
                    "      <sequence>\n" +
                    "        <command>\n" +
                    "        <exec>${args}</exec>\n" +
                    "        </command>\n" +
                    "      </sequence>\n" +
                    "   </job>\n" +
                    "</joblist>"
        }

        def jobArgs = "echo hello there ; false" // As the original test states
        def testXml = jobRetry(jobArgs)
        def jobCreatedResponse = JobUtils.createJob(projectName, testXml, client)

        when: "Job and referenced job created"
        def jobId = jobCreatedResponse.succeeded[0]?.id

        then:
        jobId != null

        when: "run job test"
        def execArgs = "-opt2 a"
        def jobRun = JobUtils.executeJobWithArgs(jobId, client, execArgs)
        assert jobRun.successful
        Execution parsedExecutionsResponse = MAPPER.readValue(jobRun.body().string(), Execution.class)
        def execId = parsedExecutionsResponse.id

        then:
        !execId.isBlank()

        when: "fail and retry 1"
        def execDetails = JobUtils.waitForExecution(
                ExecutionStatus.FAILED_WITH_RETRY.state,
                execId as String,
                client)

        def retryId1 = execDetails.retriedExecution.id

        then:
        !retryId1.isBlank()

        when: "fail and retry 2"
        def execDetails2 = JobUtils.waitForExecution(
                ExecutionStatus.FAILED_WITH_RETRY.state,
                retryId1 as String,
                client)

        def retryId2 = execDetails2.retriedExecution.id

        then:
        !retryId2.isBlank()

        when: "final retry"
        def execDetailsFinal = JobUtils.waitForExecution(
                ExecutionStatus.FAILED.state,
                retryId2 as String,
                client)

        then:
        execDetailsFinal.retriedExecution == null
        execDetailsFinal.retryAttempt == 2
        execDetailsFinal.status == ExecutionStatus.FAILED.state

    }

    def "test-job-run-GET-405"(){
        given:
        def projectName = "test-job-run-GET-405"
        def client = getClient()
        Object projectJsonMap = [
                "name": projectName.toString(),
                "description": "test-job-run-GET-405",
                "config": [
                        "test.property": "test value",
                        "project.execution.history.cleanup.enabled": "true",
                        "project.execution.history.cleanup.retention.days": "1",
                        "project.execution.history.cleanup.batch": "500",
                        "project.execution.history.cleanup.retention.minimum": "0",
                        "project.execution.history.cleanup.schedule": "0 0/1 * 1/1 * ? *"
                ]
        ]

        def responseProject = createSampleProject(projectJsonMap)
        assert responseProject.successful

        def jobXml = (String project, String args) -> "<joblist>\n" +
                "   <job>\n" +
                "      <name>cli job</name>\n" +
                "      <group>api-test/job-run</group>\n" +
                "      <description></description>\n" +
                "      <loglevel>INFO</loglevel>\n" +
                "      <context>\n" +
                "          <project>${projectName}</project>\n" +
                "          <options>\n" +
                "              <option name=\"opt1\" value=\"testvalue\" required=\"true\"/>\n" +
                "              <option name=\"opt2\" values=\"a,b,c\" required=\"true\"/>\n" +
                "          </options>\n" +
                "      </context>\n" +
                "      <dispatch>\n" +
                "        <threadcount>1</threadcount>\n" +
                "        <keepgoing>true</keepgoing>\n" +
                "      </dispatch>\n" +
                "      <sequence>\n" +
                "        <command>\n" +
                "        <exec>${args}</exec>\n" +
                "        </command>\n" +
                "      </sequence>\n" +
                "   </job>\n" +
                "</joblist>"

        def jobXml1 = jobXml(projectName, "echo hello there")

        def job1CreatedParsedResponse = JobUtils.createJob(projectName, jobXml1, client)
        def job1Id = job1CreatedParsedResponse.succeeded[0]?.id
        def argString = "-opt2+a"

        when: "we do the request api response code is 405 and exec should fail"
        def response = JobUtils.executeJobWithArgsInvalidMethod(job1Id, client, argString)

        then:
        !response.successful
        response.code() == 405

    }

    def "test-job-run-later"(){
        setup:
        def projectName = PROJECT_NAME
        def client = getClient()

        def xmlJob = (String stepArgs) -> {
            return "<joblist>\n" +
                    "   <job>\n" +
                    "      <name>cli job</name>\n" +
                    "      <group>api-test/job-run</group>\n" +
                    "      <description></description>\n" +
                    "      <loglevel>INFO</loglevel>\n" +
                    "      <context>\n" +
                    "          <project>${PROJECT_NAME}</project>\n" +
                    "          <options>\n" +
                    "              <option name=\"opt1\" value=\"testvalue\" required=\"true\"/>\n" +
                    "              <option name=\"opt2\" values=\"a,b,c\" required=\"true\"/>\n" +
                    "          </options>\n" +
                    "      </context>\n" +
                    "      <dispatch>\n" +
                    "        <threadcount>1</threadcount>\n" +
                    "        <keepgoing>true</keepgoing>\n" +
                    "      </dispatch>\n" +
                    "      <sequence>\n" +
                    "        <command>\n" +
                    "        <exec>${stepArgs}</exec>\n" +
                    "        </command>\n" +
                    "      </sequence>\n" +
                    "   </job>\n" +
                    "</joblist>"
        }

        def jobArgs = "echo asd" // As the original test states
        def testXml = xmlJob(jobArgs)
        def jobCreatedResponse = JobUtils.createJob(projectName, testXml, client)
        def jobId = jobCreatedResponse.succeeded[0]?.id

        when: "TEST: POST job/id/run should succeed with future time"
        def runtime = generateRuntime(6)
        def runLaterExecResponse = JobUtils.executeJobLaterWithArgsAndRuntime(
                jobId,
                client,
                "-opt2+a",
                runtime.string
        )
        assert runLaterExecResponse.successful
        Execution parsedExec = MAPPER.readValue(runLaterExecResponse.body().string(), Execution.class)
        String execId = parsedExec.id
        def dateS = parsedExec.dateStarted.date

        then:
        execId != null
        dateS.toString() == runtime.date.toString()

        when: "Wait until execution succeeds"
        def execAfterWait = JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                execId,
                client,
                WaitingTime.EXCESSIVE
        )

        then: "OK"
        execAfterWait.status == ExecutionStatus.SUCCEEDED.state

        when: "We call the execution to be ran later, wrong method, gets 405"
        def runtime1 = generateRuntime(25)
        def runLaterExecResponse1 = JobUtils.executeJobLaterWithArgsInvalidMethod(
                jobId,
                client,
                "-opt2+a",
                runtime1.string
        )

        then: "OK"
        !runLaterExecResponse1.successful
        runLaterExecResponse1.code() == 405

        when: "TEST: POST job/id/run with scheduled time in the past should fail"
        def runtime2 = generateRuntime(-1000) // Generates a past runtime string
        def runLaterExecResponse2 = JobUtils.executeJobLaterWithArgsAndRuntime(
                jobId,
                client,
                "-opt2+a",
                runtime2.string
        )

        then: "OK"
        !runLaterExecResponse2.successful

        when: "TEST: POST job/id/run with invalid schedule time"
        def invalidRuntime = "1999/01/01 11:10:01.000+0000"
        def runLaterExecResponse3 = JobUtils.executeJobLaterWithArgsAndRuntime(
                jobId,
                client,
                "-opt2+a",
                invalidRuntime
        )

        then: "OK"
        !runLaterExecResponse3.successful

    }

    def "test-job-run"(){
        setup:
        def projectName = PROJECT_NAME
        def client = getClient()

        def xmlJob = (String stepArgs) -> {
            return "<joblist>\n" +
                    "   <job>\n" +
                    "      <name>cli job</name>\n" +
                    "      <group>api-test/job-run</group>\n" +
                    "      <description></description>\n" +
                    "      <loglevel>INFO</loglevel>\n" +
                    "      <context>\n" +
                    "          <project>${PROJECT_NAME}</project>\n" +
                    "          <options>\n" +
                    "              <option name=\"opt1\" value=\"testvalue\" required=\"true\"/>\n" +
                    "              <option name=\"opt2\" values=\"a,b,c\" required=\"true\"/>\n" +
                    "          </options>\n" +
                    "      </context>\n" +
                    "      <dispatch>\n" +
                    "        <threadcount>1</threadcount>\n" +
                    "        <keepgoing>true</keepgoing>\n" +
                    "      </dispatch>\n" +
                    "      <sequence>\n" +
                    "        <command>\n" +
                    "        <exec>${stepArgs}</exec>\n" +
                    "        </command>\n" +
                    "      </sequence>\n" +
                    "   </job>\n" +
                    "   <job>\n" +
                    "      <name>cli job2</name>\n" +
                    "      <group>api-test/job-run</group>\n" +
                    "      <description></description>\n" +
                    "      <loglevel>INFO</loglevel>\n" +
                    "      <dispatch>\n" +
                    "        <threadcount>1</threadcount>\n" +
                    "        <keepgoing>true</keepgoing>\n" +
                    "      </dispatch>\n" +
                    "      <nodefilters>\n" +
                    "        <filter>.*</filter>\n" +
                    "      </nodefilters>\n" +
                    "      <nodesSelectedByDefault>false</nodesSelectedByDefault>\n" +
                    "      <sequence>\n" +
                    "        <command>\n" +
                    "        <exec>${stepArgs}</exec>\n" +
                    "        </command>\n" +
                    "      </sequence>\n" +
                    "   </job>\n" +
                    "</joblist>"
        }

        def jobArgs = "echo asd" // As the original test states
        def testXml = xmlJob(jobArgs)
        def jobCreatedResponse = JobUtils.createJob(projectName, testXml, client)
        def jobId1 = jobCreatedResponse.succeeded[0]?.id
        def jobId2 = jobCreatedResponse.succeeded[1]?.id

        when: "TEST: POST job/id/run should succeed"
        def optionA = 'a'
        Object optionsToMap = [
                "options": [
                        opt2: optionA
                ]
        ]
        def response = JobUtils.executeJobWithOptions(jobId1, client, optionsToMap)
        assert response.successful

        Execution execRes = MAPPER.readValue(response.body().string(), Execution.class)
        String execId = execRes.id

        def execution = JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                execId,
                client)

        then:
        execution.status == ExecutionStatus.SUCCEEDED.state

        when: "TEST: POST job/id/run should fail"
        def response2 = JobUtils.executeJob(jobId2, client)
        assert response2.successful

        Execution execRes2 = MAPPER.readValue(response2.body().string(), Execution.class)
        String execId2 = execRes2.id
        def execution2 = JobUtils.waitForExecution(
                ExecutionStatus.FAILED.state,
                execId2,
                client)

        then:
        execution2.status == ExecutionStatus.FAILED.state // should fail

        when: "TEST: POST job/id/run should succeed w/ filter"
        Object filter = [
                "filter": "name: .*"
        ]
        def response3 = JobUtils.executeJobWithOptions(jobId2, client, filter)
        assert response3.successful

        Execution execRes3 = MAPPER.readValue(response3.body().string(), Execution.class)
        String execId3 = execRes3.id
        def execution3 = JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                execId3,
                client)

        then:
        execution3.status == ExecutionStatus.SUCCEEDED.state

        when: "TEST: POST job/id/run with JSON"
        Object optionsJson = [
                "options": [
                        opt1:"xyz",
                        opt2:"def"
                ]
        ]
        def response4 = JobUtils.executeJobWithOptions(jobId1, client, optionsJson)
        assert response4.successful

        Execution execRes4 = MAPPER.readValue(response4.body().string(), Execution.class)
        String execId4 = execRes4.id
        def execution4 = JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                execId4,
                client)

        then:
        execution4.status == ExecutionStatus.SUCCEEDED.state
        execution4.argstring == "-opt1 xyz -opt2 def"
        execution4.job.options?.opt1 == "xyz"
        execution4.job.options?.opt2 == "def"

        when: "TEST: GET job/id/run should fail 405"
        def response5 = JobUtils.executeJobWithArgsInvalidMethod(jobId1, client, "-opt+a")
        assert !response5.successful

        then:
        response5.code() == 405

        when: "TEST: POST job/id/run without required opt should fail"
        def response6 = JobUtils.executeJob(jobId1, client)
        assert !response6.successful
        ErrorResponse error = ErrorResponse.fromJson(response6.body().string())

        then:
        response6.code() == 400
        error.message == "Job options were not valid: Option 'opt2' is required."

    }

    @ExcludePro
    def "test-job-run-webhook"(){
        given:
        def projectName = "test-send-notification-webhook"
        def client = getClient()

        setupProjectArchiveDirectoryResource(projectName, '/projects-import/webhook-notification-project')

        // We have the jobs id, since they are already imported
        def openNcJobId = "c81aa8af-1e0e-4fce-a7bd-102b87922ef2"
        def notificationJobId = "a20106e4-37e6-489b-a783-2beb04a367c1"
        def readNcOutputJobId = "ccda2e41-277a-4c62-ba01-8f930663561e"

        when: "We first open NC connection"
        def openNcJobRun = JobUtils.executeJob(openNcJobId, client)
        assert openNcJobRun.successful
        Execution openNcJobResponse = MAPPER.readValue(openNcJobRun.body().string(), Execution.class)

        then: "We wait for succeeded exec"
        JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                openNcJobResponse.id as String,
                client,
                WaitingTime.EXCESSIVE
        ).status == ExecutionStatus.SUCCEEDED.state

        when: "We run the job with notification"
        def jobRun = JobUtils.executeJob(notificationJobId, client)
        assert jobRun.successful
        Execution parsedExecutionsResponse = MAPPER.readValue(jobRun.body().string(), Execution.class)

        then: "Will succeed"
        JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                parsedExecutionsResponse.id as String,
                client,
                WaitingTime.EXCESSIVE
        ).status == ExecutionStatus.SUCCEEDED.state

        when: "We wait 10 seconds for netcat to deliver response"
        Thread.sleep(Duration.ofSeconds(10).toMillis())

        // Then run the job that reads the output of request
        def readJobRun = JobUtils.executeJob(readNcOutputJobId, client)
        assert readJobRun.successful
        Execution readJobRunResponse = MAPPER.readValue(readJobRun.body().string(), Execution.class)
        def readJobSucceeded = JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                readJobRunResponse.id as String,
                client)

        assert readJobSucceeded.status == ExecutionStatus.SUCCEEDED.state
        def execOutputResponse = client.doGetAcceptAll("/execution/$readJobRunResponse.id/output")
        ExecutionOutput execOutput = MAPPER.readValue(execOutputResponse.body().string(), ExecutionOutput.class)

        def webhookData = [
            "POST /test?id=$parsedExecutionsResponse.id&status=succeeded HTTP/1.1".toString(),
            "X-RunDeck-Notification-Execution-Status: succeeded",
            "X-RunDeck-Notification-Execution-ID: $parsedExecutionsResponse.id".toString(),
            "X-RunDeck-Notification-Trigger: success",
            "Content-Type: text/xml; charset=UTF-8",
            "Host: localhost:9001".toString(),
            "<notification trigger='success' status='succeeded' executionId='$parsedExecutionsResponse.id'>".toString(),
        ]
        def entries = execOutput.entries.stream().map {it.log}.collect(Collectors.toList())

        then: "The output of the job must have basic info about the webhook"
        execOutput != null
        FileHelpers.assertLinesInsideEntries(webhookData, entries)

        cleanup:
        deleteProject(projectName)
        Files.deleteIfExists(Paths.get("/tmp/netcat-out.txt"))
    }

    @ExcludePro
    def "test-job-run-without-deadlock"(){
        setup:
        def projectName = PROJECT_NAME
        def client = getClient()

        def xmlJob = "<joblist>\n" +
                    "  <job>\n" +
                    "    <context>\n" +
                    "      <options preserveOrder='true'>\n" +
                    "        <option name='maxWaitTimeSecs' value='0' />\n" +
                    "      </options>\n" +
                    "    </context>\n" +
                    "    <defaultTab>summary</defaultTab>\n" +
                    "    <description></description>\n" +
                    "    <executionEnabled>true</executionEnabled>\n" +
                    "    <id>3cce5f70-71aa-4e6c-b99e-9e866732448a</id>\n" +
                    "    <loglevel>INFO</loglevel>\n" +
                    "    <multipleExecutions>true</multipleExecutions>\n" +
                    "    <name>job_c</name>\n" +
                    "    <nodeFilterEditable>false</nodeFilterEditable>\n" +
                    "    <scheduleEnabled>true</scheduleEnabled>\n" +
                    "    <sequence keepgoing='false' strategy='node-first'>\n" +
                    "      <command>\n" +
                    "        <script><![CDATA[sleep @option.maxWaitTimeSecs@]]></script>\n" +
                    "        <scriptargs />\n" +
                    "      </command>\n" +
                    "      <command>\n" +
                    "        <exec>echo \"regular job before parallel\"</exec>\n" +
                    "      </command>\n" +
                    "    </sequence>\n" +
                    "    <uuid>3cce5f70-71aa-4e6c-b99e-9e866732448a</uuid>\n" +
                    "  </job>\n" +
                    "  <job>\n" +
                    "    <defaultTab>summary</defaultTab>\n" +
                    "    <description></description>\n" +
                    "    <executionEnabled>true</executionEnabled>\n" +
                    "    <id>7d6d0958-7987-4a35-9ec3-7720f0985ae4</id>\n" +
                    "    <loglevel>INFO</loglevel>\n" +
                    "    <multipleExecutions>true</multipleExecutions>\n" +
                    "    <name>job_d</name>\n" +
                    "    <nodeFilterEditable>false</nodeFilterEditable>\n" +
                    "    <scheduleEnabled>true</scheduleEnabled>\n" +
                    "    <sequence keepgoing='false' strategy='parallel'>\n" +
                    "      <command>\n" +
                    "        <jobref name='job_c' nodeStep='true'>\n" +
                    "          <arg line='-maxWaitTimeSecs 10 -oldmaxWaitTimeSecs 2100' />\n" +
                    "        </jobref>\n" +
                    "      </command>\n" +
                    "      <command>\n" +
                    "        <jobref name='job_c' nodeStep='true'>\n" +
                    "          <arg line='-maxWaitTimeSecs 30 -oldmaxWaitTimeSecs 2400' />\n" +
                    "        </jobref>\n" +
                    "      </command>\n" +
                    "    </sequence>\n" +
                    "    <uuid>7d6d0958-7987-4a35-9ec3-7720f0985ae4</uuid>\n" +
                    "  </job>\n" +
                    "  <job>\n" +
                    "    <defaultTab>summary</defaultTab>\n" +
                    "    <description></description>\n" +
                    "    <executionEnabled>true</executionEnabled>\n" +
                    "    <id>165ef9b9-61dc-470c-91aa-3f6dc248249d</id>\n" +
                    "    <loglevel>INFO</loglevel>\n" +
                    "    <multipleExecutions>true</multipleExecutions>\n" +
                    "    <name>job_b</name>\n" +
                    "    <nodeFilterEditable>false</nodeFilterEditable>\n" +
                    "    <scheduleEnabled>true</scheduleEnabled>\n" +
                    "    <sequence keepgoing='false' strategy='node-first'>\n" +
                    "      <command>\n" +
                    "        <jobref name='job_c' nodeStep='true' />\n" +
                    "      </command>\n" +
                    "      <command>\n" +
                    "        <jobref name='job_d' nodeStep='true' />\n" +
                    "      </command>\n" +
                    "    </sequence>\n" +
                    "    <uuid>165ef9b9-61dc-470c-91aa-3f6dc248249d</uuid>\n" +
                    "  </job>\n" +
                    "  <job>\n" +
                    "    <defaultTab>summary</defaultTab>\n" +
                    "    <description></description>\n" +
                    "    <executionEnabled>true</executionEnabled>\n" +
                    "    <id>06ba3dce-ba4f-4964-8ac2-349c3a2267bd</id>\n" +
                    "    <loglevel>INFO</loglevel>\n" +
                    "    <multipleExecutions>true</multipleExecutions>\n" +
                    "    <name>job_a</name>\n" +
                    "    <nodeFilterEditable>false</nodeFilterEditable>\n" +
                    "    <scheduleEnabled>true</scheduleEnabled>\n" +
                    "    <sequence keepgoing='false' strategy='node-first'>\n" +
                    "      <command>\n" +
                    "        <exec>echo \"start job_a\"</exec>\n" +
                    "      </command>\n" +
                    "      <command>\n" +
                    "        <jobref name='job_b' nodeStep='true' />\n" +
                    "      </command>\n" +
                    "    </sequence>\n" +
                    "    <uuid>06ba3dce-ba4f-4964-8ac2-349c3a2267bd</uuid>\n" +
                    "  </job>\n" +
                    "</joblist>"

        def jobCreatedResponse = JobUtils.createJob(projectName, xmlJob, client)
        def jobId = jobCreatedResponse.succeeded[0]?.id
        def firstJobId = "06ba3dce-ba4f-4964-8ac2-349c3a2267bd"

        when: "TEST: POST job/id/run should succeed"
        def optionA = 'a'
        Object optionsToMap = [
                "options": [
                        opt2: optionA
                ]
        ]
        def exec1 = JobUtils.executeJobWithOptions(
                firstJobId,
                client,
                optionsToMap
        )
        def exec2 = JobUtils.executeJobWithOptions(
                firstJobId,
                client,
                optionsToMap
        )
        def exec3 = JobUtils.executeJobWithOptions(
                firstJobId,
                client,
                optionsToMap
        )

        Execution execRes1 = MAPPER.readValue(exec1.body().string(), Execution.class)
        String execId1 = execRes1.id
        Execution execRes2 = MAPPER.readValue(exec2.body().string(), Execution.class)
        String execId2 = execRes2.id
        Execution execRes3 = MAPPER.readValue(exec3.body().string(), Execution.class)
        String execId3 = execRes3.id

        Execution execStatus1 = JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                execId1,
                client,
                WaitingTime.EXCESSIVE
        )

        Execution execStatus2 = JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                execId2,
                client,
                WaitingTime.EXCESSIVE
        )

        Execution execStatus3 = JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                execId3,
                client,
                WaitingTime.EXCESSIVE
        )

        then:
        execStatus1.status == ExecutionStatus.SUCCEEDED.state
        execStatus2.status == ExecutionStatus.SUCCEEDED.state
        execStatus3.status == ExecutionStatus.SUCCEEDED.state

    }

    @ExcludePro
    def "test-job-run-steps"(){
        setup:
        def projectName = PROJECT_NAME
        def client = getClient()

        def xmlJob = "<joblist>\n" +
                    "   <job>\n" +
                    "      <name>test job</name>\n" +
                    "      <group>api-test/job-run-steps</group>\n" +
                    "      <description></description>\n" +
                    "      <loglevel>INFO</loglevel>\n" +
                    "      <context>\n" +
                    "          <options>\n" +
                    "              <option name=\"opt1\" value=\"testvalue\" required=\"true\"/>\n" +
                    "              <option name=\"opt2\" values=\"a,b,c\" required=\"true\"/>\n" +
                    "          </options>\n" +
                    "      </context>\n" +
                    "      <dispatch>\n" +
                    "        <threadcount>1</threadcount>\n" +
                    "        <keepgoing>true</keepgoing>\n" +
                    "      </dispatch>\n" +
                    "      <sequence>\n" +
                    "        <command>\n" +
                    "        <exec>echo asd</exec>\n" +
                    "        </command>\n" +
                    "         <command>\n" +
                    "        <scriptargs>\${option.opt2}</scriptargs>\n" +
                    "        <script><![CDATA[#!/bin/bash\n" +
                    "\n" +
                    "echo \"option opt1: \$RD_OPTION_OPT1\"\n" +
                    "echo \"option opt1: @option.opt1@\"\n" +
                    "echo \"node: @node.name@\"\n" +
                    "echo \"option opt2: \$1\"]]></script>\n" +
                    "      </command>\n" +
                    "         <command>\n" +
                    "        <scriptargs>\${option.opt2}</scriptargs>\n" +
                    "        <script><![CDATA[#!/bin/bash\n" +
                    "\n" +
                    "echo \"this is script 2, opt2 is \$RD_OPTION_OPT2\"]]></script>\n" +
                    "      </command>\n" +
                    "      <command>\n" +
                    "        <jobref name='secondary job' group='api-test/job-run-steps'>\n" +
                    "          <arg line='-opt1 asdf -opt2 asdf2' />\n" +
                    "        </jobref>\n" +
                    "      </command>\n" +
                    "      <command>\n" +
                    "        <scriptfile>/home/rundeck/job-run-steps-test-script1.txt</scriptfile>\n" +
                    "        <scriptargs />\n" +
                    "      </command>\n" +
                    "      </sequence>\n" +
                    "   </job>\n" +
                    "   <job>\n" +
                    "      <name>secondary job</name>\n" +
                    "      <group>api-test/job-run-steps</group>\n" +
                    "      <description></description>\n" +
                    "      <loglevel>INFO</loglevel>\n" +
                    "      <context>\n" +
                    "          <options>\n" +
                    "              <option name=\"opt1\" value=\"testvalue\" required=\"true\"/>\n" +
                    "              <option name=\"opt2\" values=\"a,b,c\" required=\"true\"/>\n" +
                    "          </options>\n" +
                    "      </context>\n" +
                    "      <dispatch>\n" +
                    "        <threadcount>1</threadcount>\n" +
                    "        <keepgoing>true</keepgoing>\n" +
                    "      </dispatch>\n" +
                    "      <sequence>\n" +
                    "        <command>\n" +
                    "        <exec>echo asd</exec>\n" +
                    "        </command>\n" +
                    "      </sequence>\n" +
                    "   </job>\n" +
                    "</joblist>"

        def jobCreatedResponse = JobUtils.createJob(projectName, xmlJob, client)

        when: "TEST: job/id/run should succeed"

        def jobId = jobCreatedResponse.succeeded[0]?.id

        def optionA = 'a'
        Object optionsToMap = [
                "options": [
                        opt2: optionA
                ]
        ]

        def runResponse = JobUtils.executeJobWithOptions(jobId, client, optionsToMap)
        assert runResponse.successful

        Execution execId = MAPPER.readValue(runResponse.body().string(), Execution.class)

        Execution jobExecStatusAfterSuccess = JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                execId.id as String,
                client,
                WaitingTime.EXCESSIVE
        )

        then:
        jobExecStatusAfterSuccess.status == ExecutionStatus.SUCCEEDED.state

        when: "TEST: execution output"
        def system = doGet("/system/info")
        SystemInfo systemInfo = MAPPER.readValue(system.body().string(), SystemInfo.class)

        def expectedOutputContent = "asd\n" +
                "option opt1: testvalue\n" +
                "option opt1: testvalue\n" +
                "node: ${systemInfo.system.rundeck.node}\n" +
                "option opt2: a\n" +
                "this is script 2, opt2 is a\n" +
                "asd\n" +
                "this is script 1, opt1 is testvalue"

        File tempFile = File.createTempFile("temp", ".txt")
        FileHelpers.writeFile(expectedOutputContent, tempFile)

        List<String> expectedContent = FileHelpers.readFile(Paths.get(tempFile.toString()))
        assert expectedOutputContent.size() > 0

        def execOutput = client.doGetAcceptAll("/execution/${execId.id}/output.text")
        def execOutputString = execOutput.body().string()
        String[] lines = execOutputString.split("\n")
        List<String> outputContent = new ArrayList<>(Arrays.asList(lines))

        then:
        Arrays.equals(expectedContent.toArray(), outputContent.toArray())
    }

    def "test-workflow-errorhandler"(){
        def client = getClient()
        def projectName = "test-error-handler" // delete me
        Object projectJsonMap = [
                "name": projectName
        ]
        def responseProject = createSampleProject(projectJsonMap)
        assert responseProject.successful

        when: "TEST: execution of job with keepgoing=true, errorhandler step succeeds"
        def stepOutfile = "/tmp/error-handler-out.txt"
        def job1Xml = generateErrorHandlerJob(
                projectName,
                "test-error-handler1",
                true,
                false,
                "echo handler executed successfully >> $stepOutfile",
                "echo final workflow step >> $stepOutfile"
        )
        def jobCreatedResponse1 = JobUtils.createJob(projectName, job1Xml, client)
        def jobId = jobCreatedResponse1.succeeded[0]?.id

        def runResponse = JobUtils.executeJob(jobId, client)
        assert runResponse.successful

        Execution execId = MAPPER.readValue(runResponse.body().string(), Execution.class)

        assert JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                execId.id as String,
                client)
            .status == ExecutionStatus.SUCCEEDED.state

        // We read the output file
        def execArgs = "cat $stepOutfile"
        def readResponse = client.doPostWithoutBody("/project/$projectName/run/command?exec=${execArgs}")
        def readResponseBody = readResponse.body().string()
        def parsedReadBody = MAPPER.readValue(readResponseBody, RunCommand.class)
        String readExecId = parsedReadBody.execution.id

        assert JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                readExecId,
                client
        ).status == ExecutionStatus.SUCCEEDED.state

        def entries = getExecutionOutputLines(readExecId)

        then: "test that errorhandler output was correct"
        FileHelpers.assertLinesInsideEntries(
                List.of(
                        "handler executed successfully",
                        "final workflow step"
                ), entries)

        when: "TEST: execution of job with keepgoing=true, errorhandler step fails"
        def stepOutfile2 = "/tmp/error-handler-out2.txt"
        def job2Xml = generateErrorHandlerJob(
                projectName,
                "test-error-handler2",
                true,
                false,
                "echo handler executed successfully >> $stepOutfile2 ; false",
                "echo final workflow step >> $stepOutfile2"
        )
        def jobCreatedResponse2 = JobUtils.createJob(projectName, job2Xml, client)
        def jobId2 = jobCreatedResponse2.succeeded[0]?.id

        def runResponse2 = JobUtils.executeJob(jobId2, client)
        // The job will execute, so the response will be successful
        assert runResponse2.successful

        Execution execId2 = MAPPER.readValue(runResponse2.body().string(), Execution.class)

        // But the execution will fail
        assert JobUtils.waitForExecution(
                ExecutionStatus.FAILED.state,
                execId2.id as String,
                client
        ).status == ExecutionStatus.FAILED.state

        // So we read the output file
        def execArgs2 = "cat $stepOutfile2"
        def readResponse2 = client.doPostWithoutBody("/project/$projectName/run/command?exec=${execArgs2}")
        def readResponseBody2 = readResponse2.body().string()
        def parsedReadBody2 = MAPPER.readValue(readResponseBody2, RunCommand.class)
        String readExecId2 = parsedReadBody2.execution.id

        assert JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                readExecId2,
                client
        ).status == ExecutionStatus.SUCCEEDED.state

        def entries2 = getExecutionOutputLines(readExecId2)

        then: "test that errorhandler output was correct"
        FileHelpers.assertLinesInsideEntries(
                List.of(
                        "handler executed successfully",
                        "final workflow step"
                ), entries2)

        when: "TEST: execution of job with keepgoing=false, errorhandler step fails"
        def stepOutfile3 = "/tmp/error-handler-out3.txt"
        def job3Xml = generateErrorHandlerJob(
                projectName,
                "recover-handler1",
                false,
                false,
                "echo handler executed successfully >> $stepOutfile3 ; false",
                "echo final workflow step >> $stepOutfile3"
        )
        def jobCreatedResponse3 = JobUtils.createJob(projectName, job3Xml, client)
        def jobId3 = jobCreatedResponse3.succeeded[0]?.id

        def runResponse3 = JobUtils.executeJob(jobId3, client)
        // The job will execute, so the response will be successful
        assert runResponse3.successful

        Execution execId3 = MAPPER.readValue(runResponse3.body().string(), Execution.class)

        // But the execution will fail
        assert JobUtils.waitForExecution(
                ExecutionStatus.FAILED.state,
                execId3.id as String,
                client
        ).status == ExecutionStatus.FAILED.state

        // So we read the output file
        def execArgs3 = "cat $stepOutfile3"
        def readResponse3 = client.doPostWithoutBody("/project/$projectName/run/command?exec=${execArgs3}")
        def readResponseBody3 = readResponse3.body().string()
        def parsedReadBody3 = MAPPER.readValue(readResponseBody3, RunCommand.class)
        String readExecId3 = parsedReadBody3.execution.id

        assert JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                readExecId3,
                client
        ).status == ExecutionStatus.SUCCEEDED.state

        def entries3 = getExecutionOutputLines(readExecId3)

        then: "test that errorhandler output was correct"
        FileHelpers.assertLinesInsideEntries(
                List.of(
                        "handler executed successfully"
                ), entries3)

        when: "TEST: execution of job with keepgoing=false, errorhandler step succeeds (final wf not executed)"
        def stepOutfile4 = "/tmp/error-handler-out4.txt"
        def job4Xml = generateErrorHandlerJob(
                projectName,
                "recover-handler2",
                false,
                false,
                "echo handler executed successfully >> $stepOutfile4",
                "echo final workflow step >> $stepOutfile4"
        )
        def jobCreatedResponse4 = JobUtils.createJob(projectName, job4Xml, client)
        def jobId4 = jobCreatedResponse4.succeeded[0]?.id

        def runResponse4 = JobUtils.executeJob(jobId4, client)
        // The job will execute, so the response will be successful
        assert runResponse4.successful

        Execution execId4 = MAPPER.readValue(runResponse4.body().string(), Execution.class)

        // But the execution will fail
        assert JobUtils.waitForExecution(
                ExecutionStatus.FAILED.state,
                execId4.id as String,
                client
        ).status == ExecutionStatus.FAILED.state

        // So we read the output file
        def execArgs4 = "cat $stepOutfile4"
        def readResponse4 = client.doPostWithoutBody("/project/$projectName/run/command?exec=${execArgs4}")
        def readResponseBody4 = readResponse4.body().string()
        def parsedReadBody4 = MAPPER.readValue(readResponseBody4, RunCommand.class)
        String readExecId4 = parsedReadBody4.execution.id

        assert JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                readExecId4,
                client
        ).status == ExecutionStatus.SUCCEEDED.state

        def entries4 = getExecutionOutputLines(readExecId4)

        then: "test that errorhandler output was correct"
        FileHelpers.assertLinesInsideEntries(
                List.of(
                        "handler executed successfully"
                ), entries4)

        when: "TEST: execution of job with keepgoing=true, errorhandler step succeeds (final wf executed)"
        def stepOutfile5 = "/tmp/error-handler-out5.txt"
        def job5Xml = generateErrorHandlerJob(
                projectName,
                "recover-handler3",
                false,
                true,
                "echo handler executed successfully >> $stepOutfile5",
                "echo final workflow step >> $stepOutfile5"
        )
        def jobCreatedResponse5 = JobUtils.createJob(projectName, job5Xml, client)
        def jobId5 = jobCreatedResponse5.succeeded[0]?.id

        def runResponse5 = JobUtils.executeJob(jobId5, client)
        // The job will execute, so the response will be successful
        assert runResponse5.successful

        Execution execId5 = MAPPER.readValue(runResponse5.body().string(), Execution.class)

        // But the execution will fail
        assert JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                execId5.id as String,
                client
        ).status == ExecutionStatus.SUCCEEDED.state

        // So we read the output file
        def execArgs5 = "cat $stepOutfile5"
        def readResponse5 = client.doPostWithoutBody("/project/$projectName/run/command?exec=${execArgs5}")
        def readResponseBody5 = readResponse5.body().string()
        def parsedReadBody5 = MAPPER.readValue(readResponseBody5, RunCommand.class)
        String readExecId5 = parsedReadBody5.execution.id

        assert JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                readExecId5,
                client
        ).status == ExecutionStatus.SUCCEEDED.state

        def entries5 = getExecutionOutputLines(readExecId5)

        then: "test that errorhandler output was correct"
        FileHelpers.assertLinesInsideEntries(
                List.of(
                        "handler executed successfully",
                        "final workflow step"
                ), entries5)

        cleanup:
        deleteProject(projectName)
    }

    /**
     * Generates errorhandler-covered jobs
     *
     * @param projectName
     * @param jobName
     * @param jobKeepGoing
     * @param errorHandlerKeepGoing
     * @param errorHandlerStepExec
     * @param finalWorkflowExec
     * @return
     */
    def generateErrorHandlerJob(
            String projectName,
            String jobName,
            boolean jobKeepGoing,
            boolean errorHandlerKeepGoing,
            String errorHandlerStepExec,
            String finalWorkflowExec
    ){
        def xmlargs = "echo step will fail; false"

        return "<joblist>\n" +
                "   <job>\n" +
                "      <name>$jobName</name>\n" +
                "      <group>api-test/workflow-errorhandler</group>\n" +
                "      <description></description>\n" +
                "      <loglevel>INFO</loglevel>\n" +
                "      <context>\n" +
                "          <project>$projectName</project>\n" +
                "      </context>\n" +
                "      <dispatch>\n" +
                "        <threadcount>1</threadcount>\n" +
                "        <keepgoing>true</keepgoing>\n" +
                "      </dispatch>\n" +
                "      <sequence keepgoing=\"$jobKeepGoing\">\n" +
                "        <command>\n" +
                "          <exec>$xmlargs</exec>\n" +
                "          <errorhandler keepgoingOnSuccess=\"$errorHandlerKeepGoing\">\n" +
                "            <exec>$errorHandlerStepExec</exec>\n" +
                "          </errorhandler>\n" +
                "        </command>\n" +
                "        <command>\n" +
                "            <exec>$finalWorkflowExec</exec>\n" +
                "        </command>\n" +
                "      </sequence>\n" +
                "   </job>\n" +
                "</joblist>"
    }

    def "test-renamed-child-job"() {
        given:
        def projectName = "test-renamed-child-job"
        def client = getClient()
        Object projectJsonMap = [
                "name": projectName.toString(),
                "description": "test-renamed-child-job",
                "config": [
                        "test.property": "test value",
                        "project.execution.history.cleanup.enabled": "true",
                        "project.execution.history.cleanup.retention.days": "1",
                        "project.execution.history.cleanup.batch": "500",
                        "project.execution.history.cleanup.retention.minimum": "0",
                        "project.execution.history.cleanup.schedule": "0 0/1 * 1/1 * ? *"
                ]
        ]

        def responseProject = createSampleProject(projectJsonMap)
        assert responseProject.successful

        // Note how the name of the child job iin the ref s Child-2.
        // This simulates the renaming of the child job.
        def testXml = """
            <joblist>
               <job>
                  <name>Child</name>
                  <group>api-test/job-run</group>
                  <uuid>b51275b4-71a1-4127-80c0-366fa1f76d1d</uuid>
                  <description></description>
                  <loglevel>INFO</loglevel>
                  <context>
                      <project>${projectName}</project>
                  </context>
                  <dispatch>
                    <threadcount>1</threadcount>
                    <keepgoing>true</keepgoing>
                  </dispatch>
                  <sequence>
                    <command>
                    <exec>echo 1</exec>
                    </command>
                  </sequence>
               </job>
               <job>
                  <name>Parent</name>
                  <group>api-test/job-run</group>
                  <uuid>c7b28a32-b32c-459c-ab94-1e253f50fd47</uuid>
                  <description/>
                  <executionEnabled>true</executionEnabled>
                  <loglevel>INFO</loglevel>
                   <context>
                      <project>${projectName}</project>
                  </context>
                  <nodeFilterEditable>false</nodeFilterEditable>
                  <sequence keepgoing=\"false\" strategy=\"parallel\">
                     <command>
                         <jobref name=\"Child-2\">
                           <uuid>b51275b4-71a1-4127-80c0-366fa1f76d1d</uuid>
                         </jobref>
                     </command>
                     <command>
                         <jobref name=\"Child-2\" nodeStep=\"true\">
                           <uuid>b51275b4-71a1-4127-80c0-366fa1f76d1d</uuid>
                         </jobref>
                     </command>
                  </sequence>
               </job>
            </joblist>
            """

        def jobCreatedResponse = JobUtils.createJob(projectName, testXml, client)

        when: "Job and referenced job created"
        def childJob = jobCreatedResponse.succeeded[0]?.id
        def parentJob = jobCreatedResponse.succeeded[1]?.id

        then:
        childJob != null
        parentJob != null

        when: "run job"
        def jobRun = JobUtils.executeJob(parentJob, client)
        assert jobRun.successful

        Execution exec = MAPPER.readValue(jobRun.body().string(), Execution.class)

        Execution jobExecutionStatus = JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                exec.id as String,
                client,
                WaitingTime.EXCESSIVE)

        then:
        jobExecutionStatus.status == ExecutionStatus.SUCCEEDED.state
        // Verify that all steps on the first (and the only) node are in the SUCCEEDED state
        client.jsonValue(client.doGetAcceptAll("/execution/$jobExecutionStatus.id/state").body(), Map).nodes.values()[0].every({val -> val.executionState == "SUCCEEDED"} )
    }

    def "test /api/job/id/executions returns referenced executions"() {
        given:
        def projectName = "test-referenced-executions"
        def client = getClient()
        Object projectJsonMap = [
                "name": projectName.toString(),
                "description": "test-referenced-executions",
                "config": [
                        "test.property": "test value",
                        "project.execution.history.cleanup.enabled": "true",
                        "project.execution.history.cleanup.retention.days": "1",
                        "project.execution.history.cleanup.batch": "500",
                        "project.execution.history.cleanup.retention.minimum": "0",
                        "project.execution.history.cleanup.schedule": "0 0/1 * 1/1 * ? *"
                ]
        ]

        def responseProject = createSampleProject(projectJsonMap)
        assert responseProject.successful

        String childJobId = "a472767a-6575-46f1-8ab4-6cd2b289f1f3"
        def testXml = """
            <joblist>
               <job>
                  <name>Child</name>
                  <group>api-test/job-run</group>
                  <uuid>${childJobId}</uuid>
                  <description></description>
                  <loglevel>INFO</loglevel>
                  <context>
                      <project>${projectName}</project>
                  </context>
                  <dispatch>
                    <threadcount>1</threadcount>
                    <keepgoing>true</keepgoing>
                  </dispatch>
                  <sequence>
                    <command>
                    <exec>echo 1</exec>
                    </command>
                  </sequence>
               </job>
               <job>
                  <name>Parent</name>
                  <group>api-test/job-run</group>
                  <uuid>14e09b60-436c-42b0-b9dc-6308f1fbd5cc</uuid>
                  <description/>
                  <executionEnabled>true</executionEnabled>
                  <loglevel>INFO</loglevel>
                   <context>
                      <project>${projectName}</project>
                  </context>
                  <nodeFilterEditable>false</nodeFilterEditable>
                  <sequence keepgoing=\"false\" strategy=\"parallel\">
                     <command>
                         <jobref name=\"Child\">
                           <uuid>${childJobId}</uuid>
                         </jobref>
                     </command>                     
                  </sequence>
               </job>
            </joblist>
            """

        def jobCreatedResponse = JobUtils.createJob(projectName, testXml, client)

        when: "Job and referenced job created"
        def childJob = jobCreatedResponse.succeeded[0]?.id
        def parentJob = jobCreatedResponse.succeeded[1]?.id

        then:
        childJob != null
        parentJob != null

        when: "run job"
        def jobRun = JobUtils.executeJob(parentJob, client)
        assert jobRun.successful

        Execution exec = MAPPER.readValue(jobRun.body().string(), Execution.class)

        Execution jobExecutionStatus = JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                exec.id as String,
                client,
                WaitingTime.EXCESSIVE)

        then:
        jobExecutionStatus.status == ExecutionStatus.SUCCEEDED.state

        when: "get executions default behavior"
        def response = doGet("/job/${childJobId}/executions")
        then:
        verifyAll {
            response.successful
            response.code() == 200
            def json = jsonValue(response.body())
            json.executions.size() == 0
        }
        when: "get executions with includeJobRef true"
        def response1 = doGet("/job/${childJobId}/executions?includeJobRef=true")
        then:
        verifyAll {
            response1.successful
            response1.code() == 200
            def json = jsonValue(response1.body())
            json.executions.size() == 1
            json.executions[0].id?.toString() == exec.id
        }
        when: "get executions with includeJobRef false"
        def response2 = doGet("/job/${childJobId}/executions?includeJobRef=false")
        then:
        verifyAll {
            response2.successful
            response2.code() == 200
            def json = jsonValue(response2.body())
            json.executions.size() == 0
        }
        when: "get executions with api version before 50 should keep previous behavior"
        client.apiVersion = 49
        def response3 = doGet("/job/${childJobId}/executions?includeJobRef=true")
        then:
        verifyAll {
            response3.successful
            response3.code() == 200
            def json = jsonValue(response3.body())
            json.executions.size() == 0
        }
    }

    def "test /api/job/id/executions returns referenced executions with child and grandchild jobs"() {
        given:
        def projectName = "test-referenced-executions-grand-child"
        def client = getClient()
        Object projectJsonMap = [
                "name": projectName.toString(),
                "description": "test-referenced-executions",
                "config": [
                        "test.property": "test value 1",
                        "project.execution.history.cleanup.enabled": "true",
                        "project.execution.history.cleanup.retention.days": "1",
                        "project.execution.history.cleanup.batch": "500",
                        "project.execution.history.cleanup.retention.minimum": "0",
                        "project.execution.history.cleanup.schedule": "0 0/1 * 1/1 * ? *"
                ]
        ]

        def responseProject = createSampleProject(projectJsonMap)
        assert responseProject.successful

        String childJobId = "bd4f1bb3-67d0-45ba-b12f-6d2efac55bc6"
        String grandChildJobId = "59f0156b-a584-4a4e-9004-24d8605361bc"
        def testXml = """
            <joblist>
               <job>
                  <name>GrandChild</name>
                  <group>api-test/job-run</group>
                  <uuid>${grandChildJobId}</uuid>
                  <description></description>
                  <loglevel>INFO</loglevel>
                  <context>
                      <project>${projectName}</project>
                  </context>
                  <dispatch>
                    <threadcount>1</threadcount>
                    <keepgoing>true</keepgoing>
                  </dispatch>
                  <sequence>
                    <command>
                    <exec>echo 1</exec>
                    </command>
                  </sequence>
               </job> 
               <job>
                  <name>Child</name>
                  <group>api-test/job-run</group>
                  <uuid>${childJobId}</uuid>
                  <description></description>
                  <loglevel>INFO</loglevel>
                  <context>
                      <project>${projectName}</project>
                  </context>
                  <dispatch>
                    <threadcount>1</threadcount>
                    <keepgoing>true</keepgoing>
                  </dispatch>
                  <sequence>
                    <command>
                         <jobref name=\"GrandChild\">
                           <uuid>${grandChildJobId}</uuid>
                         </jobref>
                     </command>   
                  </sequence>
               </job>
               <job>
                  <name>Parent</name>
                  <group>api-test/job-run</group>
                  <uuid>be502211-014a-4937-8dee-18293c56b7f7</uuid>
                  <description/>
                  <executionEnabled>true</executionEnabled>
                  <loglevel>INFO</loglevel>
                   <context>
                      <project>${projectName}</project>
                  </context>
                  <nodeFilterEditable>false</nodeFilterEditable>
                  <sequence keepgoing=\"false\" strategy=\"parallel\">
                     <command>
                         <jobref name=\"Child\">
                           <uuid>${childJobId}</uuid>
                         </jobref>
                     </command>                     
                  </sequence>
               </job>
            </joblist>
            """

        def jobCreatedResponse = JobUtils.createJob(projectName, testXml, client)

        when: "Job and referenced job created"
        jobCreatedResponse.succeeded
        def grandChildJob = jobCreatedResponse.succeeded[0]?.id
        def childJob = jobCreatedResponse.succeeded[1]?.id
        def parentJob = jobCreatedResponse.succeeded[2]?.id

        then:
        grandChildJob != null
        childJob != null
        parentJob != null

        when: "run job"
        def jobRun = JobUtils.executeJob(parentJob, client)
        assert jobRun.successful

        Execution exec = MAPPER.readValue(jobRun.body().string(), Execution.class)

        Execution jobExecutionStatus = JobUtils.waitForExecution(
                ExecutionStatus.SUCCEEDED.state,
                exec.id as String,
                client,
                WaitingTime.EXCESSIVE)

        then:
        jobExecutionStatus.status == ExecutionStatus.SUCCEEDED.state

        when: "get child executions"
        def response = doGet("/job/${childJobId}/executions?includeJobRef=true")
        then:
        verifyAll {
            response.successful
            response.code() == 200
            def json = jsonValue(response.body())
            json.executions.size() == 1
            json.executions[0].id?.toString() == exec.id
        }
        when: "get grand child executions"
        def response1 = doGet("/job/${grandChildJobId}/executions?includeJobRef=true")
        then:
        verifyAll {
            response1.successful
            response1.code() == 200
            def json = jsonValue(response1.body())
            json.executions.size() == 1
            json.executions[0].id?.toString() == exec.id
        }
    }

    def generateRuntime(int secondsInFuture){
        TimeZone timeZone = TimeZone.getDefault()
        Calendar cal = Calendar.getInstance(timeZone)
        cal.add(Calendar.SECOND, secondsInFuture)
        SimpleDateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"))
        return [string: iso8601Format.format(cal.time), date: cal.time]
    }

    def createSampleProject = (Object projectJsonMap) -> {
        return client.doPost("/projects", projectJsonMap)
    }

    def "multiple executions job"(){
        given:
        String projectName = "multiExecutionProject"
        String jobUuid = "e3aad000-7d0e-4a0e-8ded-f70431de7aaa"
        setupProject(projectName)
        def yamlJob = """
                            -
                              defaultTab: nodes
                              executionEnabled: true
                              name: "multiExecutionsJob"
                              sequence:
                                commands:
                                - exec: sleep 5
                                keepgoing: false
                                strategy: sequential
                              multipleExecutions: true
                              uuid: ${jobUuid}
                            """
        def pathToJob = JobUtils.generateFileToImport(yamlJob, "yaml")
        def multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xmlBatch", new File(pathToJob).name, RequestBody.create(new File(pathToJob), MultipartBody.FORM))
                .build()
        client.doPostWithMultipart("/project/${projectName}/jobs/import?format=yaml&dupeOption=skip", multipartBody)
        when:
        def jobExecResponseFor1 = JobUtils.executeJob(jobUuid, client)
        def jobExecResponseFor2 = JobUtils.executeJob(jobUuid, client)
        def jobExecResponseFor3 = JobUtils.executeJob(jobUuid, client)
        then:
        assert jobExecResponseFor1.successful
        assert jobExecResponseFor2.successful
        assert jobExecResponseFor3.successful
        cleanup:
        // This is to allow the executions to finish
        waitFor(ExecutionUtils.Retrievers.executionsForJobId(client, jobUuid),
                verifyForAll(ExecutionUtils.Verifiers.executionFinished()),
                WaitingTime.EXCESSIVE )
        deleteProject(projectName)
    }

}
