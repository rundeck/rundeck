package org.rundeck.tests.functional.api.basic

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer
import org.rundeck.util.container.RdClient
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper

@APITest
class BasicSpec extends BaseContainer {

    def setupSpec() {
        setupProject()
    }

    def testInvalidToken() {
        given:
        def client = clientWithToken("invalidtoken")
        when:
        def response = client.doGet("/system/info")
        then:
        !response.successful
        response.code() == 403
        cleanup:
        response.close()
    }

    def testSystemInfo() {
        when:
        def data = get("/system/info", Map)
        then:
        !data.error
        data.system.rundeck.apiversion.toInteger() >= 14
    }

    def invalidUrl() {
        when:
            def data = doGet("/dnexist?project=test")
        then:
            data.code() == 404
            def json = getClient().jsonValue(data.body(), Map)
            json.error
            json.message == "Invalid API Request: /api/45/dnexist"
    }

    def "test-require-bad-version"(){
        given:
        def client = getClient()
        def mapper = new ObjectMapper()

        when: "Any string for api version"
        def response = client.doGetCustomApiVersion("/projects", "sandwich")
        def parsedResponse = mapper.readValue(response.body().string(), Object.class)

        then:
        response.code() == 400
        parsedResponse.message?.contains("Unsupported API Version")
        parsedResponse.errorCode == "api.error.api-version.unsupported"

        when: "invalid api version number"
        client.apiVersion = client.apiVersion + 1000
        def invalidVersionResponse = client.doGetAcceptAll("/projects")
        def parsedInvalidVersionResponse = mapper.readValue(invalidVersionResponse.body().string(), Object.class)

        then:
        response.code() == 400
        parsedInvalidVersionResponse.message?.contains("Unsupported API Version")
        parsedInvalidVersionResponse.errorCode == "api.error.api-version.unsupported"

        when: "api version 0"
        client.apiVersion = 0
        def zeroVersionResponse = client.doGetAcceptAll("/projects")
        def zeroVersionResponseResponse = mapper.readValue(zeroVersionResponse.body().string(), Object.class)

        then:
        response.code() == 400
        zeroVersionResponseResponse.message?.contains("Unsupported API Version")
        zeroVersionResponseResponse.errorCode == "api.error.api-version.unsupported"

        when: "api version 00000001"
        client.apiVersion = 00000001
        def multipleZeroesVersionResponse = client.doGetAcceptAll("/projects")
        def multipleZeroesResponseResponse = mapper.readValue(multipleZeroesVersionResponse.body().string(), Object.class)

        then:
        response.code() == 400
        multipleZeroesResponseResponse.message?.contains("Unsupported API Version")
        multipleZeroesResponseResponse.errorCode == "api.error.api-version.unsupported"

    }
}
