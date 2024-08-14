package org.rundeck.tests.functional.api.execution

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.annotations.APITest
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.projects.ProjectUtils
import org.rundeck.util.container.BaseContainer

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@APITest
class ExecutionSpec extends BaseContainer {

    final ObjectMapper mapper = new ObjectMapper()

    def setupSpec(){
        startEnvironment()
        setupProject()
    }

    def cleanup() {
        client.apiVersion = client.finalApiVersion
    }

    def "run command get execution"() {
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

            File tmpjar = createArchiveJarFile(projectName, new File(getClass().getResource("/projects-import/archive-test").getPath()))
        when:
            // create project
            def responseProject = client.doPost("/projects", projectJsonMap)
            // import project
            def responseImport = client.doPut(
                "/project/${projectName}/import?jobUuidOption=remove",
                tmpjar
            )
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
        cleanup:
            tmpjar.delete()
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
        setup:
            def newProject = "test-job-id-success"
            setupProject(newProject)
            def pathFile = JobUtils.updateJobFileToImport("api-test-execution-state.xml",PROJECT_NAME, ["project-name": newProject])
            def responseImport = JobUtils.jobImportFile(newProject, pathFile, client) as Map
        when:
            def jobId = responseImport.succeeded[2].id
            def jobRun = JobUtils.executeJobWithArgs(jobId, client, "-opt1 foobar")
            def execId = jsonValue(jobRun.body()).id
            def response = JobUtils.waitForExecutionToBe(
                    ExecutionStatus.SUCCEEDED.state,
                    execId as String,
                    mapper,
                    client,
                    WaitingTime.LOW,
                    WaitingTime.LOW
            )
            def state = client.get("/execution/${response.id}/state", Map)
        then:
            verifyAll {
                responseImport.succeeded.size() == 3
                response.status == 'succeeded'
                def localnode = state.serverNode
                state.steps[0].nodeStates."${localnode}".executionState == 'SUCCEEDED'
            }
        cleanup:
            deleteProject(newProject)
    }

    def "execution query OK"() {
        given:
            def newProject = "test-executions-query"
            setupProject(newProject)
            def startDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
        when: "run a command 1"
            def params1 = "exec=echo+testing+execution+api"
            def adhoc1 = post("/project/${newProject}/run/command?${params1}", Map)
        then:
            adhoc1.execution.id != null
            def execId1 = adhoc1.execution.id as Integer
        when: "run a command 2"
            def params2 = "exec=echo+testing+adhoc+execution+query+should+fail;false"
            def adhoc2 = post("/project/${newProject}/run/command?${params2}", Map)
        then:
            adhoc2.execution.id != null
            def execId2 = adhoc2.execution.id as Integer
        when:"import jobs 1"
            def pathFile1 = JobUtils.updateJobFileToImport("job-template-common.xml", PROJECT_NAME,["project-name": newProject, "job-name": "test exec query", "job-group-name": "api-test/execquery", "job-description-name": "A job to test the executions query API", "uuid": "api-v5-test-exec-query", "args":"echo hello there"])
        then:
            def jobId1 = JobUtils.jobImportFile(newProject, pathFile1,client).succeeded[0].id
        when:"import jobs 2"
            def pathFile2 = JobUtils.updateJobFileToImport("job-template-common.xml", PROJECT_NAME,["project-name": newProject, "job-name": "second test for exec query", "job-group-name": "api-test/execquery", "job-description-name": "A job to test the executions query API2", "uuid": "api-v5-test-exec-query2", "args":"echo hello there"])
        then:
            def jobId2 = JobUtils.jobImportFile(newProject, pathFile2,client).succeeded[0].id
        when:"run job 1 and 2"
            def jobRun1 = JobUtils.executeJobWithArgs(jobId1, client, "-opt2 a")
            int execId3 = jsonValue(jobRun1.body()).id as Integer
            def jobRun2 = JobUtils.executeJobWithArgs(jobId2, client, "-opt2 a")
            int execId4 = jsonValue(jobRun2.body()).id as Integer
        then: "wait for executions to finish"
            def responseExecId1 = JobUtils.waitForExecutionToBe(
                    ExecutionStatus.SUCCEEDED.state,
                    execId1 as String,
                    mapper,
                    client,
                    WaitingTime.LOW,
                    WaitingTime.LOW
            )
            responseExecId1.status == 'succeeded'
            def responseExecId2 = JobUtils.waitForExecutionToBe(
                    ExecutionStatus.FAILED.state,
                    execId2 as String,
                    mapper,
                    client,
                    WaitingTime.LOW,
                    WaitingTime.LOW
            )
            responseExecId2.status == 'failed'
            def responseExecId3 = JobUtils.waitForExecutionToBe(
                    ExecutionStatus.SUCCEEDED.state,
                    execId3 as String,
                    mapper,
                    client,
                    WaitingTime.LOW,
                    WaitingTime.LOW
            )
            responseExecId3.status == 'succeeded'
            def responseExecId4 = JobUtils.waitForExecutionToBe(
                    ExecutionStatus.SUCCEEDED.state,
                    execId4 as String,
                    mapper,
                    client,
                    WaitingTime.LOW,
                    WaitingTime.LOW
            )
            responseExecId4.status == 'succeeded'
        when: "executions"
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

    def "executions-running for list projects"() {
        given:
            String projectNameSuffix = "project-api-forecast"
            ProjectUtils.createProjectsWithJobsScheduled(projectNameSuffix, 4, 2, client)
        when:
            def response1 = doGet("/project/*/executions/running?includePostponed=true")
            def response2 = doGet("/project/${projectNameSuffix}-1/executions/running?includePostponed=true")
            def response3 = doGet("/project/${projectNameSuffix}-1,${projectNameSuffix}-2/executions/running?includePostponed=true")
            def response4 = doGet("/project/${projectNameSuffix}-1,${projectNameSuffix}-2,${projectNameSuffix}-3/executions/running?includePostponed=true")
        then:
            verifyAll {
                response1.code() == 200
                response2.code() == 200
                response3.code() == 200
                response4.code() == 200
            }
        cleanup:
            (2..4).each {
                updateConfigurationProject("${projectNameSuffix}-${it}", [
                    "project.disable.schedule": "true",
                    "project.later.schedule.enable": "false",
                    "project.disable.executions": "true"
                ])
                hold 10 //Wait until the executions stop
                deleteProject("${projectNameSuffix}-${it}")
            }
    }

    def "executions-running when project is disabled"() {
        given:
            deleteProject("project-api-forecast-1")
        when:
            def response = doGet("/project/project-api-forecast-1/executions/running?includePostponed=true")
        then:
            verifyAll {
                !response.successful
                response.code() == 404
                def json2 = jsonValue(response.body())
                json2.errorCode == 'api.error.project.disabled'
            }
    }

    def "executions-running when project is deleted"() {
        when:
            def response = doGet("/project/any-api-project/executions/running?includePostponed=true")
        then:
            verifyAll {
                !response.successful
                response.code() == 404
                def json2 = jsonValue(response.body())
                json2.errorCode == 'api.error.item.doesnotexist'
            }
    }

    void testExecQuery(String xargs = null, Integer expect = null) {
        def url = "/project/test-executions-query/executions"
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