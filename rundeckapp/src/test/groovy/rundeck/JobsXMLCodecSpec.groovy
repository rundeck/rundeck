package rundeck


import org.rundeck.core.execution.ExecCommand
import org.rundeck.core.execution.ScriptCommand
import org.rundeck.core.execution.ScriptFileCommand
import rundeck.codecs.JobsXMLCodec
import spock.lang.Specification


class JobsXMLCodecSpec extends Specification {

    def "testDecodeBasic2"() {
        given:

            def xml = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>         dig it potato

* list item
* list item2

&lt;b&gt;inline html&lt;/b&gt;
    </description>
    <loglevel>WARN</loglevel>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence>
        <command>
        <exec>true</exec>
        </command>
        <command>
        <exec>false</exec>
        </command>
        <command>
        <exec>0</exec>
        </command>
        <command>
        <script>true</script>
            <scriptargs>true</scriptargs>
        </command>
        <command>
        <script>false</script>
            <scriptargs>false</scriptargs>
        </command>
        <command>
        <script>0</script>
            <scriptargs>0</scriptargs>
        </command>
        <command>
            <scriptfile>false</scriptfile>
            <scriptargs>false</scriptargs>
            <errorhandler  keepgoingOnSuccess='false'>
                <scriptfile>false</scriptfile>
                <scriptargs>0</scriptargs>
            </errorhandler>
        </command>
        <command>
            <jobref>
            <name>false</name>
            <group>false</group>
            <arg line="123"/>
            </jobref>
        </command>
    </sequence>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
        when:
            def jobs = JobsXMLCodec.decode(xml)
        then:
            jobs != null
            jobs[0].description == '''         dig it potato

* list item
* list item2

<b>inline html</b>
    '''

            def cmds = jobs[0].workflow.commands
            cmds.size() == 8
            cmds[0].configuration.adhocRemoteString == 'true'
            cmds[1].configuration.adhocRemoteString == 'false'
            cmds[2].configuration.adhocRemoteString == '0'
            cmds[3].configuration.adhocLocalString == 'true'
            cmds[3].configuration.argString == 'true'
            cmds[4].configuration.adhocLocalString == 'false'
            cmds[4].configuration.argString == 'false'
            cmds[5].configuration.adhocLocalString == '0'
            cmds[5].configuration.argString == '0'

            cmds[6].configuration.adhocFilepath == 'false'
            cmds[6].configuration.argString == 'false'
            cmds[6].errorHandler.configuration.adhocFilepath == 'false'
            cmds[6].errorHandler.configuration.argString == '0'

            cmds[7].jobName == 'false'
            cmds[7].jobGroup == 'false'
            cmds[7].argString == '123'
    }

    def "testDecodeBasicScriptInterpreter"() {
        given:

            def xml = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence>
        <command>
            <script>true</script>
            <scriptargs>true</scriptargs>
        </command>
        <command>
            <script>true</script>
            <scriptinterpreter>bash -c</scriptinterpreter>
            <scriptargs>true</scriptargs>
        </command>
        <command>
            <script>false</script>
            <scriptinterpreter argsquoted="true">bash -c</scriptinterpreter>
            <scriptargs>false</scriptargs>
        </command>
        <command>
            <script>0</script>
            <scriptinterpreter argsquoted="false">bash -c</scriptinterpreter>
            <scriptargs>0</scriptargs>
        </command>
        <command>
            <scriptfile>false</scriptfile>
            <scriptargs>false</scriptargs>
            <scriptinterpreter argsquoted="false">bash -c</scriptinterpreter>
            <errorhandler  keepgoingOnSuccess='false'>
                <scriptfile>false</scriptfile>
                <scriptargs>0</scriptargs>
            </errorhandler>
        </command>
    </sequence>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
        when:
            def jobs = JobsXMLCodec.decode(xml)
        then:
            jobs != null
            jobs[0].workflow.commands.size() == 5

            jobs[0].workflow.commands[0].configuration==[adhocLocalString: 'true', argString: 'true']
            jobs[0].workflow.commands[1].configuration==[adhocLocalString: 'true', argString: 'true',scriptInterpreter: 'bash -c']
            jobs[0].workflow.commands[2].configuration==[adhocLocalString: 'false', argString: 'false',scriptInterpreter: 'bash -c',interpreterArgsQuoted: 'true']
            jobs[0].workflow.commands[3].configuration==[adhocLocalString: '0', argString: '0',scriptInterpreter: 'bash -c',interpreterArgsQuoted: 'false']
            jobs[0].workflow.commands[4].configuration==[adhocFilepath: 'false', argString: 'false',scriptInterpreter: 'bash -c',interpreterArgsQuoted: 'false']
            jobs[0].workflow.commands[4].errorHandler.configuration==[adhocFilepath: 'false', argString: '0']

    }

    def "testDecodeErrorhandler"() {
        given:
            def basic7 = """<joblist>
  <job>
    <id>8</id>
    <name>punch2</name>
    <description>dig it potato</description>
    <loglevel>WARN</loglevel>
    <context>
      <project>zig</project>
      <options>
        <option name='clip' value='true' />
      </options>
    </context>
    <sequence>
        <command>
            <exec>test</exec>
            <errorhandler>
                <exec>testerr</exec>
            </errorhandler>
        </command>
        <command>
            <script>test2</script>
            <scriptargs>blah blah</scriptargs>
            <errorhandler>
                <script>test2err</script>
                <scriptargs>blah blah err</scriptargs>
            </errorhandler>
        </command>
        <command>
            <scriptfile>test3</scriptfile>
            <scriptargs>blah3 blah3</scriptargs>
            <errorhandler  keepgoingOnSuccess='false'>
                <scriptfile>test3err</scriptfile>
                <scriptargs>blah3 blah3 err</scriptargs>
            </errorhandler>
        </command>
        <command>
            <jobref name="test" group="group"/>
            <errorhandler keepgoingOnSuccess='true'>
                <jobref name="testerr" group="grouperr">
                    <arg line="line err"/>
                </jobref>
            </errorhandler>
        </command>
        <command>
            <step-plugin type="blah">
                <configuration>
                    <entry key="elf" value="cheese"/>
                </configuration>
            </step-plugin>
            <errorhandler keepgoingOnSuccess='true'>
                <node-step-plugin type="blah2">
                    <configuration>
                        <entry key="rice" value="pilaf"/>
                    </configuration>
                </node-step-plugin>
            </errorhandler>
        </command>
    </sequence>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>true</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
        when:
            def jobs = JobsXMLCodec.decode(basic7)
        then:
            jobs != null
            jobs.size() == 1
            ScheduledExecution se = jobs[0]

            def cmds = jobs[0].workflow.commands
            cmds.size() == 5
            cmds.every {
                it.errorHandler != null
            }
            cmds[0].errorHandler.type == ExecCommand.EXEC_COMMAND_TYPE
            cmds[0].errorHandler.configuration == [adhocRemoteString: 'testerr']
            !cmds[0].errorHandler.keepgoingOnSuccess

            cmds[1].errorHandler.type == ScriptCommand.SCRIPT_COMMAND_TYPE
            cmds[1].errorHandler.configuration == [
                adhocLocalString: 'test2err',
                argString       : 'blah blah err'
            ]
            !cmds[1].errorHandler.keepgoingOnSuccess

            cmds[2].errorHandler.type == ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE
            cmds[2].errorHandler.configuration == [
                adhocFilepath          : 'test3err',
                argString              : 'blah3 blah3 err'
            ]
            !cmds[2].errorHandler.keepgoingOnSuccess

            cmds[3].errorHandler.jobName == 'testerr'
            cmds[3].errorHandler.jobGroup == 'grouperr'
            cmds[3].errorHandler.argString == 'line err'
            cmds[3].errorHandler.keepgoingOnSuccess != null
            cmds[3].errorHandler.keepgoingOnSuccess

            cmds[4].errorHandler.nodeStep
            cmds[4].errorHandler.type == 'blah2'
            cmds[4].errorHandler.configuration == [rice: 'pilaf']
            cmds[4].errorHandler.keepgoingOnSuccess != null
            cmds[4].errorHandler.keepgoingOnSuccess
    }


    def "testDecodeStringsShouldNotBeBoolean"() {
        given:
            def example1 = """<joblist>
  <job>
    <id>1</id>
    <name>false</name>
    <description >false</description>
    <loglevel>VERBOSE</loglevel>
    <group>false</group>
    <context>
      <project>proj1</project>
      <options>
        <option name="false" enforcedvalues="false"/>
        <option required="false" name="x" value="9000636026"/>
      </options>
    </context>
    <sequence keepgoing="false">
        <command>
            <exec>false</exec>
            <errorhandler keepgoingOnSuccess="false">
                <script>false</script>
                <scriptargs>false</scriptargs>
            </errorhandler>
        </command>
    </sequence>
    <nodefilters excludeprecedence="false">
      <include>
        <hostname>false</hostname>
        <tags />
        <os-name />
        <os-family />
        <os-arch />
        <os-version />
        <name />
      </include>
    </nodefilters>
    <dispatch>
      <threadcount>2</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
  </job>
</joblist>
"""
        when:
            def jobs = JobsXMLCodec.decode(example1)
        then:
            jobs != null
            jobs[0].jobName == "false"
            jobs[0].groupPath == "false"
            jobs[0].description == "false"
            jobs[0].nodeExcludePrecedence == false
            jobs[0].nodeKeepgoing == false
            jobs[0].nodeInclude == null
            jobs[0].filter == "hostname: false"
            jobs[0].project == 'proj1'
            jobs[0].workflow.commands.size() == 1
            jobs[0].workflow.commands[0].configuration.adhocRemoteString == "false"
            jobs[0].workflow.commands[0].errorHandler.configuration.adhocLocalString == "false"
            jobs[0].workflow.commands[0].errorHandler.configuration.argString == "false"
            jobs[0].workflow.commands[0].errorHandler.keepgoingOnSuccess == false
            jobs[0].nodeThreadcount == 2
            jobs[0].workflow.keepgoing == false
            jobs[0].options.size() == 2
            def opts = new ArrayList(jobs[0].options)
            opts[0].name == 'false'
            opts[0].enforced == false
            opts[1].name == 'x'
            opts[1].required == false
            opts[1].defaultValue == '9000636026'
    }

    def "testDecodeWorkflow"() {
        given:
            //simple workflow with script command
            def xml6 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence>
        <command>
            <exec>a script</exec>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""

        when:
            def jobs = JobsXMLCodec.decode(xml6)
        then:
            jobs != null
            jobs.size() == 1
            jobs[0].workflow != null
            jobs[0].workflow.strategy == "node-first"
            jobs[0].workflow.commands != null
            jobs[0].workflow.commands.size() == 1
            def cmd1 = jobs[0].workflow.commands[0]
            cmd1 != null
            cmd1.configuration == [adhocRemoteString: 'a script']
    }

    def "decode simple workflow with script content"() {

        //
        def xml7 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence>
        <command>
            <script>a script 2</script>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""

        when:
            def jobs = JobsXMLCodec.decode(xml7)
        then:
            jobs != null
            jobs.size() == 1
            jobs[0].workflow != null
            jobs[0].workflow.strategy == "node-first"
            jobs[0].workflow.commands != null
            jobs[0].workflow.commands.size() == 1
            def cmd1 = jobs[0].workflow.commands[0]
            cmd1 != null
            cmd1.configuration == [adhocLocalString: 'a script 2']
    }

    def "decode  script with args"() {
        //simple workflow with script content
        def xml8 = """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence>
        <command>
            <scriptfile>/a/path/to/a/script</scriptfile>
            <scriptargs>-some args -to the -script</scriptargs>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""

        when:
            def jobs = JobsXMLCodec.decode(xml8)
        then:
            jobs != null
            jobs.size() == 1
            jobs[0].workflow != null
            jobs[0].workflow.strategy == "node-first"
            jobs[0].workflow.commands != null
            jobs[0].workflow.commands.size() == 1
            def cmd1 = jobs[0].workflow.commands[0]
            cmd1 != null
            cmd1.configuration == [
                adhocFilepath          : '/a/path/to/a/script',
                argString              : '-some args -to the -script'
            ]
            cmd1.type == ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE
            //
    }

    def "decode simple workflow with jobref without jobGroup"() {
        when:
            def jobs = JobsXMLCodec.decode(
                """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence>
        <command>
            <jobref name="bob" />
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""
            )
        then:
            jobs != null
            jobs.size() == 1
            jobs[0].workflow != null
            jobs[0].workflow.strategy == "node-first"
            jobs[0].workflow.commands != null
            jobs[0].workflow.commands.size() == 1
            def cmd1 = jobs[0].workflow.commands[0]
            cmd1 != null
            (cmd1 instanceof JobExec)
            cmd1.argString == null
            cmd1.jobName == 'bob'
            cmd1.jobGroup == null
            !!cmd1.nodeStep == false
    }

    def "decode simple workflow with jobref"() {
        when:
            //
            def jobs = JobsXMLCodec.decode(
                """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence>
        <command>
            <jobref name="bob" group="/some/path"/>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""
            )
        then:
            jobs != null
            jobs.size() == 1
            jobs[0].workflow != null
            jobs[0].workflow.strategy == "node-first"
            jobs[0].workflow.commands != null
            jobs[0].workflow.commands.size() == 1
            def cmd1 = jobs[0].workflow.commands[0]
            cmd1 != null
            (cmd1 instanceof JobExec)
            cmd1.argString == null
            cmd1.jobName == 'bob'
            cmd1.jobGroup == '/some/path'
            !!cmd1.nodeStep == false
    }

    def "decode simple workflow with step-first strategy"() {
        when:
            //
            def jobs = JobsXMLCodec.decode(
                """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence strategy="step-first">
        <command>
            <jobref name="bob" group="/some/path"/>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""
            )
        then:
            jobs != null
            jobs.size() == 1
            jobs[0].workflow != null
            jobs[0].workflow.strategy == "step-first"
            jobs[0].workflow.commands != null
            jobs[0].workflow.commands.size() == 1
            def cmd1 = jobs[0].workflow.commands[0]
            cmd1 != null
            (cmd1 instanceof JobExec)
            cmd1.argString == null
            cmd1.jobName == 'bob'
            cmd1.jobGroup == '/some/path'
            !!cmd1.nodeStep == false
    }

    def "decode jobref item with args"() {
        when:
            //
            def jobs = JobsXMLCodec.decode(
                """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence>
        <command>
            <jobref name="bob" group="/some/path">
                <arg line="-test1 1 -test2 2"/>
            </jobref>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""
            )
        then:
            jobs != null
            jobs.size() == 1
            jobs[0].workflow != null
            jobs[0].workflow.strategy == "node-first"
            jobs[0].workflow.commands != null
            jobs[0].workflow.commands.size() == 1
            def cmd1 = jobs[0].workflow.commands[0]
            cmd1 != null
            (cmd1 instanceof JobExec)
            cmd1.argString != null
            cmd1.argString == "-test1 1 -test2 2"
            cmd1.jobName == 'bob'
            cmd1.jobGroup == '/some/path'
            !!cmd1.nodeStep == false
    }

    def "decode jobref item nodeStep=true"() {
        when:
            //
            def jobs = JobsXMLCodec.decode(
                """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence>
        <command>
            <jobref name="bob" group="/some/path" nodeStep="true">
                <arg line="-test1 1 -test2 2"/>
            </jobref>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""
            )
        then:
            jobs != null
            jobs.size() == 1
            jobs[0].workflow != null
            jobs[0].workflow.strategy == "node-first"
            jobs[0].workflow.commands != null
            jobs[0].workflow.commands.size() == 1
            def cmd1 = jobs[0].workflow.commands[0]
            cmd1 != null
            (cmd1 instanceof JobExec)
            cmd1.argString != null
            cmd1.argString == "-test1 1 -test2 2"
            cmd1.jobName == 'bob'
            cmd1.jobGroup == '/some/path'
            !!cmd1.nodeStep
    }

    def "decode scripturl step with script args"() {
        when:
            //
            def jobs = JobsXMLCodec.decode(
                """<joblist>
  <job>
    <id>5</id>
    <name>wait1</name>
    <description></description>
    <loglevel>INFO</loglevel>
    <context>
        <project>test1</project>
    </context>
    <sequence>
        <command>
            <scripturl>http://example.com/a/path/to/a/script</scripturl>
            <scriptargs>-some args -to the -script</scriptargs>
        </command>
    </sequence>
    <dispatch>
      <threadcount>1</threadcount>
      <keepgoing>false</keepgoing>
    </dispatch>
    <schedule>
      <time hour='11' minute='21' />
      <weekday day='*' />
      <month month='*' />
    </schedule>
  </job>
</joblist>
"""
            )
        then:
            jobs != null
            jobs.size() == 1
            jobs[0].workflow != null
            jobs[0].workflow.strategy == "node-first"
            jobs[0].workflow.commands != null
            jobs[0].workflow.commands.size() == 1
            def cmd1 = jobs[0].workflow.commands[0]
            cmd1 != null
            cmd1.configuration == [
                adhocFilepath          : 'http://example.com/a/path/to/a/script',
                argString              : '-some args -to the -script'
            ]

    }

    def "test encode decode multiline script content"() {
        given:
            def jobs3 = [
                new ScheduledExecution(
                    jobName: 'test job 1',
                    description: 'test descrip',
                    loglevel: 'INFO',
                    project: 'test1',

                    workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                            new CommandExec(
                                argString: 'elf biscuits',
                                adhocLocalString: '''#!/bin/bash

echo what is this monkey < test.out

echo this is a test

exit 0''',
                                )
                        ]
                    ),
                    nodeThreadcount: 1,
                    nodeKeepgoing: true,
                    )
            ]

        when:
            def xmlstr = JobsXMLCodec.encode(jobs3)
            assert xmlstr != null
            assert xmlstr instanceof String
            def jobs = JobsXMLCodec.decode(xmlstr)
        then:
            jobs != null
            jobs.size() == 1
            def job1 = jobs[0]
            job1.workflow != null
            job1.workflow.commands != null
            job1.workflow.commands.size() == 1
            def wfi = job1.workflow.commands[0]
            wfi.configuration == [
                adhocLocalString: '''#!/bin/bash

echo what is this monkey < test.out

echo this is a test

exit 0''', argString: 'elf biscuits'
            ]
            wfi.type == ScriptCommand.SCRIPT_COMMAND_TYPE

    }
}