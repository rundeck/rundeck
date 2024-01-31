package org.rundeck.tests.functional.api.project

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class ImportSpec extends BaseContainer {

    def "test-project-import-readme-motd.sh"(){
        given:
        String projectName = "motdTest"
        Object projectJsonMap = [
                "name": projectName,
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
        client.apiVersion = 14
        def responseProject = client.doPost("/projects", projectJsonMap)
        assert responseProject.successful
        def responseImport = client.doPut(
                "/project/${projectName}/import?jobUuidOption=remove&importConfig=true",
                new File(getClass().getResource("/projects-import/archive-test-readme.zip").getPath()))
        responseImport.successful

        when: "We try to read readme content"
        def readmeResponse = client.doGetAcceptAll("/project/${projectName}/readme.md")
        assert readmeResponse.successful
        String readmeContent = readmeResponse.body().string()

        then: "The content: "
        readmeContent != null
        readmeContent.contains("this is a readme file")

        when: "We try to read motd content"
        def motdResponse = client.doGetAcceptAll("/project/${projectName}/motd.md")
        assert motdResponse.successful
        String motdContent = motdResponse.body().string()

        then: "The content: "
        motdContent != null
        motdContent.contains("this is a message of the day")

        cleanup:
        deleteProject(projectName)
    }

}
