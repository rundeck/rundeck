package org.rundeck.tests.functional.api.execution

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.tests.functional.api.ResponseModels.CreateJobResponse
import org.rundeck.tests.functional.api.ResponseModels.Execution
import org.rundeck.tests.functional.api.ResponseModels.JobExecutionsResponse
import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.ExecutionStatus
import org.rundeck.util.api.JobUtils
import org.rundeck.util.api.WaitingTime
import org.rundeck.util.container.BaseContainer
import org.rundeck.util.container.RdClient

import java.util.concurrent.TimeUnit

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@APITest
class ExecutionSpec extends BaseContainer {

    def setupSpec(){
        startEnvironment()
        setupProject()
    }

    def "run command, get execution"() {
        when: "run a command"
            def adhoc = post("/project/${PROJECT_NAME}/run/command?exec=echo+testing+execution+api", Map)
        then:
            adhoc.execution != null
            adhoc.execution.id != null
        when: "get execution"
            def execid = adhoc.execution.id
            Map exec = get("/execution/${execid}", Map)
        then:
            exec.id == execid
            exec.href != null
            exec.permalink != null
            exec.status != null
            exec.project == PROJECT_NAME
            exec.user == 'admin'
    }

    def "get execution not found"() {
        when:
            def execid = '9999'
            def response = doGet("/execution/${execid}")
        then:
            !response.successful
            response.code() == 404
    }

    def "get execution output not found"() {
        when:
            def execid = '9999'
            def response = doGet("/execution/${execid}/output")
        then:
            !response.successful
            response.code() == 404
    }

    def "get execution output unsupported version"() {
        when:
            def execid = '1'
            def client = clientProvider.client
            client.apiVersion = 5
            def response = client.doGet("/execution/${execid}/output")
        then:
            !response.successful
            response.code() == 400
            jsonValue(response.body()).errorCode == 'api.error.api-version.unsupported'
    }

    def "delete execution not found"() {
        when:
            def execid = '9999'
            def response = doDelete("/execution/${execid}")
        then:
            response.code() == 404
            jsonValue(response.body()).errorCode == 'api.error.item.doesnotexist'
    }

    def "import project with configs and clean executions"() {
        given:
            // define project name and configs
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
            def client = getClient()
            client.apiVersion = version
        when:
            // create project
            def responseProject = client.doPost("/projects", projectJsonMap)
            // import project
            def responseImport = client.doPut(
                "/project/${projectName}/import?jobUuidOption=remove",
                new File(getClass().getResource("/projects-import/archive-test.zip").getPath()))
            // get executions
            def response = client.doGet("/project/${projectName}/executions")
        then:
            // verify if project was created, job imported and 6 executions were created
            verifyAll {
                responseProject.successful
                responseProject.code() == 201
                def json = client.jsonValue(responseProject.body(), Map)
                json.name == projectName

                responseImport.successful
                responseImport.code() == 200
                def json1 = client.jsonValue(responseImport.body(), Map)
                json1.import_status == 'successful'

                response.successful
                response.code() == 200
                def json2 = client.jsonValue(response.body(), Map)
                json2.executions.size() == 6
            }
            // wait for executions to finish
            sleep 60000
            // get executions
            def responseClean = client.doGet("/project/${projectName}/executions")
            // verify if executions were cleaned
            verifyAll {
                responseClean.successful
                responseClean.code() == 200
                def json3 = jsonValue(responseClean.body())
                json3.executions.size() == 0
            }
            deleteProject(projectName)
        where:
            version | projectName
            14      | "APIImportAndCleanHistoryTest"
            45      | "APIImportAndCleanHistoryTest45"

    }

    def "execution state not found"() {
        when:
            def response = doGet("/execution/000/state")
        then:
            verifyAll {
                !response.successful
                response.code() == 404
                def json = jsonValue(response.body())
                json.errorCode == 'api.error.item.doesnotexist'
                json.message == 'Execution does not exist: 000'
            }
    }

    def "execution state OK"() {
        when:
            def runCommand = post("/project/${PROJECT_NAME}/run/command?exec=echo+testing+execution+api", null, Map)
            def idExec = runCommand.execution.id
            sleep 5000
            def response = doGet("/execution/${idExec}/state")
        then:
            verifyAll {
                response.successful
                response.code() == 200
                def json = jsonValue(response.body())
                json.executionState
                json.targetNodes.size() == 1
            }
    }

    def "POST job/id/run should succeed"() {
        given:
            def pathFile = updateFile("api-test-execution-state.xml")
            def responseImport = jobImportFile(pathFile)
        when:
            def jobId = responseImport.succeeded[2].id
            def output = runJobAndWait(jobId, ["options":["opt1": "foobar"]])
            def state = client.get("/execution/${output.id}/state", Map)
        then:
            verifyAll {
                responseImport.succeeded.size() == 3
                output.execState == 'succeeded'
                def localnode = state.serverNode
                state.steps[0].nodeStates."${localnode}".executionState == 'SUCCEEDED'
            }
    }

    def "execution query OK"() {
        given:
            def newProject = "test-executions-query"
            setupProject(newProject)
        when: "run a command 1"
            def params1 = "exec=echo+testing+execution+api"
            def adhoc1 = post("/project/${newProject}/run/command?${params1}", Map)
        then:
            adhoc1.execution.id != null
            def execId1 = adhoc1.execution.id
        when: "run a command 2"
            def params2 = "exec=echo+testing+adhoc+execution+query+should+fail;false"
            def adhoc2 = post("/project/${newProject}/run/command?${params2}", Map)
        then:
            adhoc2.execution.id != null
            def execId2 = adhoc2.execution.id
        when:"import jobs 1"
            def pathFile1 = updateFile("job-template-common.xml", newProject, "test exec query", "api-test/execquery", "A job to test the executions query API", null, "api-v5-test-exec-query")
        then:
            def jobId1 = jobImportFile(newProject, pathFile1).succeeded[0].id
        when:"import jobs 2"
            def pathFile2 = updateFile("job-template-common.xml", newProject, "second test for exec query", "api-test/execquery", "A job to test the executions query API2", null, "api-v5-test-exec-query2")
        then:
            def jobId2 = jobImportFile(newProject, pathFile2).succeeded[0].id
        when:"run job 1 and 2"
            def execId3 = runJob(jobId1, ["options":["opt2": "a"]])
            def execId4 = runJob(jobId2, ["options":["opt2": "a"]])
        then:
            obtainExecution(execId1).execState == 'succeeded'
            obtainExecution(execId2).execState == 'failed'
            obtainExecution(execId3).execState == 'succeeded'
            obtainExecution(execId4).execState == 'succeeded'
        when: "executions"
            def startDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
            def fakeDate = "2213-05-08T01:05:19Z"
        then: "begin"
            testExecQuery ""
            testExecQuery "begin=$startDate", 4
            testExecQuery "begin=$fakeDate", 0
        when:
            def baseQuery="begin=$startDate"
        then: "jobIdListFilter"
            testExecQuery "jobIdListFilter=$jobId1&$baseQuery", 1
            testExecQuery "jobIdListFilter=$jobId1&jobIdListFilter=$jobId2&$baseQuery", 2
            testExecQuery "jobIdListFilter=$jobId1+DNE-ID&$baseQuery", 0
        when: "excludeJobIdListFilter"
        then:
            testExecQuery "excludeJobIdListFilter=$jobId1&$baseQuery", 1
            testExecQuery "excludeJobIdListFilter=$jobId2&$baseQuery", 1
            testExecQuery "excludeJobIdListFilter=$jobId1&excludeJobIdListFilter=$jobId2&$baseQuery", 0
            testExecQuery "excludeJobIdListFilter=$jobId1+DNE-ID&$baseQuery", 2
        when: "jobFilter"
        then:
            testExecQuery "jobFilter=test+exec+query&$baseQuery"
            testExecQuery "jobFilter=test+exec+query+DNE&$baseQuery", 0
        when: "jobListFilter"
        then:
            testExecQuery "jobListFilter=api-test%2Fexecquery%2Ftest+exec+query&jobListFilter=api-test%2Fexecquery%2Fsecond+test+for+exec+query&$baseQuery", 2
            testExecQuery "jobListFilter=api-test%2Fexecquery%2FDNE+second+test+for+exec+query&$baseQuery", 0
        when: "excludeJobListFilter"
        then:
            testExecQuery "excludeJobListFilter=api-test%2Fexecquery%2Ftest+exec+query&excludeJobListFilter=api-test%2Fexecquery%2Fsecond+test+for+exec+query&$baseQuery", 0
            testExecQuery "excludeJobListFilter=api-test%2Fexecquery%2Ftest+exec+query&$baseQuery", 1
            testExecQuery "excludeJobListFilter=api-test%2Fexecquery%2FDNE+second+test+for+exec+query&$baseQuery", 2
        when: "jobExactFilter"
        then:
            testExecQuery "jobExactFilter=test+exec+query&$baseQuery"
            testExecQuery "jobExactFilter=test+exec+query+DNE&$baseQuery", 0
        when: "groupPath"
        then:
            testExecQuery "groupPath=api-test%2Fexecquery&$baseQuery"
            testExecQuery "groupPath=api-test%2Fexecquery%2FDNEGROUP&$baseQuery", 0
        when: "groupPathExact"
        then:
            testExecQuery "groupPathExact=api-test%2Fexecquery&$baseQuery"
            testExecQuery "groupPathExact=api-test%2Fexecquery%2FDNEGROUP&$baseQuery", 0
        when: "descFilter"
        then:
            testExecQuery "descFilter=executions+query+API&$baseQuery"
            testExecQuery "descFilter=DNE+description&$baseQuery", 0
        when: "userFilter"
        then:
            testExecQuery "userFilter=admin&$baseQuery"
            testExecQuery "userFilter=DNEUser&$baseQuery", 0
        when: "statusFilter"
        then:
            testExecQuery "statusFilter=succeeded&jobIdListFilter=$jobId1&$baseQuery"
            testExecQuery "statusFilter=aborted&jobIdListFilter=$jobId1&$baseQuery", 0
        when: "adHocFilter"
        then:
            testExecQuery "adhoc=true&$baseQuery", 2
            testExecQuery "adhoc=false&$baseQuery", 2
        deleteProject(newProject)
    }

    def "TEST: executions-running for project test"() {
        when:
            def demo = doGet("/project/${PROJECT_NAME}/executions/running")
        then:
            verifyAll {
                demo.successful
                demo.code() == 200
                def json = jsonValue(demo.body())
                json.executions.size() != null
            }
    }

    void testExecQuery(String xargs = null, Integer expect = null, String project = "test-executions-query") {
        def url = "/project/${project}/executions"
        def response = doGet(xargs ? "${url}?${xargs}" : url)
        def itemCount = getClient().jsonValue(response.body(), Map).executions.size()
        verifyAll {
            response.successful
            response.code() == 200
            if (expect != null && itemCount != 0)
                itemCount == expect
        }
    }

    def "test-job-flip-executionEnabled"(){
        given:
        def projectName = "test-job-flip-executionEnabled"
        def apiVersion = 40
        client.apiVersion = apiVersion
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
        def apiVersion = 40
        client.apiVersion = apiVersion
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
        def apiVersion = 40
        def client = getClient()
        client.apiVersion = apiVersion
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
        def apiVersion = 40
        def client = getClient()
        client.apiVersion = apiVersion
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
        def apiVersion = 40
        def client = getClient()
        client.apiVersion = apiVersion
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

        JobExecutionsResponse JobExecutionStatus = waitForJobExecutionToSucceed(
                jobId as String,
                mapper,
                client,
                WaitingTime.MODERATE.milliSeconds,
                WaitingTime.EXCESSIVE.milliSeconds / 1000 as int
        )

        then:
        JobExecutionStatus.executions[0].status == EXECUTION_SUCCEEDED

        when: "run job test"
        def referencedJobRun = JobUtils.executeJob(refJobId, client)
        assert referencedJobRun.successful

        JobExecutionsResponse refJobExecutionStatus = waitForJobExecutionToSucceed(
                refJobId as String,
                mapper,
                client,
                WaitingTime.MODERATE.milliSeconds,
                WaitingTime.EXCESSIVE.milliSeconds / 1000 as int
        )

        then:
        refJobExecutionStatus.executions[0].status == EXECUTION_SUCCEEDED

    }

    def "test-job-retry.sh"(){
        given:
        def projectName = "test-job-retry"
        def apiVersion = 40
        def client = getClient()
        client.apiVersion = apiVersion
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
        def execDetails = waitForExecutionToBe(
                ExecutionStatus.FAILED_WITH_RETRY.state,
                execId as String,
                mapper,
                client,
                1,
                WaitingTime.MODERATE.milliSeconds
        )
        def retryId1 = execDetails.retriedExecution.id

        then:
        retryId1 > 0

        when: "fail and retry 2"
        def execDetails2 = waitForExecutionToBe(
                ExecutionStatus.FAILED_WITH_RETRY.state,
                retryId1 as String,
                mapper,
                client,
                1,
                WaitingTime.MODERATE.milliSeconds
        )
        def retryId2 = execDetails2.retriedExecution.id

        then:
        retryId2 > 0

        when: "final retry"
        def execDetailsFinal = waitForExecutionToBe(
                ExecutionStatus.FAILED.state,
                retryId2 as String,
                mapper,
                client,
                1,
                WaitingTime.MODERATE.milliSeconds
        )

        then:
        execDetailsFinal.retriedExecution == null
        execDetailsFinal.retryAttempt == 2
        execDetailsFinal.status == ExecutionStatus.FAILED.state

    }

    JobExecutionsResponse waitForJobExecutionToSucceed(
            String jobId,
            ObjectMapper mapper,
            RdClient client,
            int iterationGap,
            int timeout
    ){
        JobExecutionsResponse executionStatus
        def refJobExec = client.doGet("/job/${jobId}/executions")
        executionStatus = mapper.readValue(refJobExec.body().string(), JobExecutionsResponse.class)
        long initTime = System.currentTimeMillis()
        while(executionStatus.executions[0].status == ExecutionStatus.SUCCEEDED.state){
            if ((System.currentTimeMillis() - initTime) >= TimeUnit.SECONDS.toMillis(timeout)) {
                throw new InterruptedException("Timeout reached (${timeout} seconds).")
            }
            def transientExecutionResponse = doGet("/job/${jobId}/executions")
            executionStatus = mapper.readValue(transientExecutionResponse.body().string(), JobExecutionsResponse.class)
            Thread.sleep(iterationGap)
        }
        return executionStatus
    }

    Execution waitForExecutionToBe(
            String state,
            String executionId,
            ObjectMapper mapper,
            RdClient client,
            int iterationGap,
            int timeout
    ){
        Execution executionStatus
        def execDetail = client.doGet("/execution/${executionId}")
        executionStatus = mapper.readValue(execDetail.body().string(), Execution.class)
        long initTime = System.currentTimeMillis()
        while(executionStatus.status != state){
            if ((System.currentTimeMillis() - initTime) >= TimeUnit.SECONDS.toMillis(timeout)) {
                throw new InterruptedException("Timeout reached (${timeout} seconds).")
            }
            def transientExecutionResponse = doGet("/execution/${executionId}")
            executionStatus = mapper.readValue(transientExecutionResponse.body().string(), Execution.class)
            Thread.sleep(iterationGap)
        }
        return executionStatus
    }

    def createSampleProject = (String projectName, Object projectJsonMap) -> {
        return client.doPost("/projects", projectJsonMap)
    }

}