package org.rundeck.tests.functional.api.system

import okhttp3.Headers
import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class SysInformationSpec extends BaseContainer{

    def "test_get_listing"() {
        when:
        Headers header = new Headers.Builder()
                .add("format", "json")
                .build()
        //client.setApiVersion(apiVersion)
        def response = client.doGetAddHeaders("/system/info", header)
        then:
        response
        verifyAll {
            response.successful
            response.code() == 200
            def json = jsonValue(response.body())
            json.system.size() == 11
            json.system.rundeck.size() == 7
            json.system.rundeck.apiversion == client.getApiVersion()
        }
    }

    /*def "test_get_listing2"() {
        when:
        Headers header = new Headers.Builder()
                .add("Accept", "application/json")
                .build()
        def response = client.doGetAddHeaders("/system/info",header)
        then:
        response
        verifyAll {
            response.successful
            response.code() == 200
            response.body().contentType().type() == "application"
            response.body().contentType().subtype() == "json"

        }
    }
    */


}
