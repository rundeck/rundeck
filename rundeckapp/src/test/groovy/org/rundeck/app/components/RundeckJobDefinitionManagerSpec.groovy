package org.rundeck.app.components

import grails.test.hibernate.HibernateSpec
import org.rundeck.app.components.jobs.ImportedJob
import rundeck.CommandExec
import rundeck.ScheduledExecution
import rundeck.Workflow
import testhelper.RundeckHibernateSpec

class RundeckJobDefinitionManagerSpec extends RundeckHibernateSpec   {
    RundeckJobDefinitionManager rundeckJobDefinitionManager = new RundeckJobDefinitionManager()

    List<Class> getDomainClasses() { [Workflow, ScheduledExecution, CommandExec] }


    def "test decode format file with/without expandTokenInScriptFile field"(){
        when:
        List<ImportedJob<ScheduledExecution>> jobList = rundeckJobDefinitionManager.decodeFormat(format, new ByteArrayInputStream(input.getBytes()))

        then:
        jobList.size() == 1
        ScheduledExecution se = jobList.first().getJob()
        se
        se.workflow.commands.size() == 1
        se.workflow.commands.first().expandTokenInScriptFile == expandTokenInScriptFile

        where:
        format | input             | expandTokenInScriptFile
        "xml"  | getJobXml(true)   | true
        "xml"  | getJobXml(false)  | false
        "yaml" | getJobYaml(true)  | true
        "yaml" | getJobYaml(false) | false
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
        jobDefinition == writer.toString()

        where:
        format | jobDefinition
        "xml"  | getJobXml(true)
        "yaml" | getJobYaml(true)
    }

    private static String getJobXml(boolean expandTokenInScriptFile){
        return """<joblist>
  <job>
    <description>a job</description>
    <executionEnabled>true</executionEnabled>
    <group>some/where</group>
    <id>1</id>
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
        ${expandTokenInScriptFile ? "<expandTokenInScriptFile>true</expandTokenInScriptFile>" : ""}
        <scriptargs />
        <scriptfile>path/to/file.sh</scriptfile>
      </command>
    </sequence>
  </job>
</joblist>"""
    }

    private static String getJobYaml(boolean expandTokenInScriptFile){
        return """- description: a job
  executionEnabled: true
  group: some/where
  id: 2
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
    ${expandTokenInScriptFile ? """- expandTokenInScriptFile: true
      scriptfile: path/to/file.sh""" : "- scriptfile: path/to/file.sh"}
    keepgoing: true
    strategy: node-first
"""
    }
}
