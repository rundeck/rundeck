package org.rundeck.tests.functional.api.execution

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class ExecutionModeSpec extends BaseContainer{

    def "test_enable_json"() {
        when:
            Map json = post("/system/executions/enable",Map)
        then:
        verifyAll {
            json.executionMode == "active"
        }
    }

    def "test_disable_json"() {
        when:
            Map json = client.post("/system/executions/disable",Map)
        then:
        verifyAll {
            json.executionMode == "passive"
        }
    }

    def "test_all"(){
        when:
            Map json = client.post("/system/executions/" + mode,Map)

        then:
        verifyAll {
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







