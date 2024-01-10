package org.rundeck.tests.functional.api.execution

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.tests.functional.api.execution.apiResponseModels.CreateJobResponse
import org.rundeck.tests.functional.api.execution.apiResponseModels.JobExecutionsResponse
import org.rundeck.util.annotations.APITest
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
        client.apiVersion = 40
        // create project
        def responseProject = client.doPost("/projects", projectJsonMap)
        assert responseProject.successful
        def jobName1 = "xmljob"
        def jobXml1 = "<joblist>\n" +
                "   <job>\n" +
                "      <name>${jobName1}</name>\n" +
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
        def jobName2 = "xmljob2"
        def jobXml2 = "<joblist>\n" +
                "   <job>\n" +
                "      <name>${jobName2}</name>\n" +
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
        def response1 = createJob(projectName, jobXml1)
        ObjectMapper mapper = new ObjectMapper()
        CreateJobResponse parsedResponse = mapper.readValue(response1.body().string(), CreateJobResponse.class)
        def response2 = createJob(projectName, jobXml2)
        assert response1.successful
        assert response2.successful

        when: "assert_job_execution_count with job 1"
        def executions1 = doGet("/job/${parsedResponse.succeeded[0]?.id}/executions")
        JobExecutionsResponse parsedExecutions = mapper.readValue(executions1.body().string(), JobExecutionsResponse.class)
        then:
        parsedExecutions.executions.size() == 0
    }

    def createJob(
            final String project,
            final String jobDefinitionXml

    ) {
        final String CREATE_JOB_ENDPOINT = "/project/${project}/jobs/import"
        return client.doPostWithRawText(CREATE_JOB_ENDPOINT, "application/xml", jobDefinitionXml)
    }

}