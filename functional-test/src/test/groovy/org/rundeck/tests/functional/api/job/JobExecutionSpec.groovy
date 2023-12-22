package org.rundeck.tests.functional.api.job

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class JobExecutionSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
        setupProject()
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
            def response = client.doGet("/execution/${idExec}/state")
        then:
            verifyAll {
                response.successful
                response.code() == 200
                def json = client.jsonValue(response.body(), Map)
                json.executionState
                json.targetNodes.size() == 1
                json.targetNodes[0] == 'localhost'
            }
    }

    def "POST job/id/run should succeed"() {
        given:
            def client = getClient()
            def pathXmlFile = getClass().getResource("/projects-import/api-test-execution-state.xml").getPath()
            def xmlProjectContent = new File(pathXmlFile).text
            def xmlProject = xmlProjectContent.replaceAll("api-test-execution-state.xml", PROJECT_NAME)
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
        deleteProject(PROJECT_NAME)
    }

}
