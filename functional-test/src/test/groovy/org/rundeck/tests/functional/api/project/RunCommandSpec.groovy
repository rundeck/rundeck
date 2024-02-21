package org.rundeck.tests.functional.api.project

import org.rundeck.tests.functional.api.ResponseModels.Execution
import org.rundeck.tests.functional.api.ResponseModels.RunCommand
import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper

@APITest
class RunCommandSpec extends BaseContainer{

    def setupSpec() {
        startEnvironment()
        setupProject()
    }

    def "test-run-command"(){
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

        then:
        noExceptionThrown()
        newExecId > 0
        mapper.readValue(client.doGetAcceptAll("/execution/$newExecId").body().string(), Execution.class) != null //extra validation to check if exec exists
    }

}
