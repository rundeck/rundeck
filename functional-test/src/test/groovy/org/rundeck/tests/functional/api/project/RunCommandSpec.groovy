package org.rundeck.tests.functional.api.project


import org.rundeck.util.api.responses.execution.RunCommand
import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper

@APITest
class RunCommandSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
        setupProject()
    }

    def "test-run-command"() {
        given:
        def client = getClient()
        def mapper = new ObjectMapper()
        def projectName = PROJECT_NAME
        def execArgs = "echo 'this is a test of /api/run/command'"

        when:
        def parsedResponseBody = post("/project/$projectName/run/command?exec=${execArgs}", RunCommand)
        String newExecId = parsedResponseBody.execution.id
        def completedExecution = waitForExecutionFinish(newExecId)

        then:
        noExceptionThrown()
        completedExecution.status != "failed"

        when:
        def nodeFilter = ".*"
        parsedResponseBody = post("/project/$projectName/run/command?exec=${execArgs};filter=${nodeFilter}",RunCommand)
        newExecId = parsedResponseBody.execution.id
        completedExecution = waitForExecutionFinish(newExecId)

        then:
        noExceptionThrown()
        completedExecution.status != "failed"

        when:
        nodeFilter = "not-matching-node-filter"
        parsedResponseBody = post("/project/$projectName/run/command?exec=${execArgs}&filter=${nodeFilter}",RunCommand)
        newExecId = parsedResponseBody.execution.id
        completedExecution = waitForExecutionFinish(newExecId)

        then:
        noExceptionThrown()
        completedExecution.status == "failed"
    }
}
