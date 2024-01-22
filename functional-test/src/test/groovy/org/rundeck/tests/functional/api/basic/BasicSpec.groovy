package org.rundeck.tests.functional.api.basic

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer
import org.rundeck.util.container.RdClient

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

}
