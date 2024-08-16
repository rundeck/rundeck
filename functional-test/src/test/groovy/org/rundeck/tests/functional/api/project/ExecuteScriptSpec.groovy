package org.rundeck.tests.functional.api.project

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.api.responses.execution.ExecutionOutput
import org.rundeck.util.api.responses.execution.RunCommand
import org.rundeck.util.api.responses.system.SystemInfo
import org.rundeck.util.annotations.APITest
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.FileHelpers
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.container.BaseContainer

import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

@APITest
class ExecuteScriptSpec extends BaseContainer{

    def setupSpec(){
        startEnvironment()
        setupProject()
    }

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

        then: "Job succeeds"
        JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                runScript1Id as String,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        ).status == ExecutionStatus.SUCCEEDED.state

        when: "the job succeeds, we read the output of the file with a rundeck job"
        def readJobId = "5e307c0e-9a8a-4a8e-b394-d1ed8b5d15df"
        def readJobXml = "<joblist>\n" +
                "  <job>\n" +
                "    <context>\n" +
                "      <options preserveOrder='true'>\n" +
                "        <option name='filepath' value='/tmp/interpreter-test-out.txt' />\n" +
                "      </options>\n" +
                "    </context>\n" +
                "    <defaultTab>nodes</defaultTab>\n" +
                "    <description></description>\n" +
                "    <executionEnabled>true</executionEnabled>\n" +
                "    <id>5e307c0e-9a8a-4a8e-b394-d1ed8b5d15df</id>\n" +
                "    <loglevel>INFO</loglevel>\n" +
                "    <name>fileReader</name>\n" +
                "    <nodeFilterEditable>false</nodeFilterEditable>\n" +
                "    <plugins />\n" +
                "    <scheduleEnabled>true</scheduleEnabled>\n" +
                "    <schedules />\n" +
                "    <sequence keepgoing='false' strategy='node-first'>\n" +
                "      <command>\n" +
                "        <exec>cat \$RD_OPTION_FILEPATH</exec>\n" +
                "      </command>\n" +
                "    </sequence>\n" +
                "    <uuid>$readJobId</uuid>\n" +
                "  </job>\n" +
                "</joblist>"

        def created = JobUtils.createJob(projectName, readJobXml, client)
        assert created.successful

        // Then run the job that reads the output of request
        def readJobRun = JobUtils.executeJob(readJobId, client)
        assert readJobRun.successful

        Execution readJobRunResponse = mapper.readValue(readJobRun.body().string(), Execution.class)
        def readJobSucceeded = JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                readJobRunResponse.id as String,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        )
        assert readJobSucceeded.status == ExecutionStatus.SUCCEEDED.state
        String execId = readJobRunResponse.id
        def entries = getExecutionOutput(execId)

        //Extract local nodename, this name have to be the only line in output file
        def systemInfoResponse = doGet("/system/info")
        SystemInfo systemInfo = mapper.readValue(systemInfoResponse.body().string(), SystemInfo.class)
        def localNode = systemInfo.system?.rundeck?.node

        def nodeLine = List.of(localNode)

        then: "The output file in: $scriptRunOutPath will have the local nodename in it"
        FileHelpers.assertLinesInsideEntries(nodeLine, entries)

        when: "We have to remove the out file first"
        def execArgs = "rm -rf $scriptRunOutPath"
        def runResponse = client.doPostWithoutBody("/project/$projectName/run/command?exec=${execArgs}")
        def runResponseBody = runResponse.body().string()
        def parsedResponseBody = mapper.readValue(runResponseBody, RunCommand.class)
        def newExecId = parsedResponseBody.execution.id
        def deleteResponse = JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                newExecId as String,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        )

        then: "the job will succeed"
        deleteResponse.status == ExecutionStatus.SUCCEEDED.state

        when: "We do the same request without the args quoted"
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
                WaitingTime.LOW,
                WaitingTime.MODERATE
        )

        // Then run the job that reads the output of request
        def readJobRunEmpty = JobUtils.executeJob(readJobId, client)
        assert readJobRunEmpty.successful

        Execution readJobRunEmptyResponse = mapper.readValue(readJobRunEmpty.body().string(), Execution.class)
        def readJobRunEmptySucceeded = JobUtils.waitForExecutionToBe(
                ExecutionStatus.SUCCEEDED.state,
                readJobRunEmptyResponse.id as String,
                mapper,
                client,
                WaitingTime.LOW,
                WaitingTime.MODERATE
        )
        assert readJobRunEmptySucceeded.status == ExecutionStatus.SUCCEEDED.state
        def execOutputEmptyResponse = client.doGetAcceptAll("/execution/$readJobRunEmptyResponse.id/output")
        def execOutputEmptyString = execOutputEmptyResponse.body().string()
        ExecutionOutput execOutputEmpty = mapper.readValue(execOutputEmptyString, ExecutionOutput.class)
        def emptyEntries = execOutputEmpty.entries.stream().map {it.log}.collect(Collectors.toList())

        then: "The file is empty"
        emptyEntries[0].isEmpty()

    }

    def "test-run-script"(){
        given:
        def projectName = PROJECT_NAME
        def mapper = new ObjectMapper()
        def client = getClient()

        def scriptFilenameEmpty = "/tmp/test-empty.sh"
        def scriptFilename = "/tmp/test-run-script.sh"
        def scriptToFile = new File(scriptFilename)

        def testScript = "#!/bin/bash\n echo sandwich"

        when: "We write the scripts in the filesystem and give permissions"
        FileHelpers.writeFile(testScript, scriptToFile)
        def emptyFile = new File(scriptFilenameEmpty)
        emptyFile.createNewFile()
        scriptToFile.setExecutable(true)

        then: "the script is created"
        Files.exists(Paths.get(scriptFilename))
        emptyFile

        when: "We run a script with scriptInterpreter and argsQuoted"
        def scriptRunResponse = client.doPostWithoutBody("/project/$projectName/run/script")
        Object noParamsResponse = mapper.readValue(scriptRunResponse.body().string(), Object.class)

        then: "Job fails not having params"
        !scriptRunResponse.successful
        noParamsResponse.errorCode == "api.error.parameter.required"
        noParamsResponse.error
        noParamsResponse.message == "parameter \"scriptFile\" is required"

        when: "We run a script without content"
        def emptyScriptRunResponse = client.doPostWithFormData(
                "/project/$projectName/run/script",
                "scriptFile",
                emptyFile
        )
        Object emptyScriptRunErrors = mapper.readValue(emptyScriptRunResponse.body().string(), Object.class)

        then: "Api will fail with error"
        !emptyScriptRunResponse.successful
        emptyScriptRunErrors.errorCode == "api.error.run-script.upload.is-empty"
        emptyScriptRunErrors.error
        emptyScriptRunErrors.message == "Input script file was empty"

        when: "We run a script all ok, execution id returned"
        def validScriptRunResponse = client.doPostWithFormData(
                "/project/$projectName/run/script",
                "scriptFile",
                scriptToFile
        )
        assert validScriptRunResponse.successful
        RunCommand validRunScript = mapper.readValue(validScriptRunResponse.body().string(), RunCommand.class)
        def runScriptId = validRunScript.execution.id

        then:
        runScriptId > 0
    }

}
