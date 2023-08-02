package org.rundeck.tests.api.tests.execution

import org.rundeck.tests.api.util.Base

class ExecutionSpec extends Base {
    def setup(){
        setupProject()
    }
    def "run command, get execution"() {
        when:
            def adhoc = post("/project/${PROJECT_NAME}/run/command?exec=echo+testing+execution+api", Map)
        then:
            adhoc.execution != null
            adhoc.execution.id != null
        when:
            def execid = adhoc.execution.id
            Map exec = get("/execution/${execid}", Map)
        then:
            exec.id == execid
            exec.href != null
            exec.permalink != null
            exec.status != null
            exec.project == PROJECT_NAME
            exec.user == 'admin'
    }

    def "get execution not found"() {
        when:
            def execid = '9999'
            def response = doGet("/execution/${execid}")
        then:
            !response.successful
            response.code() == 404
    }

    def "get execution output not found"() {
        when:
            def execid = '9999'
            def response = doGet("/execution/${execid}/output")
        then:
            !response.successful
            response.code() == 404
    }

    def "get execution output unsupported version"() {
        when:
            def execid = '1'
            def client = clientProvider.client
            client.apiVersion = 5
            def response = client.doGet("/execution/${execid}/output")
        then:
            !response.successful
            response.code() == 400
            jsonValue(response.body()).errorCode == 'api.error.api-version.unsupported'
    }

    def "delete execution not found"() {
        when:
            def execid = '9999'
            def response = doDelete("/execution/${execid}")
        then:
            response.code() == 404
            jsonValue(response.body()).errorCode == 'api.error.item.doesnotexist'
    }
}
