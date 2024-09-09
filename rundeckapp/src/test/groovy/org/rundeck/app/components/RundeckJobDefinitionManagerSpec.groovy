package org.rundeck.app.components

import grails.testing.gorm.DataTest
import org.rundeck.app.components.jobs.ImportedJob
import org.rundeck.core.execution.ExecCommand
import org.rundeck.core.execution.ScriptCommand
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


    def "test decode script file step with/without expandTokenInScriptFile field"() {
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
            expandTokenInScriptFile: expandTokenInScriptFile
        ] + (emptyargs ? [argString: ''] : [:])

        where:
        format | input                       | expandTokenInScriptFile | emptyargs
        "xml"  | getJobXmlScriptfile(true)   | true                    | true
        "xml"  | getJobXmlScriptfile(false)  | false                   | true
        "xml"  | getJobXmlScriptfile(null)   | false                   | true
        "yaml" | getJobYamlScriptfile(true)  | true                    | false
        "yaml" | getJobYamlScriptfile(false) | false                   | false
        "yaml" | getJobYamlScriptfile(null)  | false                   | false
        "json" | getJobJsonScriptfile(true)  | true                    | false
        "json" | getJobJsonScriptfile(false) | false                   | false
        "json" | getJobJsonScriptfile(null)  | false                   | false
    }
    def "test decode script URL step with/without expandTokenInScriptFile field"() {
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
            adhocFilepath          : 'http://example.com',
            expandTokenInScriptFile: expandTokenInScriptFile
        ] + (emptyargs ? [argString: ''] : [:])

        where:
        format | input                                            | expandTokenInScriptFile | emptyargs
        "xml"  | getJobXmlScriptURL('http://example.com', true)   | true                    | true
        "xml"  | getJobXmlScriptURL('http://example.com', false)  | false                   | true
        "xml"  | getJobXmlScriptURL('http://example.com', null)   | false                   | true
        "yaml" | getJobYamlScriptURL('http://example.com', true)  | true                    | false
        "yaml" | getJobYamlScriptURL('http://example.com', false) | false                   | false
        "yaml" | getJobYamlScriptURL('http://example.com', null)  | false                   | false
        "json" | getJobJsonScriptURL('http://example.com', true)  | true                    | false
        "json" | getJobJsonScriptURL('http://example.com', false) | false                   | false
        "json" | getJobJsonScriptURL('http://example.com', null)  | false                   | false
    }

    def "test decode command step"() {
        when:
            List<ImportedJob<ScheduledExecution>> jobList = rundeckJobDefinitionManager
                .decodeFormat(format, new ByteArrayInputStream(input.getBytes()))

        then:
            jobList.size() == 1
            ScheduledExecution se = jobList.first().getJob()
            se
            se.workflow.commands.size() == 1
            se.workflow.commands.first() instanceOf PluginStep
            se.workflow.commands.first().type == ExecCommand.EXEC_COMMAND_TYPE

            se.workflow.commands.first().configuration == [
                adhocRemoteString: 'some command',
            ]

        where:
            format | input
            "xml"  | getJobXmlCommand('some command')
            "yaml" | getJobYamlCommand('some command')
            "json" | getJobJsonCommand('some command')
    }

    def "test decode script step"() {
        when:
            List<ImportedJob<ScheduledExecution>> jobList = rundeckJobDefinitionManager
                .decodeFormat(format, new ByteArrayInputStream(input.getBytes()))

        then:
            jobList.size() == 1
            ScheduledExecution se = jobList.first().getJob()
            se
            se.workflow.commands.size() == 1
            se.workflow.commands.first() instanceOf PluginStep
            se.workflow.commands.first().type == ScriptCommand.SCRIPT_COMMAND_TYPE

            se.workflow.commands.first().configuration == [
                adhocLocalString: 'some script',
            ] + (emptyargs ? [argString: ''] : [:])

        where:
            format | input              | emptyargs
            "xml"  | getJobXmlScript()  | true
            "yaml" | getJobYamlScript() | false
            "json" | getJobJsonScript() | false
    }

    def "test decode script step multiline"() {
        when:
            List<ImportedJob<ScheduledExecution>> jobList = rundeckJobDefinitionManager
                .decodeFormat(format, new ByteArrayInputStream(input.getBytes()))

        then:
            jobList.size() == 1
            ScheduledExecution se = jobList.first().getJob()
            se
            se.workflow.commands.size() == 1
            se.workflow.commands.first() instanceOf PluginStep
            se.workflow.commands.first().type == ScriptCommand.SCRIPT_COMMAND_TYPE

            se.workflow.commands.first().configuration == [
                adhocLocalString: script,
            ] + (emptyargs ? [argString: ''] : [:])

        where:
            script = 'some\n\nscript\n'
            format | input | emptyargs
            "xml" | getJobXmlScriptMultiline('some\n\nscript\n') | true
            "yaml" | getJobYamlScriptMulti('some\n          \n          script\n') | false
            "json" | getJobJsonScriptMulti('some\\n\\nscript\\n') | false
    }

    def "Test hasMultiNewlineEnding regex"() {
        expect: "internal method to determine if script content has multiple newlines at the end"
            hasMultiNewlineEnding(script) == expected

        where:
            script                 | expected
            'some\n\nscript'       | false
            'some\n\nscript\n'     | false
            'some\n\nscript\n\n'   | true
            'some\n\nscript\n\n\n' | true
            'some\nscript'         | false
            'some\nscript\n'       | false
            'some\nscript\n\n'     | true
            'some\nscript\n\n\n'   | true
    }

    def "test decode yaml script step multiline preserve newlines"() {
        when:
            List<ImportedJob<ScheduledExecution>> jobList = rundeckJobDefinitionManager
                .decodeFormat(format, new ByteArrayInputStream(input.getBytes()))

        then:

            jobList.size() == 1
            ScheduledExecution se = jobList.first().getJob()
            se
            se.workflow.commands.size() == 1
            se.workflow.commands.first() instanceOf PluginStep
            se.workflow.commands.first().type == ScriptCommand.SCRIPT_COMMAND_TYPE

            se.workflow.commands.first().configuration == [
                adhocLocalString: script,
            ]

        where:
            format = 'yaml'
            script | input
            'some\n\nscript' | getJobYamlScriptMulti('some\n          \n          script')
            'some\n\nscript\n' | getJobYamlScriptMulti('some\n          \n          script\n')
            'some\n\nscript\n\n' | getJobYamlScriptMulti('some\n          \n          script\n\n')
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
            "xml"  | getJobXmlScriptfile(true)
            "yaml" | getJobYamlScriptfile(true)
    }
    static final String XML_PREFIX = '''<joblist>
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
    <sequence keepgoing='true' strategy='node-first'>'''

    static final String XML_SUFFIX = '''
    </sequence>
  </job>
</joblist>'''

    private static String getJobXmlScriptfile(Boolean expandTokenInScriptFile){
        return """${XML_PREFIX}
      <command>
        ${expandTokenInScriptFile!=null ? "<expandTokenInScriptFile>${expandTokenInScriptFile}</expandTokenInScriptFile>" : ""
        }
        <scriptargs />
        <scriptfile>path/to/file.sh</scriptfile>
      </command>${XML_SUFFIX}"""
    }
    private static String getJobXmlScriptURL(String url,Boolean expandTokenInScriptFile){
        return """${XML_PREFIX}
      <command>
        ${expandTokenInScriptFile!=null ? "<expandTokenInScriptFile>${expandTokenInScriptFile}</expandTokenInScriptFile>" : ""
        }
        <scriptargs />
        <scripturl>${url}</scripturl>
      </command>${XML_SUFFIX}"""
    }

    private static String getJobXmlCommand(String command) {
        return """${XML_PREFIX}
      <command>
        <exec>${command}</exec>
      </command>${XML_SUFFIX}"""
    }

    private static String getJobXmlScript() {
        return """${XML_PREFIX}
      <command>
        <scriptargs />
        <script>some script</script>
      </command>${XML_SUFFIX}"""
    }

    private static String getJobXmlScriptMultiline(String data) {
        return """${XML_PREFIX}
      <command>
        <scriptargs />
        <script><![CDATA[${data}]]></script>
      </command>${XML_SUFFIX}"""
    }

    static final String YAML_PREFIX = '''- description: a job
  executionEnabled: true
  group: some/where
  id: IDSUB
  loglevel: WARN
  name: blue
  nodeFilterEditable: false
  schedule:
    month: '*\'
    time:
      hour: '0\'
      minute: '0\'
      seconds: '0\'
    weekday:
      day: '*\'
    year: '*\'
  scheduleEnabled: true
  sequence:
    commands:'''

    static final String YAML_SUFFIX = '''    keepgoing: true
    strategy: node-first
'''

    private static String getJobYamlScriptfile(Boolean expandTokenInScriptFile){
        return """${YAML_PREFIX}
    ${expandTokenInScriptFile!=null ? """- expandTokenInScriptFile: ${expandTokenInScriptFile}
      scriptfile: path/to/file.sh""" : "- scriptfile: path/to/file.sh"}
${YAML_SUFFIX}"""
    }

    private static String getJobYamlScriptURL(String url, Boolean expandTokenInScriptFile){
        return """${YAML_PREFIX}
    ${expandTokenInScriptFile!=null ? """- expandTokenInScriptFile: ${expandTokenInScriptFile}
      scripturl: """+url : "- scripturl: "+url}
${YAML_SUFFIX}"""
    }

    private static String getJobYamlCommand(String command) {
        return """${YAML_PREFIX}
      - exec: ${command}
${YAML_SUFFIX}
"""
    }

    private static String getJobYamlScript() {
        return """${YAML_PREFIX}
      - script: some script
${YAML_SUFFIX}
"""
    }

    private static String getJobYamlScriptMulti(String script) {
        def sigil = ""
        if (hasMultiNewlineEnding(script)) {
            //2+ newlines at the end
            sigil = "+"
        } else if (script.endsWith("\n")) {
            //1 newline at the end
        } else {
            sigil = "-"
            //no newlines at the end, insert one to correctly format
            script = script + "\n"
        }

        return """${YAML_PREFIX}
      - script: |${sigil}
          ${script}${YAML_SUFFIX}
"""
    }

    static boolean hasMultiNewlineEnding(String script) {
        script.matches('(?s)^.*\\n{2,}$')
    }

    static final String JSON_PREFIX = '''[
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
      "commands": '''

    static final String JSON_SUFFIX = ''',
      "keepgoing": true,
      "strategy": "node-first"
    }
  }
]'''

    private static String getJobJsonScriptfile(Boolean expandTokenInScriptFile){
        return """${JSON_PREFIX}[{
      """+(expandTokenInScriptFile!=null?"""\"expandTokenInScriptFile\": ${expandTokenInScriptFile},""":"")+"""
        "scriptfile": "path/to/file.sh"
      }]${JSON_SUFFIX}
"""
    }
    private static String getJobJsonScriptURL(String url, Boolean expandTokenInScriptFile){
        return """${JSON_PREFIX}[{
      """+(expandTokenInScriptFile!=null?"""\"expandTokenInScriptFile\": ${expandTokenInScriptFile},""":"")+"""
        "scripturl": "${url}"
      }]${JSON_SUFFIX}
"""
    }

    private static String getJobJsonCommand(String command) {
        return """${JSON_PREFIX}[{
        "exec": "${command}"
      }]${JSON_SUFFIX}
"""
    }

    private static String getJobJsonScript() {
        return """${JSON_PREFIX}[{
        "script": "some script"
      }]${JSON_SUFFIX}
"""
    }

    private static String getJobJsonScriptMulti(String script) {
        return """${JSON_PREFIX}[{
        "script": "${script}"
      }]${JSON_SUFFIX}
"""
    }
}
