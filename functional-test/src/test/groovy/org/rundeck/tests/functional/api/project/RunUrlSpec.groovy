package org.rundeck.tests.functional.api.project

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.api.responses.execution.RunCommand
import org.rundeck.util.annotations.APITest
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.container.BaseContainer

@APITest
class RunUrlSpec extends BaseContainer{

    def setupSpec() {
        startEnvironment()
        setupProject()
    }

    def "test-run-url-v14"(){
        given:
        def projectName = PROJECT_NAME
        def client = getClient()
        def mapper = new ObjectMapper()

        when: "should fail with no url param"
        def noParamsResponse = client.doGetAcceptAll("/project/$projectName/run/url")
        def parsedNoParamsResponse = mapper.readValue(noParamsResponse.body().string(), Object.class)

        then: "should fail with no url param"
        noParamsResponse.code() == 400
        parsedNoParamsResponse.errorCode == "api.error.parameter.required"
        parsedNoParamsResponse.error
        parsedNoParamsResponse.message == "parameter \"scriptURL\" is required"

        when:
        def scriptLocation = "/home/rundeck/simpleScript.sh"
        def scriptArg = "file:$scriptLocation"
        def scriptExecArgsResponse = client.doGetAcceptAll("/project/$projectName/run/url?scriptURL=$scriptArg")
        def parsedScriptExecArgs = mapper.readValue(scriptExecArgsResponse.body().string(), RunCommand.class)
        def scriptExecId = parsedScriptExecArgs.execution.id

        then:
        scriptExecArgsResponse.code() == 200
        scriptExecId > 0
        mapper.readValue(client.doGetAcceptAll("/execution/$scriptExecId").body().string(), Execution.class) != null

        when: "We check the execution status"
        def succeededExec = JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                scriptExecId as String,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        )

        then:
        succeededExec.status == ExecutionStatus.SUCCEEDED.state
    }

}
