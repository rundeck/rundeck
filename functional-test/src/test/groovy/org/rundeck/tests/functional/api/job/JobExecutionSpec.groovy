package org.rundeck.tests.functional.api.job

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@APITest
class JobExecutionSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
        setupProject()
        setupProject("test-executions-query")
    }

    def "import project with configs and clean executions"() {
        given:
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
            def responseProject = client.doPost("/projects", projectJsonMap)
            def responseImport = client.doPut(
                    "/project/${projectName}/import?jobUuidOption=remove",
                    new File(getClass().getResource("/projects-import/archive-test.zip").getPath()))
            def response = client.doGet("/project/${projectName}/executions")
        then:
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
            sleep 50000
            def responseClean = client.doGet("/project/${projectName}/executions")
            verifyAll {
                responseClean.successful
                responseClean.code() == 200
                def json3 = client.jsonValue(responseClean.body(), Map)
                json3.executions.size() == 0
            }
            deleteProject(projectName)
        where:
            version | projectName
            14      | "APIImportAndCleanHistoryTest"
            45      | "APIImportAndCleanHistoryTest45"

    }

    def "execution state not found"() {
        given:
            def client = getClient()
        when:
            def response = client.doGet("/execution/000/state")
        then:
            verifyAll {
                !response.successful
                response.code() == 404
                def json = client.jsonValue(response.body(), Map)
                json.errorCode == 'api.error.item.doesnotexist'
                json.message == 'Execution does not exist: 000'
            }
    }

    def "execution state OK"() {
        given:
            def client = getClient()
        when:
            def runCommand = client.post("/project/${PROJECT_NAME}/run/command?exec=echo+testing+execution+api", null, Map)
            def idExec = runCommand.execution.id
            sleep 5000
            def response = client.doGet("/execution/${idExec}/state")
        then:
            verifyAll {
                response.successful
                response.code() == 200
                def json = client.jsonValue(response.body(), Map)
                json.executionState
                json.targetNodes.size() == 1
            }
    }

    def "POST job/id/run should succeed"() {
        given:
            def client = getClient()
            def pathXmlFile = getClass().getResource("/projects-import/api-test-execution-state.xml").getPath()
            def xmlProjectContent = new File(pathXmlFile).text
            def xmlProject = xmlProjectContent.replaceAll('xml-project-name', PROJECT_NAME)
            new File(pathXmlFile).text = xmlProject
        when:
            def responseImport = client.doPost("/project/${PROJECT_NAME}/jobs/import", new File(pathXmlFile), "application/xml")
            def json = client.jsonValue(responseImport.body(), Map)
            def jobId = json.succeeded[2].id
            def output = runJobAndWait(jobId, ["options":["opt1": "foobar"]])
            def state = client.get("/execution/${output.id}/state", Map)
        then:
            verifyAll {
                responseImport.successful
                responseImport.code() == 200
                json.succeeded.size() == 3
                output.execState == 'succeeded'
                def localnode = state.serverNode
                state.steps[0].nodeStates."${localnode}".executionState == 'SUCCEEDED'
            }
    }

    def "execution query OK"() {
        when: "run a command 1"
            def newProject = "test-executions-query"
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
            def pathXmlFile = getClass().getResource("/projects-import/test-executions-query.xml").getPath()
            def xmlProjectContent = new File(pathXmlFile).text
            def xmlProject1 = xmlProjectContent.replaceAll('xml-project-name', newProject).replaceAll('xml-args', "echo hello there")
            new File(pathXmlFile).text = xmlProject1
        then:
            def jobId1 = jobImportFile(newProject, pathXmlFile).succeeded[0].id
        when:"import jobs 2"
            def pathXmlFile1 = getClass().getResource("/projects-import/test-executions-query-2.xml").getPath()
            def xmlProjectContent1 = new File(pathXmlFile1).text
            def xmlProject2 = xmlProjectContent1.replaceAll('xml-project-name', newProject).replaceAll('xml-args', "echo hello there")
            new File(pathXmlFile1).text = xmlProject2
        then:
            def jobId2 = jobImportFile(newProject, pathXmlFile1).succeeded[0].id
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

}
