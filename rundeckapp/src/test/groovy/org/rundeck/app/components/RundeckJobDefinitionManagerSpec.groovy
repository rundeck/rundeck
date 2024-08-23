package org.rundeck.app.components

import grails.testing.gorm.DataTest
import org.rundeck.app.components.jobs.ImportedJob
import org.rundeck.core.execution.ScriptFileCommand
import rundeck.CommandExec
import rundeck.PluginStep
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Specification

class RundeckJobDefinitionManagerSpec extends Specification implements DataTest   {
    RundeckJobDefinitionManager rundeckJobDefinitionManager = new RundeckJobDefinitionManager()

    void setupSpec() {
        mockDomains Workflow, ScheduledExecution, CommandExec, PluginStep
    }


    def "test decode format file with/without expandTokenInScriptFile field"(){
        when:
        List<ImportedJob<ScheduledExecution>> jobList = rundeckJobDefinitionManager.decodeFormat(format, new ByteArrayInputStream(input.getBytes()))

        then:
        jobList.size() == 1
        ScheduledExecution se = jobList.first().getJob()
        se
        se.workflow.commands.size() == 1
        se.workflow.commands.first() instanceOf PluginStep
        se.workflow.commands.first().type == ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE

        se.workflow.commands.first().configuration == [
            adhocFilepath          : 'path/to/file.sh',
            adhocExecution         : true,
            expandTokenInScriptFile: expandTokenInScriptFile
        ] + (emptyargs ? [argString: ''] : [:])

        where:
        format | input             | expandTokenInScriptFile | emptyargs
        "xml"  | getJobXml(true)   | true                    | true
        "xml"  | getJobXml(false)  | false                   | true
        "xml"  | getJobXml(null)   | false                   | true
        "yaml" | getJobYaml(true)  | true                    | false
        "yaml" | getJobYaml(false) | false                   | false
        "yaml" | getJobYaml(null)  | false                   | false
        "json" | getJobJson(true)  | true                    | false
        "json" | getJobJson(false) | false                   | false
        "json" | getJobJson(null)  | false                   | false
    }

    def "export job to a format with expandTokenInScriptFile"(){
        given:
        def writer = new StringWriter()
        def se = new ScheduledExecution([
                jobName       : 'blue',
                project       : 'AProject',
                groupPath     : 'some/where',
                description   : 'a job',
                argString     : '-a b -c d',
                workflow      : new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec([adhocFilepath: 'path/to/file.sh', expandTokenInScriptFile: true])]
                ),
                serverNodeUUID: null,
                scheduled     : true
        ] ).save()

        when:
        rundeckJobDefinitionManager.exportAs(format,[se], writer)
        writer.flush()

        then:
        jobDefinition.replace("IDSUB",se.id.toString()) == writer.toString()

        where:
        format | jobDefinition
        "xml"  | getJobXml(true)
        "yaml" | getJobYaml(true)
    }

    private static String getJobXml(Boolean expandTokenInScriptFile){
        return """<joblist>
  <job>
    <description>a job</description>
    <executionEnabled>true</executionEnabled>
    <group>some/where</group>
    <id>IDSUB</id>
    <loglevel>WARN</loglevel>
    <name>blue</name>
    <nodeFilterEditable>false</nodeFilterEditable>
    <schedule>
      <month month='*' />
      <time hour='0' minute='0' seconds='0' />
      <weekday day='*' />
      <year year='*' />
    </schedule>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='true' strategy='node-first'>
      <command>
        ${expandTokenInScriptFile!=null ? "<expandTokenInScriptFile>${expandTokenInScriptFile}</expandTokenInScriptFile>" : ""}
        <scriptargs />
        <scriptfile>path/to/file.sh</scriptfile>
      </command>
    </sequence>
  </job>
</joblist>"""
    }

    private static String getJobYaml(Boolean expandTokenInScriptFile){
        return """- description: a job
  executionEnabled: true
  group: some/where
  id: IDSUB
  loglevel: WARN
  name: blue
  nodeFilterEditable: false
  schedule:
    month: '*'
    time:
      hour: '0'
      minute: '0'
      seconds: '0'
    weekday:
      day: '*'
    year: '*'
  scheduleEnabled: true
  sequence:
    commands:
    ${expandTokenInScriptFile!=null ? """- expandTokenInScriptFile: ${expandTokenInScriptFile}
      scriptfile: path/to/file.sh""" : "- scriptfile: path/to/file.sh"}
    keepgoing: true
    strategy: node-first
"""
    }
    private static String getJobJson(Boolean expandTokenInScriptFile){
        return """[
  {
    "description": "a job",
    "executionEnabled": true,
    "group": "some/where",
    "id": "IDSUB",
    "loglevel": "WARN",
    "name": "blue",
    "nodeFilterEditable": false,
    "schedule": {
      "month": "*",
      "time": {
        "hour": "0",
        "minute": "0",
        "seconds": "0"
      },
      "weekday": {
        "day": "*"
      },
      "year": "*"
    },
    "scheduleEnabled": true,
    "sequence": {
      "commands": [{
      """+(expandTokenInScriptFile!=null?"""\"expandTokenInScriptFile\": ${expandTokenInScriptFile},""":"")+"""
        "scriptfile": "path/to/file.sh"
      }],
      "keepgoing": true,
      "strategy": "node-first"
    }
  }
]
"""
    }
}
