package org.rundeck.tests.functional.api.project

import org.rundeck.tests.functional.api.ResponseModels.ConfigProperty
import org.rundeck.tests.functional.api.ResponseModels.Execution
import org.rundeck.tests.functional.api.ResponseModels.ProjectCreateResponse
import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml

@APITest
class ConfigSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
    }

    def "test-project-config-key-json"(){
        given:
        def client = getClient()
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

    def "test-project-config"(){
        given:
        def client = getClient()
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

    def "test-project-delete-v45"(){
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

    def "test-project-delete"(){
        given:
        def client = getClient()
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

    def "test-project-invalid"(){
        given:
        def client = getClient()
        def projectName = "RhetoricalMiscalculationElephant"
        def mapper = new ObjectMapper()

        when:
        def response = client.doGet("/project/$projectName")
        def parsedBody = mapper.readValue(response.body().string(), Object.class)

        then:
        response.code() == 404
        parsedBody.errorCode == "api.error.project.missing"
        parsedBody.message == "Project does not exist: $projectName"

    }

    def "test-project-json"(){
        given:
        def client = getClient()
        def projectName = "RhetoricalMiscalculationElephant"
        def mapper = new ObjectMapper()

        when: "We check only the format of the response, regardless of the content"
        def jsonResponseBody = client.doGet("/project/$projectName")
        def responseString = jsonResponseBody.body().string()
        def validJsonParse = mapper.readValue(responseString, Object.class)

        then:
        !isYamlValid(responseString)
        validJsonParse.errorCode == "api.error.project.missing"
        validJsonParse.error
        validJsonParse.message == "Project does not exist: $projectName"
    }

    def "test-project-missing"(){
        given:
        def client = getClient()
        def projectName = "RhetoricalMiscalculationElephant"

        when:
        def jsonResponseBody = client.doGet("/project/$projectName/resources")

        then:
        !jsonResponseBody.successful
        jsonResponseBody.code() == 404

    }

    def "test-project-resources-404"(){
        given:
        def client = getClient()
        def mapper = new ObjectMapper()

        when:
        def jsonResponseBody = client.doGet("/project/someProject/resources")
        def validJsonParse = mapper.readValue(jsonResponseBody.body().string(), Object.class)

        then:
        !jsonResponseBody.successful
        jsonResponseBody.code() == 404
        validJsonParse.errorCode == "api.error.project.missing"
        validJsonParse.error
        validJsonParse.message == "Project does not exist: someProject"
    }

    def "test-project-space-in-name-fails"(){
        given:
        def client = getClient()
        def projectName = "test project"
        Object testProperties = [
                "name": projectName,
                "description":"project name with spaces",
                "config": [
                        "test.property": "test value",
                        "test.property2": "test value2"
                ]
        ]

        when:
        def response = client.doPost(
                "/projects",
                testProperties
        )

        then:
        !response.successful
        response.code() == 400
    }

    def "test-project-resources"(){
        given:
        setupProject()
        def mapper = new ObjectMapper()
        def client = getClient()

        when: "YAML"
        def yamlResponse = client.doGet("/project/$PROJECT_NAME/resources?format=yaml")
        def responseBody = yamlResponse.body().string()
        mapper.readValue(responseBody, Object.class)

        then: "Invalid JSON"
        thrown Exception

        when: "Valid YAML"
        def yaml = new Yaml().load(responseBody)

        then:
        noExceptionThrown()
        yaml != null

        when:
        def jsonResponse = client.doGet("/project/$PROJECT_NAME/resources?format=json")
        def responseJsonBody = jsonResponse.body().string()
        def json = mapper.readValue(responseJsonBody, Object.class)

        then: "JSON is invalid"
        !isYamlValid(responseJsonBody)
        json != null

        when: "A unsupported format"
        def unsupportedResponse = client.doGetAcceptAll("/project/$PROJECT_NAME/resources?format=unsupported")
        def unsupportedParsedResponse = mapper.readValue(unsupportedResponse.body().string(), Object.class)

        then: "unsupported"
        unsupportedParsedResponse.errorCode == "api.error.resource.format.unsupported"
        unsupportedParsedResponse.error
        unsupportedParsedResponse.message == "The format specified is unsupported: unsupported"

        when:
        client.apiVersion = 2 // as the original test states
        def unsupportedApiVersionResponse = client.doGetAcceptAll("/project/$PROJECT_NAME/resources")
        def parsedUnsupportedApiVersionResponse = mapper.readValue(unsupportedApiVersionResponse.body().string(), Object.class)

        then:
        parsedUnsupportedApiVersionResponse.errorCode == "api.error.api-version.unsupported"
        parsedUnsupportedApiVersionResponse.error
        parsedUnsupportedApiVersionResponse.message?.contains("Unsupported API Version \"2\"")
    }

    boolean isYamlValid(String yamlString) {
        def yamlRegex = /^(?:\s*[\w-]+(\s*:\s*(?:(?:\s*".*?"\s*)|(?:\s*'.*?'\s*)|(?:.*?))\s*)?\s*)+$/
        yamlString =~ yamlRegex
    }

}
