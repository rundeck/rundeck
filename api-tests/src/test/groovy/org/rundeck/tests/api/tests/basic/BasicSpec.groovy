package org.rundeck.tests.api.tests.basic

import org.rundeck.tests.api.util.Base


class BasicSpec extends Base {

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
