package org.rundeck.tests.functional.api.execution

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.tests.functional.api.execution.apiResponseModels.CreateJobResponse
import org.rundeck.tests.functional.api.execution.apiResponseModels.JobExecutionsResponse
import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.WaitingTime
import org.rundeck.util.container.BaseContainer

@APITest
class ExecutionSpec extends BaseContainer {

    def setupSpec(){
        startEnvironment()
        setupProject()
    }

    def "run command, get execution"() {
        when:
        def adhoc = post("/project/${PROJECT_NAME}/run/command?exec=echo+testing+execution+api", Map)
        then:
        adhoc.execution != null
        adhoc.execution.id != null
        when:
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

    def "test-job-flip-executionEnabled-bulk"(){
        given:
        def projectName = "project-test"
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

        def responseProject = client.doPost("/projects", projectJsonMap)
        assert responseProject.successful

        def jobName1 = "xmljob"
        def jobXml1 = generateExecForEnabledXmlTest(jobName1)

        def jobName2 = "xmljob2"
        def jobXml2 = generateExecForEnabledXmlTest(jobName2)

        def job1CreatedResponse = createJob(projectName, jobXml1)
        assert job1CreatedResponse.successful

        def job2CreatedResponse = createJob(projectName, jobXml2)
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
        def jobExecResponseFor1 = executeJob job1Id
        assert jobExecResponseFor1.successful
        def executions1AfterExecResponse = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1AfterExec = mapper.readValue(executions1AfterExecResponse.body().string(), JobExecutionsResponse.class)
        then: "assert_job_execution_count job1"
        parsedExecutionsResponseForExecution1AfterExec.executions.size() == 1

        when: "execute_job 2"
        def jobExecResponseFor2 = executeJob job2Id
        assert jobExecResponseFor2.successful
        def executions2AfterExecResponse = doGet("/job/${job2Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution2AfterExec = mapper.readValue(executions2AfterExecResponse.body().string(), JobExecutionsResponse.class)
        then: "assert_job_execution_count job1"
        parsedExecutionsResponseForExecution2AfterExec.executions.size() == 1

        when: "TEST: bulk job execution disable"
        def idList = "idlist=${job1Id},${job2Id}"
        def disabledJobsResponse = doPost("/jobs/execution/disable?idlist=${idList}", "{}")
        assert disabledJobsResponse.successful

        def executionsForJob1AfterDisable = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1AfterDisable = mapper.readValue(executionsForJob1AfterDisable.body().string(), JobExecutionsResponse.class)

        def executionsForJob2AfterDisable = doGet("/job/${job2Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution2AfterDisable = mapper.readValue(executionsForJob2AfterDisable.body().string(), JobExecutionsResponse.class)

        then:
        parsedExecutionsResponseForExecution1AfterDisable.executions.size() == 1
        parsedExecutionsResponseForExecution2AfterDisable.executions.size() == 1

        when: "TEST: bulk job execution enable"
        def enabledJobsResponse = doPost("/jobs/execution/enable?idlist=${idList}", "{}")
        assert enabledJobsResponse.successful

        // Necessary since the api needs to breathe after enable execs
        Thread.sleep(WaitingTime.LOW.milliSeconds)

        def jobExecResponseFor1AfterEnable = executeJob job1Id
        assert jobExecResponseFor1AfterEnable.successful

        def jobExecResponseFor2AfterEnable = executeJob job2Id
        assert jobExecResponseFor2AfterEnable.successful

        def executionsForJob1AfterEnable = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1AfterEnable = mapper.readValue(executionsForJob1AfterEnable.body().string(), JobExecutionsResponse.class)

        def executionsForJob2AfterEnable = doGet("/job/${job2Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution2AfterEnable = mapper.readValue(executionsForJob2AfterEnable.body().string(), JobExecutionsResponse.class)

        then:
        parsedExecutionsResponseForExecution1AfterEnable.executions.size() == 2
        parsedExecutionsResponseForExecution2AfterEnable.executions.size() == 2

    }

    def executeJob = (jobId) -> {
        return doPost("/job/${jobId}/run", "{}")
    }

    def createJob(
            final String project,
            final String jobDefinitionXml

    ) {
        final String CREATE_JOB_ENDPOINT = "/project/${project}/jobs/import"
        return client.doPostWithRawText(CREATE_JOB_ENDPOINT, "application/xml", jobDefinitionXml)
    }

    private def generateExecForEnabledXmlTest(String jobName){
        return  "<joblist>\n" +
                "   <job>\n" +
                "      <name>${jobName}</name>\n" +
                "      <group>api-test</group>\n" +
                "      <description></description>\n" +
                "      <loglevel>INFO</loglevel>\n" +
                "      <dispatch>\n" +
                "        <threadcount>1</threadcount>\n" +
                "        <keepgoing>true</keepgoing>\n" +
                "      </dispatch>\n" +
                "      <sequence>\n" +
                "        <command>\n" +
                "        <exec>echo hello there</exec>\n" +
                "        </command>\n" +
                "      </sequence>\n" +
                "   </job>\n" +
                "</joblist>"
    }

}