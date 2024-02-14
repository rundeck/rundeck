package org.rundeck.tests.functional.api.system

import okhttp3.Headers
import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class SysInformationSpec extends BaseContainer{

    def "test_get_listing_with_format"() {

        when:
        def response = client.doGet("/system/info?format=json")

        then:
        verifyAll {
            response.successful
            response.code() == 200
            def json = jsonValue(response.body())
            json.system != null && json.system.size() == 11
            json.system.executions!=null && json.system.executions.size() == 2
            //json.system.extended!=null && json.system.extended.size() == 1
            json.system.healthcheck!=null && json.system.healthcheck.size() == 2
            json.system.jvm!=null && json.system.jvm.size() == 4
            json.system.metrics!=null && json.system.metrics.size() == 2
            json.system.os!=null && json.system.os.size() == 3
            json.system.ping!=null && json.system.ping.size() == 2
            json.system.stats!=null && json.system.stats.size() == 5
            json.system.threadDump!=null && json.system.threadDump.size() == 2
            json.system.timestamp!=null && json.system.timestamp.size() == 3
            json.system.rundeck!=null && json.system.rundeck.size() == 7
            json.system.rundeck.apiversion == client.apiVersion.toString()
        }
    }

    def "test_get_listing_with_header"() {
        when:
        Headers header = new Headers.Builder()
                .add("Accept", "application/json")
                .build()
        def response = client.doGetAddHeaders("/system/info", header)

        then:
        verifyAll {
            response.successful
            response.code() == 200
            def json = jsonValue(response.body())
            json.system != null && json.system.size() == 11
            json.system.executions!=null && json.system.executions.size() == 2
            //json.system.extended!=null && json.system.extended.size() == 1
            json.system.healthcheck!=null && json.system.healthcheck.size() == 2
            json.system.jvm!=null && json.system.jvm.size() == 4
            json.system.metrics!=null && json.system.metrics.size() == 2
            json.system.os!=null && json.system.os.size() == 3
            json.system.ping!=null && json.system.ping.size() == 2
            json.system.stats!=null && json.system.stats.size() == 5
            json.system.threadDump!=null && json.system.threadDump.size() == 2
            json.system.timestamp!=null && json.system.timestamp.size() == 3
            json.system.rundeck!=null && json.system.rundeck.size() == 7
            json.system.rundeck.apiversion == client.apiVersion.toString()
        }
    }



}
