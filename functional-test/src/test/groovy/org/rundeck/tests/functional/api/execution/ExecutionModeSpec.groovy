package org.rundeck.tests.functional.api.execution

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class ExecutionModeSpec extends BaseContainer{

    def "test_enable_json"() {
        when:
        def response =client.doPostWithoutBody("/system/executions/enable")
        then:
        response
        verifyAll {
            response.successful
            response.code() == 200
            response.message() == "OK"
            def json = jsonValue(response.body())
            json.executionMode == "active"
        }
    }

    def "test_disable_json"() {
        when:
        def response =client.doPostWithoutBody("/system/executions/disable")
        then:
        response
        verifyAll {
            response.successful
            response.code() == 200
            response.message() == "OK"
            def json = jsonValue(response.body())
            json.executionMode == "passive"
        }
    }

    def "test_all"(){
        when:
        def response =client.doPostWithoutBody("/system/executions/"+mode)

        then:
        verifyAll {
            response.successful
            response.code() == 200
            response.message() == "OK"
            def json = jsonValue(response.body())
            json.executionMode == responseMode
        }
        where:
        mode        | responseMode
        "disable"   | "passive"
        "enable"    | "active"
        "disable"   | "passive"
        "enable"    | "active"

    }

}







