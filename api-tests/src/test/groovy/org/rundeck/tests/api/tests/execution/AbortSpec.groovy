package org.rundeck.tests.api.tests.execution

import org.rundeck.tests.api.util.Base

class AbortSpec extends Base {
    def setup(){
        setupProject()
    }
    def "abort running execution"() {
        def params = "exec=echo+testing+execution+abort+api%3Bsleep+120"

        when: "start a long running command"
            def adhoc = post("/project/${PROJECT_NAME}/run/command?${params}", Map)
        then:
            adhoc.execution.id != null

        when: "get the execution detail"
            def exec = get("/execution/${adhoc.execution.id}", Map)

        then: "it should be running"
            exec.status == 'running'

        when: "abort the execution"
            sleep(4000)
            def result = post("/execution/${adhoc.execution.id}/abort", Map)

        then: "it should be pending"
            result.abort.status == 'pending'
            result.execution.id == adhoc.execution.id.toString()
            result.execution.status == 'running'

        when: "wait for the execution to abort"
            sleep(4000)
            def exec2 = get("/execution/${adhoc.execution.id}", Map)

        then: "it should be aborted"
            exec2.status == 'aborted'
            exec2.abortedby == 'admin'
    }

    def "abort 404"() {
        when: "abort the execution"
            def result = doPost("/execution/999/abort")

        then: "it should be 404"
            result.code() == 404
    }
}
