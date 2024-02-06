package org.rundeck.tests.functional.api.project

import org.rundeck.tests.functional.api.ResponseModels.ConfigProperty
import org.rundeck.tests.functional.api.ResponseModels.ProjectCreateResponse
import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper

@APITest
class ConfigSpec extends BaseContainer{

    def "test GET /api/14/project/name/config/key"(){
        given:
        def client = getClient()
        client.apiVersion = 14 // as the original test
        def projectName = "testConfigJson"
        Object testProperties = [
                "name": projectName,
                "config": [
                        "test.property": "test value",
                        "test.property2": "test value2"
                ]
        ]
        def mapper = new ObjectMapper()

        when:
        def response = client.doPost(
                "/projects",
                testProperties
        )
        assert response.successful
        ProjectCreateResponse parsedResponse = mapper.readValue(
                response.body().string(),
                ProjectCreateResponse.class
        )

        then:
        parsedResponse.name != null
        parsedResponse.name == projectName

        parsedResponse.config."test.property" == "test value"
        parsedResponse.config."test.property2" == "test value2"
        !parsedResponse.config."test.property3"

        when: "TEST: GET config"
        def responseForProp1 = doGet("/project/${projectName}/config/test.property")
        assert responseForProp1.successful
        ConfigProperty prop1 = mapper.readValue(responseForProp1.body().string(), ConfigProperty.class)

        def responseForProp2 = doGet("/project/${projectName}/config/test.property2")
        assert responseForProp2.successful
        ConfigProperty prop2 = mapper.readValue(responseForProp2.body().string(), ConfigProperty.class)

        then:
        prop1.key == "test.property"
        prop1.value == "test value"

        prop2.key == "test.property2"
        prop2.value == "test value2"

        when: "TEST: PUT (update) config"
        def updatedValueForProp1 = "A better value"
        def updatedBodyForProp1 = [ "key":"test.property", "value":updatedValueForProp1 ]

        def updatedValueForProp2 = "A better value2"
        def updatedBodyForProp2 = [ "key":"test.property2", "value":updatedValueForProp2 ]

        def updated1 = client.doPutWithJsonBody("/project/${projectName}/config/test.property", updatedBodyForProp1)
        assert updated1.successful

        def updated2 = client.doPutWithJsonBody("/project/${projectName}/config/test.property2", updatedBodyForProp2)
        assert updated2.successful

        def responseForUpdatedProp1 = doGet("/project/${projectName}/config/test.property")
        assert responseForUpdatedProp1.successful

        def responseForUpdatedProp2 = doGet("/project/${projectName}/config/test.property2")
        assert responseForUpdatedProp1.successful


        ConfigProperty updatedProp1 = mapper.readValue(responseForUpdatedProp1.body().string(), ConfigProperty.class)
        ConfigProperty updatedProp2 = mapper.readValue(responseForUpdatedProp2.body().string(), ConfigProperty.class)

        then:
        updatedProp1.value != "test value"
        updatedValueForProp1 == updatedProp1.value

        updatedProp2.value != "test value2"
        updatedValueForProp2 == updatedProp2.value

        when: "create a new prop"
        def updatedValueForProp3 = "A better value3"
        def updatedBodyForProp3 = [ "key":"test.property3", "value":updatedValueForProp3 ]

        def updated3 = client.doPutWithJsonBody("/project/${projectName}/config/test.property3", updatedBodyForProp3)
        assert updated3.successful

        def responseForUpdatedProp3 = doGet("/project/${projectName}/config/test.property3")
        assert responseForUpdatedProp3.successful

        ConfigProperty updatedProp3 = mapper.readValue(responseForUpdatedProp3.body().string(), ConfigProperty.class)

        then:
        updatedProp3 != null
        updatedValueForProp3 == updatedProp3.value

        when: "All config updated"
        def allConfigResponse = doGet("/project/${projectName}/config")
        assert allConfigResponse.successful
        Map<String, Object> props = mapper.readValue(allConfigResponse.body().string(), HashMap<String, Object>.class)

        then:
        props != null
        props."test.property" == updatedValueForProp1
        props."test.property2" == updatedValueForProp2
        props."test.property3" == updatedValueForProp3

        cleanup:
        deleteProject(projectName)
    }

    def "test-project-config.sh"(){
        given:
        def client = getClient()
        client.apiVersion = 14 // as the original test
        def projectName = "testConfig"
        Object testProperties = [
                "name": projectName,
                "config": [
                        "test.property": "test value",
                        "test.property2": "test value2"
                ]
        ]
        def mapper = new ObjectMapper()

        when:
        def response = client.doPost(
                "/projects",
                testProperties
        )
        assert response.successful
        ProjectCreateResponse parsedResponse = mapper.readValue(
                response.body().string(),
                ProjectCreateResponse.class
        )

        then:
        parsedResponse.name != null
        parsedResponse.name == projectName

        parsedResponse.config."test.property" == "test value"
        parsedResponse.config."test.property2" == "test value2"

        when: "TEST: GET config"
        def responseForProp1 = doGet("/project/${projectName}/config/test.property")
        assert responseForProp1.successful
        ConfigProperty prop1 = mapper.readValue(responseForProp1.body().string(), ConfigProperty.class)

        def responseForProp2 = doGet("/project/${projectName}/config/test.property2")
        assert responseForProp2.successful
        ConfigProperty prop2 = mapper.readValue(responseForProp2.body().string(), ConfigProperty.class)

        then:
        prop1.key == "test.property"
        prop1.value == "test value"

        prop2.key == "test.property2"
        prop2.value == "test value2"

        when: "bulk update"
        def updatedProps = [
                "test.property":"updated value 1",
                "test.property3":"created value 3"
        ]
        def updatedResponse = client.doPutWithJsonBody("/project/${projectName}/config", updatedProps)
        Map<String, Object> parsedUpdatedProps = mapper.readValue(updatedResponse.body().string(), HashMap<String, Object>.class)

        then:
        parsedUpdatedProps."test.property" == "updated value 1"
        parsedUpdatedProps."test.property3" == "created value 3"

        cleanup:
        deleteProject(projectName)
    }

    def "test-project-create-json"(){
        given:
        def client = getClient()
        client.apiVersion = 14 // as the original test
        def projectName = "testProjectCreate"
        def projectDescription = "a description"
        Object testProperties = [
                "name": projectName,
                "description": projectDescription,
                "config": [
                        "test.property": "test value",
                        "test.property2": "test value2"
                ]
        ]
        def mapper = new ObjectMapper()

        when:
        def response = client.doPost(
                "/projects",
                testProperties
        )
        assert response.successful
        ProjectCreateResponse parsedResponse = mapper.readValue(
                response.body().string(),
                ProjectCreateResponse.class
        )

        then:
        parsedResponse.name != null
        parsedResponse.name == projectName

        parsedResponse.config."test.property" == "test value"
        parsedResponse.config."test.property2" == "test value2"

        parsedResponse.description == projectDescription

        cleanup:
        deleteProject(projectName)
    }

    def "test-project-create.sh"(){
        given:
        def client = getClient()
        client.apiVersion = 14 // as the original test
        def projectName = "testProjectCreateConflict"
        def projectDescription = "a description"
        Object testProperties = [
                "name": projectName,
                "description": projectDescription,
                "config": [
                        "test.property": "test value",
                ]
        ]
        def mapper = new ObjectMapper()

        when: "TEST: POST /api/14/projects"
        def response = client.doPost(
                "/projects",
                testProperties
        )
        assert response.successful
        ProjectCreateResponse parsedResponse = mapper.readValue(
                response.body().string(),
                ProjectCreateResponse.class
        )

        then:
        parsedResponse.name != null
        parsedResponse.name == projectName

        parsedResponse.config."test.property" == "test value"

        when: "TEST: POST /api/14/projects (existing project results in conflict)"
        def conflictedResponse = client.doPost(
                "/projects",
                testProperties
        )

        then:
        !conflictedResponse.successful
        conflictedResponse.code() == 409

        cleanup:
        deleteProject(projectName)
    }

    def "test-project-delete-v45.sh"(){
        given:
        def client = getClient()
        client.apiVersion = 45 // as the original test
        def projectName = "testProjectDeleteDeferred"
        def projectDescription = "a description"
        Object testProperties = [
                "name": projectName,
                "description": projectDescription,
                "config": [
                        "test.property": "test value",
                ]
        ]
        def mapper = new ObjectMapper()

        when: "TEST: POST /api/14/projects"
        def response = client.doPost(
                "/projects",
                testProperties
        )
        assert response.successful
        ProjectCreateResponse parsedResponse = mapper.readValue(
                response.body().string(),
                ProjectCreateResponse.class
        )

        then:
        parsedResponse.name != null
        parsedResponse.name == projectName

        when: "#TEST: delete project"
        def deleteResponse = doDelete("/project/${projectName}?deferred=false")
        assert deleteResponse.successful

        then:
        deleteResponse.code() == 204
    }

    def "test-project-delete.sh"(){
        given:
        def client = getClient()
        client.apiVersion = 14 // as the original test
        def projectName = "testProjectDelete"
        def projectDescription = "a description"
        Object testProperties = [
                "name": projectName,
                "description": projectDescription,
                "config": [
                        "test.property": "test value",
                ]
        ]
        def mapper = new ObjectMapper()

        when: "TEST: POST /api/14/projects"
        def response = client.doPost(
                "/projects",
                testProperties
        )
        assert response.successful
        ProjectCreateResponse parsedResponse = mapper.readValue(
                response.body().string(),
                ProjectCreateResponse.class
        )

        then:
        parsedResponse.name != null
        parsedResponse.name == projectName

        when: "#TEST: delete project"
        def deleteResponse = doDelete("/project/${projectName}")
        assert deleteResponse.successful

        then:
        deleteResponse.code() == 204
    }

}
