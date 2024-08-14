package org.rundeck.tests.functional.api.job

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.rundeck.util.api.responses.jobs.CreateJobResponse
import org.rundeck.util.api.responses.common.ErrorResponse
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.api.responses.execution.ExecutionOutput
import org.rundeck.util.api.responses.jobs.JobExecutionsResponse
import org.rundeck.util.api.responses.execution.RunCommand
import org.rundeck.util.api.responses.system.SystemInfo
import org.rundeck.util.annotations.APITest
import org.rundeck.util.annotations.ExcludePro
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.FileHelpers
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
            sleep 5000
            def response = doGet("/job/${jobId}/executions")
        then:
            verifyAll {
                response.successful
                response.code() == 200
                def json = jsonValue(response.body())
                json.executions.size() == 1
                json.executions[0].id == execId
            }
    }

    def "job/id/executions?status=succeeded should succeed with 1 results"() {
        when:
            def response = doGet("/job/${jobId}/executions?status=succeeded")
        then:
        verifyAll {
            response.successful
            response.code() == 200
            def json = jsonValue(response.body())
            json.executions.size() == 1
            json.executions[0].id == execId
        }
    }

    def "run again job/id/run should succeed"() {
        when:
            def jobRun = JobUtils.executeJobWithArgs(jobId, client, "-opt2 a")
            execId2 = jsonValue(jobRun.body()).id as Integer
        then:
            verifyAll {
                execId2 > 0
            }
        when: "job/id/executions all results"
            sleep 5000
            def response = doGet("/job/${jobId}/executions")
        then:
         verifyAll {
             response.successful
             response.code() == 200
             def json = jsonValue(response.body())
             json.executions.size() == 2
         }
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
            sleep 5000
            def response = doGet("/job/${jobId2}/executions?status=failed")
        then:
        verifyAll {
            response.successful
            response.code() == 200
            def json = jsonValue(response.body())
            json.executions.size() == 1
            json.executions[0].id == execId2
        }
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
            sleep 5000
        then:
            def response = doGet("/job/${jobId3}/executions?status=aborted")
            verifyAll {
                response.successful
                response.code() == 200
                def json = jsonValue(response.body())
                json.executions.size() == 1
                json.executions[0].id == execId3
            }
    }

    def "test-job-flip-executionEnabled"(){
        given:
        def projectName = "test-job-flip-executionEnabled"
        def client = getClient()
        ObjectMapper mapper = new ObjectMapper()
        Object projectJsonMap = [
                "name": projectName.toString(),
                "description": "test1",
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

        def jobName1 = "xmljob"
        def jobXml1 = JobUtils.generateScheduledExecutionXml(jobName1)

        def job1CreatedResponse = JobUtils.createJob(projectName, jobXml1, client)
        assert job1CreatedResponse.successful

        when: "TEST: when execution is on, job does execute"
        CreateJobResponse job1CreatedParsedResponse = mapper.readValue(job1CreatedResponse.body().string(), CreateJobResponse.class)
        def job1Id = job1CreatedParsedResponse.succeeded[0]?.id
        def executions1 = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1 = mapper.readValue(executions1.body().string(), JobExecutionsResponse.class)
        then:
        parsedExecutionsResponseForExecution1.executions.size() == 0

        when:
        def jobExecResponseFor1 = JobUtils.executeJob(job1Id, client)
        assert jobExecResponseFor1.successful
        def executions1AfterExecResponse = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1AfterExec = mapper.readValue(executions1AfterExecResponse.body().string(), JobExecutionsResponse.class)
        then: "assert_job_execution_count job1"
        parsedExecutionsResponseForExecution1AfterExec.executions.size() == 1

        when: "TEST: when execution is off, job doesn't execute"
        def disabledJobsResponse = client.doPostWithoutBody("/job/${job1Id}/execution/disable")
        assert disabledJobsResponse.successful

        def jobExecResponseFor1AfterDisable = JobUtils.executeJob(job1Id, client)
        assert jobExecResponseFor1AfterDisable.code() == 400 // bc execs are disabled

        def executionsForJob1AfterDisable = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1AfterDisable = mapper.readValue(executionsForJob1AfterDisable.body().string(), JobExecutionsResponse.class)

        then:
        parsedExecutionsResponseForExecution1AfterDisable.executions.size() == 1

        when: "TEST: when execution is off and then on again, job does execute"
        def enabledJobsResponse = client.doPostWithoutBody("/job/${job1Id}/execution/enable")
        assert enabledJobsResponse.successful

        // Necessary since the api needs to breathe after enable execs
        Thread.sleep(WaitingTime.LOW.duration.toMillis())

        def jobExecResponseFor1AfterEnable = JobUtils.executeJob(job1Id, client)
        assert jobExecResponseFor1AfterEnable.successful


        def executionsForJob1AfterEnable = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1AfterEnable = mapper.readValue(executionsForJob1AfterEnable.body().string(), JobExecutionsResponse.class)

        then:
        parsedExecutionsResponseForExecution1AfterEnable.executions.size() == 2
    }

    def "test-job-flip-executionEnabled-bulk"(){
        given:
        def projectName = "test-job-flip-executionEnabled-bulk"
        def client = getClient()
        ObjectMapper mapper = new ObjectMapper()
        Object projectJsonMap = [
                "name": projectName.toString(),
                "description": "test1",
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

        def jobName1 = "xmljob"
        def jobXml1 = JobUtils.generateScheduledExecutionXml(jobName1)

        def jobName2 = "xmljob2"
        def jobXml2 = JobUtils.generateScheduledExecutionXml(jobName2)

        def job1CreatedResponse = JobUtils.createJob(projectName, jobXml1, client)
        assert job1CreatedResponse.successful

        def job2CreatedResponse = JobUtils.createJob(projectName, jobXml2, client)
        assert job2CreatedResponse.successful

        when: "assert_job_execution_count with job 1"
        CreateJobResponse job1CreatedParsedResponse = mapper.readValue(job1CreatedResponse.body().string(), CreateJobResponse.class)
        def job1Id = job1CreatedParsedResponse.succeeded[0]?.id
        def executions1 = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1 = mapper.readValue(executions1.body().string(), JobExecutionsResponse.class)
        then:
        parsedExecutionsResponseForExecution1.executions.size() == 0

        when: "assert_job_execution_count with job 2"
        CreateJobResponse job2CreatedParsedResponse = mapper.readValue(job2CreatedResponse.body().string(), CreateJobResponse.class)
        def job2Id = job2CreatedParsedResponse.succeeded[0]?.id
        def executions2 = doGet("/job/${job2Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution2 = mapper.readValue(executions2.body().string(), JobExecutionsResponse.class)
        then:
        parsedExecutionsResponseForExecution2.executions.size() == 0

        when: "execute_job 1"
        def jobExecResponseFor1 = JobUtils.executeJob(job1Id, client)
        assert jobExecResponseFor1.successful
        def executions1AfterExecResponse = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1AfterExec = mapper.readValue(executions1AfterExecResponse.body().string(), JobExecutionsResponse.class)
        then: "assert_job_execution_count job1"
        parsedExecutionsResponseForExecution1AfterExec.executions.size() == 1

        when: "execute_job 2"
        def jobExecResponseFor2 = JobUtils.executeJob(job2Id, client)
        assert jobExecResponseFor2.successful
        def executions2AfterExecResponse = doGet("/job/${job2Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution2AfterExec = mapper.readValue(executions2AfterExecResponse.body().string(), JobExecutionsResponse.class)
        then: "assert_job_execution_count job1"
        parsedExecutionsResponseForExecution2AfterExec.executions.size() == 1

        when: "TEST: bulk job execution disable"
        Object idList = [
                "idlist": List.of(
                        job1Id,
                        job2Id
                )
        ]
        def disabledJobsResponse = doPost("/jobs/execution/disable", idList)
        assert disabledJobsResponse.successful

        def jobExecResponseFor1AfterDisable = JobUtils.executeJob(job1Id, client)
        assert jobExecResponseFor1AfterDisable.code() == 400 // bc execs are disabled

        def jobExecResponseFor2AfterDisable = JobUtils.executeJob(job2Id, client)
        assert jobExecResponseFor2AfterDisable.code() == 400  // bc execs are disabled

        def executionsForJob1AfterDisable = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1AfterDisable = mapper.readValue(executionsForJob1AfterDisable.body().string(), JobExecutionsResponse.class)

        def executionsForJob2AfterDisable = doGet("/job/${job2Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution2AfterDisable = mapper.readValue(executionsForJob2AfterDisable.body().string(), JobExecutionsResponse.class)

        then:
        parsedExecutionsResponseForExecution1AfterDisable.executions.size() == 1
        parsedExecutionsResponseForExecution2AfterDisable.executions.size() == 1

        when: "TEST: bulk job execution enable"
        def enabledJobsResponse = doPost("/jobs/execution/enable", idList)
        assert enabledJobsResponse.successful

        // Necessary since the api needs to breathe after enable execs
        Thread.sleep(WaitingTime.LOW.duration.toMillis())

        def jobExecResponseFor1AfterEnable = JobUtils.executeJob(job1Id, client)
        assert jobExecResponseFor1AfterEnable.successful

        def jobExecResponseFor2AfterEnable = JobUtils.executeJob(job2Id, client)
        assert jobExecResponseFor2AfterEnable.successful

        def executionsForJob1AfterEnable = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1AfterEnable = mapper.readValue(executionsForJob1AfterEnable.body().string(), JobExecutionsResponse.class)

        def executionsForJob2AfterEnable = doGet("/job/${job2Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution2AfterEnable = mapper.readValue(executionsForJob2AfterEnable.body().string(), JobExecutionsResponse.class)

        then:
        parsedExecutionsResponseForExecution1AfterEnable.executions.size() == 2
        parsedExecutionsResponseForExecution2AfterEnable.executions.size() == 2
    }

    def "test-job-flip-scheduleEnabled-bulk"(){
        given:
        def projectName = "test-job-flip-scheduleEnabled-bulk"
        def client = getClient()
        ObjectMapper mapper = new ObjectMapper()
        Object projectJsonMap = [
                "name": projectName.toString(),
                "description": "test-job-flip-scheduleEnabled-bulk",
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

        def jobName1 = "scheduledJob1"
        def jobXml1 = JobUtils.generateScheduledJobsXml(jobName1)

        def jobName2 = "scheduledJob2"
        def jobXml2 = JobUtils.generateScheduledJobsXml(jobName2)

        def job1CreatedResponse = JobUtils.createJob(projectName, jobXml1, client)
        assert job1CreatedResponse.successful

        def job2CreatedResponse = JobUtils.createJob(projectName, jobXml2, client)
        assert job2CreatedResponse.successful

        CreateJobResponse job1CreatedParsedResponse = mapper.readValue(job1CreatedResponse.body().string(), CreateJobResponse.class)
        def job1Id = job1CreatedParsedResponse.succeeded[0]?.id

        CreateJobResponse job2CreatedParsedResponse = mapper.readValue(job2CreatedResponse.body().string(), CreateJobResponse.class)
        def job2Id = job2CreatedParsedResponse.succeeded[0]?.id

        when: "assert_job_schedule_enabled for job1"
        def job1Detail = JobUtils.getJobDetailsById(job1Id as String, mapper, client)
        then:
        job1Detail?.executionEnabled

        when: "assert_job_schedule_enabled for job2"
        def job2Detail = JobUtils.getJobDetailsById(job2Id as String, mapper, client)
        then:
        job2Detail?.executionEnabled

        Thread.sleep(WaitingTime.LOW.duration.toMillis()) // As the original test says

        when: "TEST: bulk job schedule disable"
        Object idList = [
                "idlist" : List.of(
                        job1Id,
                        job2Id
                )
        ]
        def disableSchedulesResponse = doPost("/jobs/schedule/disable", idList)
        assert disableSchedulesResponse.successful
        def job1DetailAfterDisable = JobUtils.getJobDetailsById(job1Id as String, mapper, client)
        def job2DetailAfterDisable = JobUtils.getJobDetailsById(job2Id as String, mapper, client)

        then:
        !job1DetailAfterDisable?.scheduleEnabled
        !job2DetailAfterDisable?.scheduleEnabled

        when: "TEST: bulk job schedule enable"
        def enableSchedulesResponse = doPost("/jobs/schedule/enable", idList)
        assert enableSchedulesResponse.successful
        def job1DetailAfterEnable = JobUtils.getJobDetailsById(job1Id as String, mapper, client)
        def job2DetailAfterEnable = JobUtils.getJobDetailsById(job2Id as String, mapper, client)

        then:
        job1DetailAfterEnable?.scheduleEnabled
        job2DetailAfterEnable?.scheduleEnabled

    }

    def "test-job-flip-scheduleEnabled"(){
        given:
        def projectName = "test-job-flip-scheduleEnabled"
        def client = getClient()
        ObjectMapper mapper = new ObjectMapper()
        Object projectJsonMap = [
                "name": projectName.toString(),
                "description": "test-job-flip-scheduleEnabled",
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

        def jobName1 = "scheduledJob1"
        def jobXml1 = JobUtils.generateScheduledJobsXml(jobName1)

        def job1CreatedResponse = JobUtils.createJob(projectName, jobXml1, client)
        assert job1CreatedResponse.successful


        CreateJobResponse job1CreatedParsedResponse = mapper.readValue(job1CreatedResponse.body().string(), CreateJobResponse.class)
        def job1Id = job1CreatedParsedResponse.succeeded[0]?.id


        when: "assert_job_schedule_enabled for job1"
        def job1Detail = JobUtils.getJobDetailsById(job1Id as String, mapper, client)
        then:
        job1Detail?.executionEnabled

        when: "TEST: when schedule is on, job does execute"
        def disableSchedulesResponse = client.doPostWithoutBody("/job/${job1Id}/schedule/disable")
        assert disableSchedulesResponse.successful
        def job1DetailAfterDisable = JobUtils.getJobDetailsById(job1Id as String, mapper, client)

        then:
        !job1DetailAfterDisable?.scheduleEnabled

        when: "TEST: bulk job schedule enable"
        def enableSchedulesResponse = client.doPostWithoutBody("/job/${job1Id}/schedule/enable")
        assert enableSchedulesResponse.successful
        def job1DetailAfterEnable = JobUtils.getJobDetailsById(job1Id as String, mapper, client)

        then:
        job1DetailAfterEnable?.scheduleEnabled

    }

    def "test-job-long-run"(){
        given:
        def projectName = "test-job-long-run"
        def client = getClient()
        ObjectMapper mapper = new ObjectMapper()
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
        def created = JobUtils.createJob(projectName, testXml, client)
        assert created.successful

        when: "Job and referenced job created"
        CreateJobResponse jobCreatedResponse = mapper.readValue(
                created.body().string(),
                CreateJobResponse.class
        )
        def jobId = jobCreatedResponse.succeeded[0]?.id
        def refJobId = jobCreatedResponse.succeeded[1]?.id

        then:
        jobId != null
        refJobId != null

        when: "run job test"
        def jobRun = JobUtils.executeJob(jobId, client)
        assert jobRun.successful

        Execution exec = mapper.readValue(jobRun.body().string(), Execution.class)

        Execution JobExecutionStatus = JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                exec.id as String,
                mapper,
                client,
                WaitingTime.MODERATE,
                WaitingTime.EXCESSIVE
        )

        then:
        JobExecutionStatus.status == ExecutionStatus.SUCCEEDED.state

        when: "run job test"
        def referencedJobRun = JobUtils.executeJob(refJobId, client)
        assert referencedJobRun.successful

        Execution refExec = mapper.readValue(referencedJobRun.body().string(), Execution.class)

        Execution refJobExecutionStatus = JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                refExec.id as String,
                mapper,
                client,
                WaitingTime.MODERATE,
                WaitingTime.EXCESSIVE
        )

        then:
        refJobExecutionStatus.status == ExecutionStatus.SUCCEEDED.state

    }

    def "test-job-retry"(){
        given:
        def projectName = "test-job-retry"
        def client = getClient()
        ObjectMapper mapper = new ObjectMapper()
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
        def created = JobUtils.createJob(projectName, testXml, client)
        assert created.successful

        when: "Job and referenced job created"
        CreateJobResponse jobCreatedResponse = mapper.readValue(
                created.body().string(),
                CreateJobResponse.class
        )
        def jobId = jobCreatedResponse.succeeded[0]?.id

        then:
        jobId != null

        when: "run job test"
        def execArgs = "-opt2 a"
        def jobRun = JobUtils.executeJobWithArgs(jobId, client, execArgs)
        assert jobRun.successful
        Execution parsedExecutionsResponse = mapper.readValue(jobRun.body().string(), Execution.class)
        def execId = parsedExecutionsResponse.id

        then:
        execId > 0

        when: "fail and retry 1"
        def execDetails = JobUtils.waitForExecutionToBe(
                ExecutionStatus.FAILED_WITH_RETRY.state,
                execId as String,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        )
        def retryId1 = execDetails.retriedExecution.id

        then:
        retryId1 > 0

        when: "fail and retry 2"
        def execDetails2 = JobUtils.waitForExecutionToBe(
                ExecutionStatus.FAILED_WITH_RETRY.state,
                retryId1 as String,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        )
        def retryId2 = execDetails2.retriedExecution.id

        then:
        retryId2 > 0

        when: "final retry"
        def execDetailsFinal = JobUtils.waitForExecutionToBe(
                ExecutionStatus.FAILED.state,
                retryId2 as String,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        )

        then:
        execDetailsFinal.retriedExecution == null
        execDetailsFinal.retryAttempt == 2
        execDetailsFinal.status == ExecutionStatus.FAILED.state

    }

    def "test-job-run-GET-405"(){
        given:
        def projectName = "test-job-run-GET-405"
        def client = getClient()
        ObjectMapper mapper = new ObjectMapper()
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

        def job1CreatedResponse = JobUtils.createJob(projectName, jobXml1, client)
        assert job1CreatedResponse.successful

        CreateJobResponse job1CreatedParsedResponse = mapper.readValue(job1CreatedResponse.body().string(), CreateJobResponse.class)
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
        ObjectMapper mapper = new ObjectMapper()

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
        def created = JobUtils.createJob(projectName, testXml, client)
        assert created.successful

        CreateJobResponse jobCreatedResponse = mapper.readValue(
                created.body().string(),
                CreateJobResponse.class
        )
        def jobId = jobCreatedResponse.succeeded[0]?.id

        when: "TEST: POST job/id/run should succeed with future time"
        def runtime = generateRuntime(25)
        def runLaterExecResponse = JobUtils.executeJobLaterWithArgsAndRuntime(
                jobId,
                client,
                "-opt2+a",
                runtime.string
        )
        assert runLaterExecResponse.successful
        Execution parsedExec = mapper.readValue(runLaterExecResponse.body().string(), Execution.class)
        String execId = parsedExec.id
        def dateS = parsedExec.dateStarted.date

        then:
        execId != null
        dateS.toString() == runtime.date.toString()

        when: "Wait until execution succeeds"
        def execAfterWait = JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                execId,
                mapper,
                client,
                WaitingTime.MODERATE,
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
        ObjectMapper mapper = new ObjectMapper()

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
        def created = JobUtils.createJob(projectName, testXml, client)
        assert created.successful
        CreateJobResponse jobCreatedResponse = mapper.readValue(
                created.body().string(),
                CreateJobResponse.class
        )
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

        Execution execRes = mapper.readValue(response.body().string(), Execution.class)
        String execId = execRes.id

        def execution = JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                execId,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        )

        then:
        execution.status == ExecutionStatus.SUCCEEDED.state

        when: "TEST: POST job/id/run should fail"
        def response2 = JobUtils.executeJob(jobId2, client)
        assert response2.successful

        Execution execRes2 = mapper.readValue(response2.body().string(), Execution.class)
        String execId2 = execRes2.id
        def execution2 = JobUtils.waitForExecutionToBe(
                ExecutionStatus.FAILED.state,
                execId2,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        )

        then:
        execution2.status == ExecutionStatus.FAILED.state // should fail

        when: "TEST: POST job/id/run should succeed w/ filter"
        Object filter = [
                "filter": "name: .*"
        ]
        def response3 = JobUtils.executeJobWithOptions(jobId2, client, filter)
        assert response3.successful

        Execution execRes3 = mapper.readValue(response3.body().string(), Execution.class)
        String execId3 = execRes3.id
        def execution3 = JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                execId3,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        )

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

        Execution execRes4 = mapper.readValue(response4.body().string(), Execution.class)
        String execId4 = execRes4.id
        def execution4 = JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                execId4,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        )

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
        ObjectMapper mapper = new ObjectMapper()

        setupProjectArchiveDirectoryResource(projectName, '/projects-import/webhook-notification-project')

        // We have the jobs id, since they are already imported
        def openNcJobId = "c81aa8af-1e0e-4fce-a7bd-102b87922ef2"
        def notificationJobId = "a20106e4-37e6-489b-a783-2beb04a367c1"
        def readNcOutputJobId = "ccda2e41-277a-4c62-ba01-8f930663561e"

        when: "We first open NC connection"
        def openNcJobRun = JobUtils.executeJob(openNcJobId, client)
        assert openNcJobRun.successful
        Execution openNcJobResponse = mapper.readValue(openNcJobRun.body().string(), Execution.class)

        then: "We wait for succeeded exec"
        JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                openNcJobResponse.id as String,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.EXCESSIVE
        ).status == ExecutionStatus.SUCCEEDED.state

        when: "We run the job with notification"
        def jobRun = JobUtils.executeJob(notificationJobId, client)
        assert jobRun.successful
        Execution parsedExecutionsResponse = mapper.readValue(jobRun.body().string(), Execution.class)

        then: "Will succeed"
        JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                parsedExecutionsResponse.id as String,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.EXCESSIVE
        ).status == ExecutionStatus.SUCCEEDED.state

        when: "We wait 10 seconds for netcat to deliver response"
        Thread.sleep(Duration.ofSeconds(10).toMillis())

        // Then run the job that reads the output of request
        def readJobRun = JobUtils.executeJob(readNcOutputJobId, client)
        assert readJobRun.successful
        Execution readJobRunResponse = mapper.readValue(readJobRun.body().string(), Execution.class)
        def readJobSucceeded = JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                readJobRunResponse.id as String,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        )
        assert readJobSucceeded.status == ExecutionStatus.SUCCEEDED.state
        def execOutputResponse = client.doGetAcceptAll("/execution/$readJobRunResponse.id/output")
        ExecutionOutput execOutput = mapper.readValue(execOutputResponse.body().string(), ExecutionOutput.class)

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
        ObjectMapper mapper = new ObjectMapper()

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
                    "          <arg line='-maxWaitTimeSecs 20 -oldmaxWaitTimeSecs 2100' />\n" +
                    "        </jobref>\n" +
                    "      </command>\n" +
                    "      <command>\n" +
                    "        <jobref name='job_c' nodeStep='true'>\n" +
                    "          <arg line='-maxWaitTimeSecs 60 -oldmaxWaitTimeSecs 2400' />\n" +
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

        def created = JobUtils.createJob(projectName, xmlJob, client)
        assert created.successful
        CreateJobResponse jobCreatedResponse = mapper.readValue(
                created.body().string(),
                CreateJobResponse.class
        )
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

        Execution execRes1 = mapper.readValue(exec1.body().string(), Execution.class)
        String execId1 = execRes1.id
        Execution execRes2 = mapper.readValue(exec2.body().string(), Execution.class)
        String execId2 = execRes2.id
        Execution execRes3 = mapper.readValue(exec3.body().string(), Execution.class)
        String execId3 = execRes3.id

        Execution execStatus1 = JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                execId1,
                mapper,
                client,
                WaitingTime.MODERATE,
                WaitingTime.EXCESSIVE
        )

        Execution execStatus2 = JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                execId2,
                mapper,
                client,
                WaitingTime.MODERATE,
                WaitingTime.EXCESSIVE
        )

        Execution execStatus3 = JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                execId3,
                mapper,
                client,
                WaitingTime.MODERATE,
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
        ObjectMapper mapper = new ObjectMapper()

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

        def created = JobUtils.createJob(projectName, xmlJob, client)
        assert created.successful

        when: "TEST: job/id/run should succeed"
        CreateJobResponse jobCreatedResponse = mapper.readValue(
                created.body().string(),
                CreateJobResponse.class
        )

        def jobId = jobCreatedResponse.succeeded[0]?.id

        def optionA = 'a'
        Object optionsToMap = [
                "options": [
                        opt2: optionA
                ]
        ]

        def runResponse = JobUtils.executeJobWithOptions(jobId, client, optionsToMap)
        assert runResponse.successful

        Execution execId = mapper.readValue(runResponse.body().string(), Execution.class)

        Execution jobExecStatusAfterSuccess = JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                execId.id as String,
                mapper,
                client,
                WaitingTime.MODERATE,
                WaitingTime.EXCESSIVE
        )

        then:
        jobExecStatusAfterSuccess.status == ExecutionStatus.SUCCEEDED.state

        when: "TEST: execution output"
        def system = doGet("/system/info")
        SystemInfo systemInfo = mapper.readValue(system.body().string(), SystemInfo.class)

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
        String[] lines = execOutputString.split("\n");
        List<String> outputContent = new ArrayList<>(Arrays.asList(lines))

        then:
        Arrays.equals(expectedContent.toArray(), outputContent.toArray())
    }

    def "test-workflow-errorhandler"(){
        def client = getClient()
        def mapper = new ObjectMapper()
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
        def created1 = JobUtils.createJob(projectName, job1Xml, client)
        assert created1.successful

        CreateJobResponse jobCreatedResponse1 = mapper.readValue(
                created1.body().string(),
                CreateJobResponse.class
        )
        def jobId = jobCreatedResponse1.succeeded[0]?.id

        def runResponse = JobUtils.executeJob(jobId, client)
        assert runResponse.successful

        Execution execId = mapper.readValue(runResponse.body().string(), Execution.class)

        assert JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                execId.id as String,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        ).status == ExecutionStatus.SUCCEEDED.state

        // We read the output file
        def execArgs = "cat $stepOutfile"
        def readResponse = client.doPostWithoutBody("/project/$projectName/run/command?exec=${execArgs}")
        def readResponseBody = readResponse.body().string()
        def parsedReadBody = mapper.readValue(readResponseBody, RunCommand.class)
        String readExecId = parsedReadBody.execution.id

        assert JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                readExecId,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        ).status == ExecutionStatus.SUCCEEDED.state

        def entries = getExecutionOutput(readExecId)

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
        def created2 = JobUtils.createJob(projectName, job2Xml, client)
        assert created2.successful

        CreateJobResponse jobCreatedResponse2 = mapper.readValue(
                created2.body().string(),
                CreateJobResponse.class
        )
        def jobId2 = jobCreatedResponse2.succeeded[0]?.id

        def runResponse2 = JobUtils.executeJob(jobId2, client)
        // The job will execute, so the response will be successful
        assert runResponse2.successful

        Execution execId2 = mapper.readValue(runResponse2.body().string(), Execution.class)

        // But the execution will fail
        assert JobUtils.waitForExecutionToBe(
                ExecutionStatus.FAILED.state,
                execId2.id as String,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        ).status == ExecutionStatus.FAILED.state

        // So we read the output file
        def execArgs2 = "cat $stepOutfile2"
        def readResponse2 = client.doPostWithoutBody("/project/$projectName/run/command?exec=${execArgs2}")
        def readResponseBody2 = readResponse2.body().string()
        def parsedReadBody2 = mapper.readValue(readResponseBody2, RunCommand.class)
        String readExecId2 = parsedReadBody2.execution.id

        assert JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                readExecId2,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        ).status == ExecutionStatus.SUCCEEDED.state

        def entries2 = getExecutionOutput(readExecId2)

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
        def created3 = JobUtils.createJob(projectName, job3Xml, client)
        assert created3.successful

        CreateJobResponse jobCreatedResponse3 = mapper.readValue(
                created3.body().string(),
                CreateJobResponse.class
        )
        def jobId3 = jobCreatedResponse3.succeeded[0]?.id

        def runResponse3 = JobUtils.executeJob(jobId3, client)
        // The job will execute, so the response will be successful
        assert runResponse3.successful

        Execution execId3 = mapper.readValue(runResponse3.body().string(), Execution.class)

        // But the execution will fail
        assert JobUtils.waitForExecutionToBe(
                ExecutionStatus.FAILED.state,
                execId3.id as String,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        ).status == ExecutionStatus.FAILED.state

        // So we read the output file
        def execArgs3 = "cat $stepOutfile3"
        def readResponse3 = client.doPostWithoutBody("/project/$projectName/run/command?exec=${execArgs3}")
        def readResponseBody3 = readResponse3.body().string()
        def parsedReadBody3 = mapper.readValue(readResponseBody3, RunCommand.class)
        String readExecId3 = parsedReadBody3.execution.id

        assert JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                readExecId3,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        ).status == ExecutionStatus.SUCCEEDED.state

        def entries3 = getExecutionOutput(readExecId3)

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
        def created4 = JobUtils.createJob(projectName, job4Xml, client)
        assert created4.successful

        CreateJobResponse jobCreatedResponse4 = mapper.readValue(
                created4.body().string(),
                CreateJobResponse.class
        )
        def jobId4 = jobCreatedResponse4.succeeded[0]?.id

        def runResponse4 = JobUtils.executeJob(jobId4, client)
        // The job will execute, so the response will be successful
        assert runResponse4.successful

        Execution execId4 = mapper.readValue(runResponse4.body().string(), Execution.class)

        // But the execution will fail
        assert JobUtils.waitForExecutionToBe(
                ExecutionStatus.FAILED.state,
                execId4.id as String,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        ).status == ExecutionStatus.FAILED.state

        // So we read the output file
        def execArgs4 = "cat $stepOutfile4"
        def readResponse4 = client.doPostWithoutBody("/project/$projectName/run/command?exec=${execArgs4}")
        def readResponseBody4 = readResponse4.body().string()
        def parsedReadBody4 = mapper.readValue(readResponseBody4, RunCommand.class)
        String readExecId4 = parsedReadBody4.execution.id

        assert JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                readExecId4,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        ).status == ExecutionStatus.SUCCEEDED.state

        def entries4 = getExecutionOutput(readExecId4)

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
        def created5 = JobUtils.createJob(projectName, job5Xml, client)
        assert created5.successful

        CreateJobResponse jobCreatedResponse5 = mapper.readValue(
                created5.body().string(),
                CreateJobResponse.class
        )
        def jobId5 = jobCreatedResponse5.succeeded[0]?.id

        def runResponse5 = JobUtils.executeJob(jobId5, client)
        // The job will execute, so the response will be successful
        assert runResponse5.successful

        Execution execId5 = mapper.readValue(runResponse5.body().string(), Execution.class)

        // But the execution will fail
        assert JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                execId5.id as String,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        ).status == ExecutionStatus.SUCCEEDED.state

        // So we read the output file
        def execArgs5 = "cat $stepOutfile5"
        def readResponse5 = client.doPostWithoutBody("/project/$projectName/run/command?exec=${execArgs5}")
        def readResponseBody5 = readResponse5.body().string()
        def parsedReadBody5 = mapper.readValue(readResponseBody5, RunCommand.class)
        String readExecId5 = parsedReadBody5.execution.id

        assert JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                readExecId5,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        ).status == ExecutionStatus.SUCCEEDED.state

        def entries5 = getExecutionOutput(readExecId5)

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
        ObjectMapper mapper = new ObjectMapper()
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

        def created = JobUtils.createJob(projectName, testXml, client)
        assert created.successful

        when: "Job and referenced job created"
        CreateJobResponse jobCreatedResponse = mapper.readValue(
                created.body().string(),
                CreateJobResponse.class
        )
        def childJob = jobCreatedResponse.succeeded[0]?.id
        def parentJob = jobCreatedResponse.succeeded[1]?.id

        then:
        childJob != null
        parentJob != null

        when: "run job"
        def jobRun = JobUtils.executeJob(parentJob, client)
        assert jobRun.successful

        Execution exec = mapper.readValue(jobRun.body().string(), Execution.class)

        Execution jobExecutionStatus = JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                exec.id as String,
                mapper,
                client,
                WaitingTime.MODERATE,
                WaitingTime.EXCESSIVE
        )

        then:
        then:
        jobExecutionStatus.status == ExecutionStatus.SUCCEEDED.state
        // Verify that all steps on the first (and the only) node are in the SUCCEEDED state
        client.jsonValue(client.doGetAcceptAll("/execution/$jobExecutionStatus.id/state").body(), Map).nodes.values()[0].every({val -> val.executionState == "SUCCEEDED"} )
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
        hold 10 //This is to allow the executions to finish
        deleteProject(projectName)
    }

}
