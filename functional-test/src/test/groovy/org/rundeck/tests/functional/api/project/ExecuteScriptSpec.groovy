package org.rundeck.tests.functional.api.project

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.tests.functional.api.ResponseModels.ExecutionOutput
import org.rundeck.tests.functional.api.ResponseModels.RunCommand
import org.rundeck.tests.functional.api.ResponseModels.SystemInfo
import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.ExecutionStatus
import org.rundeck.util.api.FileHelpers
import org.rundeck.util.api.JobUtils
import org.rundeck.util.api.WaitingTime
import org.rundeck.util.container.BaseContainer
import org.testcontainers.shaded.org.apache.commons.io.FileUtils

import java.nio.file.Files
import java.nio.file.Path
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
        def scriptRunOutPath = Paths.get("/tmp/interpreter-test-out.txt")
        def scriptFilename = "/tmp/test-interpreter.sh"
        def scriptToFile = new File(scriptFilename)
        def testScript = "#!/bin/bash\n echo \$1 > $scriptRunOutPath"
        def projectName = PROJECT_NAME
        def mapper = new ObjectMapper()
        def client = getClient()

        when: "We write the script in the filesystem and give permissions"
        FileHelpers.writeFile(testScript, scriptToFile)
        scriptToFile.setExecutable(true)

        then: "the script is created"
        Files.exists(Paths.get(scriptFilename))

        when: "We run a script with scriptInterpreter and argsQuoted"
        def args = "scriptInterpreter=bash+-c&argString=%24%7Bnode.name%7D&interpreterArgsQuoted=true"
        def scriptRunResponse = client.doPostWithFormData(
                "/project/$projectName/run/script?$args",
                'scriptFile',
                scriptToFile
        )
        assert scriptRunResponse.successful
        RunCommand runScript1 = mapper.readValue(scriptRunResponse.body().string(), RunCommand.class)
        def runScript1Id = runScript1.execution.id

        JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                runScript1Id as String,
                mapper,
                client,
                WaitingTime.LOW.milliSeconds,
                WaitingTime.MODERATE.milliSeconds / 1000 as int
        )

        def outFileContent = FileHelpers.readFile(scriptRunOutPath)

        //Extract local nodename, this name have to be the only line in output file
        def systemInfoResponse = doGet("/system/info")
        SystemInfo systemInfo = mapper.readValue(systemInfoResponse.body().string(), SystemInfo.class)
        def localNode = systemInfo.system?.rundeck?.node

        def nodeLine = List.of(localNode)

        then: "The output file in: $scriptRunOutPath will have the local nodename in it"
        FileHelpers.assertLinesInsideEntries(nodeLine, outFileContent)

        when: "We do the same request without the args quoted"
        Files.delete(scriptRunOutPath) // Remove the file, start over
        def argsUnquoted = "scriptInterpreter=bash+-c&argString=%24%7Bnode.name%7D&interpreterArgsQuoted=false"
        def unquotedScriptRunResponse = client.doPostWithFormData(
                "/project/$projectName/run/script?$argsUnquoted",
                'scriptFile',
                scriptToFile
        )
        assert unquotedScriptRunResponse.successful
        RunCommand unquotedRunScript1 = mapper.readValue(unquotedScriptRunResponse.body().string(), RunCommand.class)
        def unquotedRunScript1Id = unquotedRunScript1.execution.id

        JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                unquotedRunScript1Id as String,
                mapper,
                client,
                WaitingTime.LOW.milliSeconds,
                WaitingTime.MODERATE.milliSeconds / 1000 as int
        )

        def linesOfEmptyFile = FileHelpers.readFile(scriptRunOutPath)

        then: "The file is empty"
        linesOfEmptyFile.size() > 0 // Just a line in the file
        linesOfEmptyFile.each {
            line -> assert line.isEmpty() // But is empty
        }

        cleanup:
        Files.delete(scriptRunOutPath)
        Files.delete(Paths.get(scriptFilename))

    }

}
