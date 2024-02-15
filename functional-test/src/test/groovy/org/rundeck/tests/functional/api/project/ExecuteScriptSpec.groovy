package org.rundeck.tests.functional.api.project

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

import java.nio.file.Paths

@APITest
class ExecuteScriptSpec extends BaseContainer{

    def setupSpec(){
        startEnvironment()
        setupProject()
    }

    /**
     * Runs a filesystem based script as an ad-hoc execution
     */
    def "test-run-script-interpreter"(){
        given:
        def projectName = PROJECT_NAME
        def mapper = new ObjectMapper()
        def client = getClient()
        def scriptPath = Paths.get("/home/darwis/Desktop/test/test.sh")
        def formField = "scriptFile"
        def formData = ["$formField" as String: "$scriptPath" as String] as Map

        when: "We run a script with scriptInterpreter and argsQuoted"
        def args = "scriptInterpreter=bash+-c&argString=%24%7Bnode.name%7D&interpreterArgsQuoted=true"
        def scriptRunResponse = client.doPostWithFormData(
                "/project/$projectName/run/script?$args" as String,
                formData
        )
        assert scriptRunResponse.successful

        then:


    }

}
