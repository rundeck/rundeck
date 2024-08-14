package org.rundeck.tests.functional.api.project

import org.rundeck.util.api.responses.execution.Execution
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
        def runResponse = client.doPostWithoutBody("/project/$projectName/run/command?exec=${execArgs}")
        def runResponseBody = runResponse.body().string()
        def parsedResponseBody = mapper.readValue(runResponseBody, RunCommand.class)
        def newExecId = parsedResponseBody.execution.id
        waitForExecutionStatus(newExecId, 100)

        then:
        noExceptionThrown()
        newExecId > 0
        mapper.readValue(client.doGetAcceptAll("/execution/$newExecId").body().string(), Execution.class).status != "failed"

        when:
        def nodeFilter = ".*"
        runResponse = client.doPostWithoutBody("/project/$projectName/run/command?exec=${execArgs};filter=${nodeFilter}")
        runResponseBody = runResponse.body().string()
        parsedResponseBody = mapper.readValue(runResponseBody, RunCommand.class)
        newExecId = parsedResponseBody.execution.id
        waitForExecutionStatus(newExecId, 100)

        then:
        noExceptionThrown()
        newExecId > 0
        mapper.readValue(client.doGetAcceptAll("/execution/$newExecId").body().string(), Execution.class).status != "failed"

        when:
        nodeFilter = "not-matching-node-filter"
        runResponse = client.doPostWithoutBody("/project/$projectName/run/command?exec=${execArgs}&filter=${nodeFilter}")
        runResponseBody = runResponse.body().string()
        parsedResponseBody = mapper.readValue(runResponseBody, RunCommand.class)
        newExecId = parsedResponseBody.execution.id
        waitForExecutionStatus(newExecId, 100)

        then:
        noExceptionThrown()
        newExecId > 0
        mapper.readValue(client.doGetAcceptAll("/execution/$newExecId").body().string(), Execution.class).status == "failed"
    }
}
