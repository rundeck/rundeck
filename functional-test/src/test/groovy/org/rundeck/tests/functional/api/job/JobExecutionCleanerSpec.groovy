package org.rundeck.tests.functional.api.job

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class JobExecutionCleanerSpec extends BaseContainer {

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
            sleep 40000
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

}
