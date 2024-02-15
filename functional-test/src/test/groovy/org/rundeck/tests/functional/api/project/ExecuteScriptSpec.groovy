package org.rundeck.tests.functional.api.project

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.tests.functional.api.ResponseModels.RunCommand
import org.rundeck.tests.functional.api.ResponseModels.SystemInfo
import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

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
        def projectName = PROJECT_NAME
        def mapper = new ObjectMapper()
        def client = getClient()
        def scriptPath = Paths.get("/home/darwis/Desktop/test/test.sh")
        def scriptRunOutPath = Paths.get("/home/darwis/Desktop/interpreter-test-out.txt")
        def formData = [scriptFile: "$scriptPath" as String] as Map

        when: "We run a script with scriptInterpreter and argsQuoted"
        def args = "scriptInterpreter=bash+-c&argString=%24%7Bnode.name%7D&interpreterArgsQuoted=true"
        def scriptRunResponse = client.doPostWithFormData(
                "/project/$projectName/run/script?$args" as String,
                formData
        )
        assert scriptRunResponse.successful
        RunCommand runScript1 = mapper.readValue(scriptRunResponse.body().string(), RunCommand.class)
        def runScript1Id = runScript1.execution.id
        def outFileContent = readFile(scriptRunOutPath)

        //Extract local nodename
        def systemInfoResponse = doGet("/system/info")
        SystemInfo systemInfo = mapper.readValue(systemInfoResponse.body().string(), SystemInfo.class)
        def localNode = systemInfo.system?.rundeck?.node

        def nodeLine = List.of(localNode)

        then:
        runScript1 != null
        runScript1Id > 0

    }

    def readFile(final Path filePath){
        try{
            return Files.readAllLines(filePath)
        }catch(Exception e){
            e.printStackTrace()
        }
    }

    def assertLinesInsideEntries(List<String> lines, List<String> entries){
        def assertion = true
        lines.each { el -> {
            if( !entries.contains(el) ){
                assertion = false
            }
        }}
        return assertion
    }

}
