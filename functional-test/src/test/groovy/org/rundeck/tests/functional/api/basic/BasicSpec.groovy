package org.rundeck.tests.functional.api.basic

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class BasicSpec extends BaseContainer {

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

}
