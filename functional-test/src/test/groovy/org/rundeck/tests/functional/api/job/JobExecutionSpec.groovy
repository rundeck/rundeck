package org.rundeck.tests.functional.api.job

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.tests.functional.api.ResponseModels.CreateJobResponse
import org.rundeck.tests.functional.api.ResponseModels.ErrorResponse
import org.rundeck.tests.functional.api.ResponseModels.Execution
import org.rundeck.tests.functional.api.ResponseModels.JobExecutionsResponse
import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.ExecutionStatus
import org.rundeck.util.api.JobUtils
import org.rundeck.util.api.WaitingTime
import org.rundeck.util.container.BaseContainer
import spock.lang.Shared
import spock.lang.Stepwise

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

        def responseProject = createSampleProject(projectName, projectJsonMap)
        assert responseProject.successful

        def jobName1 = "xmljob"
        def jobXml1 = JobUtils.generateExecForEnabledXmlTest(jobName1)

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
        assert jobExecResponseFor1AfterDisable.code() == 500 // bc execs are disabled

        def executionsForJob1AfterDisable = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1AfterDisable = mapper.readValue(executionsForJob1AfterDisable.body().string(), JobExecutionsResponse.class)

        then:
        parsedExecutionsResponseForExecution1AfterDisable.executions.size() == 1

        when: "TEST: when execution is off and then on again, job does execute"
        def enabledJobsResponse = client.doPostWithoutBody("/job/${job1Id}/execution/enable")
        assert enabledJobsResponse.successful

        // Necessary since the api needs to breathe after enable execs
        Thread.sleep(WaitingTime.LOW.milliSeconds)

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

        def responseProject = createSampleProject(projectName, projectJsonMap)
        assert responseProject.successful

        def jobName1 = "xmljob"
        def jobXml1 = JobUtils.generateExecForEnabledXmlTest(jobName1)

        def jobName2 = "xmljob2"
        def jobXml2 = JobUtils.generateExecForEnabledXmlTest(jobName2)

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
        assert jobExecResponseFor1AfterDisable.code() == 500 // bc execs are disabled

        def jobExecResponseFor2AfterDisable = JobUtils.executeJob(job2Id, client)
        assert jobExecResponseFor2AfterDisable.code() == 500  // bc execs are disabled

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
        Thread.sleep(WaitingTime.LOW.milliSeconds)

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

        def responseProject = createSampleProject(projectName, projectJsonMap)
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

        Thread.sleep(WaitingTime.LOW.milliSeconds) // As the original test says

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

        def responseProject = createSampleProject(projectName, projectJsonMap)
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

        def responseProject = createSampleProject(projectName, projectJsonMap)
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
                WaitingTime.MODERATE.milliSeconds,
                WaitingTime.EXCESSIVE.milliSeconds / 1000 as int
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
                WaitingTime.MODERATE.milliSeconds,
                WaitingTime.EXCESSIVE.milliSeconds / 1000 as int
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

        def responseProject = createSampleProject(projectName, projectJsonMap)
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
                WaitingTime.LOW.milliSeconds / 1000 as int,
                WaitingTime.MODERATE.milliSeconds
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
                WaitingTime.LOW.milliSeconds / 1000 as int,
                WaitingTime.MODERATE.milliSeconds
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
                WaitingTime.LOW.milliSeconds / 1000 as int,
                WaitingTime.MODERATE.milliSeconds
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

        def responseProject = createSampleProject(projectName, projectJsonMap)
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
                WaitingTime.MODERATE.milliSeconds,
                WaitingTime.EXCESSIVE.milliSeconds / 1000 as int
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
                WaitingTime.LOW.milliSeconds,
                WaitingTime.MODERATE.milliSeconds / 1000 as int
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
                WaitingTime.LOW.milliSeconds,
                WaitingTime.MODERATE.milliSeconds / 1000 as int
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
                WaitingTime.LOW.milliSeconds,
                WaitingTime.MODERATE.milliSeconds / 1000 as int
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
                WaitingTime.LOW.milliSeconds,
                WaitingTime.MODERATE.milliSeconds / 1000 as int
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

    def generateRuntime(int secondsInFuture){
        TimeZone timeZone = TimeZone.getDefault()
        Calendar cal = Calendar.getInstance(timeZone)
        cal.add(Calendar.SECOND, secondsInFuture)
        SimpleDateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"))
        return [string: iso8601Format.format(cal.time), date: cal.time]
    }

    def createSampleProject = (String projectName, Object projectJsonMap) -> {
        return client.doPost("/projects", projectJsonMap)
    }

}
