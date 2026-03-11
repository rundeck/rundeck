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

    def "run command"() {
        given:
            def execArgs = "echo 'this is a test of /api/run/command'"

        when:
            def parsedResponseBody = post("/project/$PROJECT_NAME/run/command?exec=${execArgs}", null, RunCommand)
            String newExecId = parsedResponseBody.execution.id
            def completedExecution = waitForExecutionFinish(newExecId)

        then:
            noExceptionThrown()
            completedExecution.status != "failed"
    }

    def "run command with node filter"() {
        given:
            def execArgs = "echo 'this is a test of /api/run/command'"

        when:
            def nodeFilter = ".*"
            def parsedResponseBody = post("/project/$PROJECT_NAME/run/command?exec=${execArgs}&filter=${nodeFilter}", null, RunCommand)
            def newExecId = parsedResponseBody.execution.id
            def completedExecution = waitForExecutionFinish(newExecId)

        then:
            noExceptionThrown()
            completedExecution.status != "failed"
    }

    def "run command with not matching filter"(){
        given:
            def execArgs = "echo 'this is a test of /api/run/command'"
        when:
        def nodeFilter = "not-matching-node-filter"
        def parsedResponseBody = post("/project/$PROJECT_NAME/run/command?exec=${execArgs}&filter=${nodeFilter}", null, RunCommand)
        def newExecId = parsedResponseBody.execution.id
        def completedExecution = waitForExecutionFinish(newExecId)

        then:
        noExceptionThrown()
        completedExecution.status == "failed"
    }
}
