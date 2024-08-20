package rundeck

import org.junit.Test
import org.rundeck.core.execution.ExecCommand
import org.rundeck.core.execution.ScriptCommand
import org.rundeck.core.execution.ScriptFileCommand
import rundeck.codecs.JobsXMLCodec
import spock.lang.Specification

import static org.junit.Assert.*

class JobsXMLCodecSpec extends Specification {

     def "testDecodeBasic2"(){
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
        assertNotNull jobs
        assertEquals  '         dig it potato\n' +
                      '\n' +
                      '* list item\n' +
                      '* list item2\n' +
                      '\n' +
                      '<b>inline html</b>' +
                      '\n    ', jobs[0].description
        assertEquals  8, jobs[0].workflow.commands.size()
        assertEquals 'true', jobs[0].workflow.commands[0].configuration.adhocRemoteString
        assertEquals 'false', jobs[0].workflow.commands[1].configuration.adhocRemoteString
        assertEquals '0', jobs[0].workflow.commands[2].configuration.adhocRemoteString
        assertEquals 'true', jobs[0].workflow.commands[3].configuration.adhocLocalString
        assertEquals 'true', jobs[0].workflow.commands[3].configuration.argString
        assertEquals 'false', jobs[0].workflow.commands[4].configuration.adhocLocalString
        assertEquals 'false', jobs[0].workflow.commands[4].configuration.argString
        assertEquals '0', jobs[0].workflow.commands[5].configuration.adhocLocalString
        assertEquals '0', jobs[0].workflow.commands[5].configuration.argString

        assertEquals 'false', jobs[0].workflow.commands[6].configuration.adhocFilepath
        assertEquals 'false', jobs[0].workflow.commands[6].configuration.argString
        assertEquals 'false', jobs[0].workflow.commands[6].errorHandler.configuration.adhocFilepath
        assertEquals '0', jobs[0].workflow.commands[6].errorHandler.configuration.argString

        assertEquals 'false', jobs[0].workflow.commands[7].jobName
        assertEquals 'false', jobs[0].workflow.commands[7].jobGroup
        assertEquals '123', jobs[0].workflow.commands[7].argString
    }

     def "testDecodeBasicScriptInterpreter"(){
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
        assertNotNull jobs
        assertEquals  5, jobs[0].workflow.commands.size()

        assertEquals 'true', jobs[0].workflow.commands[0].configuration.adhocLocalString
        assertEquals 'true', jobs[0].workflow.commands[0].configuration.argString
        assertEquals null, jobs[0].workflow.commands[0].configuration.scriptInterpreter
        assertEquals false, !!jobs[0].workflow.commands[0].configuration.interpreterArgsQuoted

        assertEquals 'true', jobs[0].workflow.commands[1].configuration.adhocLocalString
        assertEquals 'true', jobs[0].workflow.commands[1].configuration.argString
        assertEquals 'bash -c', jobs[0].workflow.commands[1].configuration.scriptInterpreter
        assertEquals false, !!jobs[0].workflow.commands[1].configuration.interpreterArgsQuoted

        assertEquals 'false', jobs[0].workflow.commands[2].configuration.adhocLocalString
        assertEquals 'false', jobs[0].workflow.commands[2].configuration.argString
        assertEquals 'bash -c', jobs[0].workflow.commands[2].configuration.scriptInterpreter
        assertEquals true, !!jobs[0].workflow.commands[2].configuration.interpreterArgsQuoted

        assertEquals '0', jobs[0].workflow.commands[3].configuration.adhocLocalString
        assertEquals '0', jobs[0].workflow.commands[3].configuration.argString
        assertEquals 'bash -c', jobs[0].workflow.commands[3].configuration.scriptInterpreter
        assertEquals false, !!jobs[0].workflow.commands[3].configuration.interpreterArgsQuoted

        assertEquals 'false', jobs[0].workflow.commands[4].configuration.adhocFilepath
        assertEquals 'false', jobs[0].workflow.commands[4].configuration.argString
        assertEquals 'false', jobs[0].workflow.commands[4].errorHandler.configuration.adhocFilepath
        assertEquals 'bash -c', jobs[0].workflow.commands[4].configuration.scriptInterpreter
        assertEquals false, !!jobs[0].workflow.commands[4].configuration.interpreterArgsQuoted
        assertEquals '0', jobs[0].workflow.commands[4].errorHandler.configuration.argString

    }

    def "testDecodeErrorhandler"(){
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
        assertNotNull jobs
        assertEquals 1, jobs.size()
        ScheduledExecution se=jobs[0]
        assertEquals(5,jobs[0].workflow.commands.size())
        jobs[0].workflow.commands.each{
            assertNotNull(it.errorHandler)
        }
        jobs[0].workflow.commands[0].errorHandler.type == ExecCommand.EXEC_COMMAND_TYPE
        jobs[0].workflow.commands[0].errorHandler.configuration == [adhocExecution:true, adhocRemoteString: 'testerr']
        assertFalse(jobs[0].workflow.commands[0].errorHandler.keepgoingOnSuccess)

        jobs[0].workflow.commands[1].errorHandler.type == ScriptCommand.SCRIPT_COMMAND_TYPE
        jobs[0].workflow.commands[1].errorHandler.configuration == [adhocExecution:true, adhocLocalString: 'test2err', argString: 'blah blah err']
        assertFalse(jobs[0].workflow.commands[1].errorHandler.keepgoingOnSuccess)

        jobs[0].workflow.commands[2].errorHandler.type == ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE
        jobs[0].workflow.commands[2].errorHandler.configuration == [adhocExecution:true, adhocFilepath: 'test3err', expandTokenInScriptFile:false, argString: 'blah3 blah3 err']
        assertFalse(jobs[0].workflow.commands[2].errorHandler.keepgoingOnSuccess)

        assertEquals('testerr', jobs[0].workflow.commands[3].errorHandler.jobName)
        assertEquals('grouperr', jobs[0].workflow.commands[3].errorHandler.jobGroup)
        assertEquals('line err',jobs[0].workflow.commands[3].errorHandler.argString)
        assertNotNull(jobs[0].workflow.commands[3].errorHandler.keepgoingOnSuccess)
        assertTrue(jobs[0].workflow.commands[3].errorHandler.keepgoingOnSuccess)

        assertTrue( jobs[0].workflow.commands[4].errorHandler.nodeStep)
        assertEquals('blah2', jobs[0].workflow.commands[4].errorHandler.type)
        assertEquals([rice:'pilaf'],jobs[0].workflow.commands[4].errorHandler.configuration)
        assertNotNull(jobs[0].workflow.commands[4].errorHandler.keepgoingOnSuccess)
        assertTrue(jobs[0].workflow.commands[4].errorHandler.keepgoingOnSuccess)
    }


    def "testDecodeStringsShouldNotBeBoolean"(){
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
        assertNotNull jobs
        assertEquals "false",jobs[0].jobName
        assertEquals  "false",jobs[0].groupPath
        assertEquals  "false",jobs[0].description
        assertEquals  false, jobs[0].nodeExcludePrecedence
        assertEquals  false, jobs[0].nodeKeepgoing
        assertEquals  null, jobs[0].nodeInclude
        assertEquals  "hostname: false", jobs[0].filter
        assertEquals  'proj1',jobs[0].project
        assertEquals  1, jobs[0].workflow.commands.size()
        assertEquals  "false", jobs[0].workflow.commands[0].configuration.adhocRemoteString
        assertEquals  "false", jobs[0].workflow.commands[0].errorHandler.configuration.adhocLocalString
        assertEquals  "false", jobs[0].workflow.commands[0].errorHandler.configuration.argString
        assertEquals  false, jobs[0].workflow.commands[0].errorHandler.keepgoingOnSuccess
        assertEquals  2, jobs[0].nodeThreadcount
        assertEquals  false, jobs[0].workflow.keepgoing
        assertEquals 2, jobs[0].options.size()
        def opts=new ArrayList(jobs[0].options)
        assertEquals 'false', opts[0].name
        assertEquals false, opts[0].enforced
        assertEquals 'x', opts[1].name
        assertEquals false, opts[1].required
        assertEquals '9000636026', opts[1].defaultValue
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
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "incorrect workflow", jobs[0].workflow
            assertEquals "incorrect workflow strategy", "node-first", jobs[0].workflow.strategy
            assertNotNull "incorrect workflow strategy", jobs[0].workflow.commands
            assertEquals "incorrect workflow strategy", 1, jobs[0].workflow.commands.size()
            def cmd1 = jobs[0].workflow.commands[0]
            assertNotNull "incorrect workflow", cmd1
            cmd1.configuration == [adhocExecution:true, adhocRemoteString: 'a script']
    }
    def "decode simple workflow with script content"(){

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
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertNotNull "incorrect workflow", jobs[0].workflow
        assertEquals "incorrect workflow strategy", "node-first", jobs[0].workflow.strategy
        assertNotNull "incorrect workflow strategy", jobs[0].workflow.commands
        assertEquals "incorrect workflow strategy", 1, jobs[0].workflow.commands.size()
        def cmd1 = jobs[0].workflow.commands[0]
        assertNotNull "incorrect workflow", cmd1
        cmd1.configuration == [adhocExecution:true, adhocLocalString: 'a script 2']
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
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "incorrect workflow", jobs[0].workflow
            assertEquals "incorrect workflow strategy", "node-first", jobs[0].workflow.strategy
            assertNotNull "incorrect workflow strategy", jobs[0].workflow.commands
            assertEquals "incorrect workflow strategy", 1, jobs[0].workflow.commands.size()
            def cmd1 = jobs[0].workflow.commands[0]
            assertNotNull "incorrect workflow", cmd1
            cmd1.configuration == [adhocExecution:true, adhocFilepath: '/a/path/to/a/script',expandTokenInScriptFile:false, argString: '-some args -to the -script']
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
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "incorrect workflow", jobs[0].workflow
            assertEquals "incorrect workflow strategy", "node-first", jobs[0].workflow.strategy
            assertNotNull "incorrect workflow strategy", jobs[0].workflow.commands
            assertEquals "incorrect workflow strategy", 1, jobs[0].workflow.commands.size()
            def cmd1 = jobs[0].workflow.commands[0]
            assertNotNull "incorrect workflow", cmd1
            assertTrue "incorrect type: ${cmd1}", (cmd1 instanceof JobExec)
            assertNull "incorrect argString", cmd1.argString
            assertEquals "incorrect jobName", 'bob', cmd1.jobName
            assertNull "incorrect jobGroup", cmd1.jobGroup
            assertEquals "incorrect nodeStep", false, !!cmd1.nodeStep
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
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "incorrect workflow", jobs[0].workflow
            assertEquals "incorrect workflow strategy", "node-first", jobs[0].workflow.strategy
            assertNotNull "incorrect workflow strategy", jobs[0].workflow.commands
            assertEquals "incorrect workflow strategy", 1, jobs[0].workflow.commands.size()
            def cmd1 = jobs[0].workflow.commands[0]
            assertNotNull "incorrect workflow", cmd1
            assertTrue "incorrect type: ${cmd1}", (cmd1 instanceof JobExec)
            assertNull "incorrect adhocRemoteString", cmd1.argString
            assertEquals "incorrect jobName", 'bob', cmd1.jobName
            assertEquals "incorrect jobGroup", '/some/path', cmd1.jobGroup
            assertEquals "incorrect nodeStep", false, !!cmd1.nodeStep
    }

    def "decode simple workflow with step-first strategy"(){
        when:
        //
       def jobs = JobsXMLCodec.decode("""<joblist>
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
""")
        then:
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertNotNull "incorrect workflow", jobs[0].workflow
        assertEquals "incorrect workflow strategy", "step-first", jobs[0].workflow.strategy
        assertNotNull "incorrect workflow strategy", jobs[0].workflow.commands
        assertEquals "incorrect workflow strategy", 1, jobs[0].workflow.commands.size()
       def cmd1 = jobs[0].workflow.commands[0]
        assertNotNull "incorrect workflow", cmd1
        assertTrue "incorrect type: ${cmd1}", (cmd1 instanceof JobExec)
        assertNull "incorrect adhocRemoteString", cmd1.argString
        assertEquals "incorrect jobName", 'bob', cmd1.jobName
        assertEquals "incorrect jobGroup", '/some/path', cmd1.jobGroup
        assertEquals "incorrect nodeStep", false, !!cmd1.nodeStep
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
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "incorrect workflow", jobs[0].workflow
            assertEquals "incorrect workflow strategy", "node-first", jobs[0].workflow.strategy
            assertNotNull "incorrect workflow strategy", jobs[0].workflow.commands
            assertEquals "incorrect workflow strategy", 1, jobs[0].workflow.commands.size()
            def cmd1 = jobs[0].workflow.commands[0]
            assertNotNull "incorrect workflow", cmd1
            assertTrue "incorrect type: ${cmd1}", (cmd1 instanceof JobExec)
            assertNotNull "incorrect adhocRemoteString", cmd1.argString
            assertEquals "incorrect adhocRemoteString", "-test1 1 -test2 2", cmd1.argString
            assertEquals "incorrect jobName", 'bob', cmd1.jobName
            assertEquals "incorrect jobGroup", '/some/path', cmd1.jobGroup
            assertEquals "incorrect nodeStep", false, !!cmd1.nodeStep
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
            assertNotNull jobs
            assertEquals "incorrect size", 1, jobs.size()
            assertNotNull "incorrect workflow", jobs[0].workflow
            assertEquals "incorrect workflow strategy", "node-first", jobs[0].workflow.strategy
            assertNotNull "incorrect workflow strategy", jobs[0].workflow.commands
            assertEquals "incorrect workflow strategy", 1, jobs[0].workflow.commands.size()
            def cmd1 = jobs[0].workflow.commands[0]
            assertNotNull "incorrect workflow", cmd1
            assertTrue "incorrect type: ${cmd1}", (cmd1 instanceof JobExec)
            assertNotNull "incorrect adhocRemoteString", cmd1.argString
            assertEquals "incorrect adhocRemoteString", "-test1 1 -test2 2", cmd1.argString
            assertEquals "incorrect jobName", 'bob', cmd1.jobName
            assertEquals "incorrect jobGroup", '/some/path', cmd1.jobGroup
            assertEquals "incorrect nodeStep", true, !!cmd1.nodeStep
    }
    def "decode scripturl step with script args"(){
        when:
        //
       def jobs = JobsXMLCodec.decode("""<joblist>
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
""")
        then:
        assertNotNull jobs
        assertEquals "incorrect size", 1, jobs.size()
        assertNotNull "incorrect workflow", jobs[0].workflow
        assertEquals "incorrect workflow strategy", "node-first", jobs[0].workflow.strategy
        assertNotNull "incorrect workflow strategy", jobs[0].workflow.commands
        assertEquals "incorrect workflow strategy", 1, jobs[0].workflow.commands.size()
       def cmd1 = jobs[0].workflow.commands[0]
        assertNotNull "incorrect workflow", cmd1
        cmd1.configuration == [adhocExecution:true, adhocFilepath: 'http://example.com/a/path/to/a/script', expandTokenInScriptFile:false, argString: '-some args -to the -script']

    }

    def "testEncodeDecode"(){
given:
        def XmlParser parser = new XmlParser()

        //test multiline script content
        def jobs3 = [
            new ScheduledExecution(
                jobName:'test job 1',
                description:'test descrip',
                loglevel: 'INFO',
                project:'test1',
                //type
                //name
                //command

                workflow: new Workflow(keepgoing: true, commands: [new CommandExec(
                    argString:'elf biscuits',
                    adhocExecution:true,
                    adhocLocalString:'#!/bin/bash\n\necho what is this monkey < test.out\n\necho this is a test\n\nexit 0',

                    )]),
                nodeThreadcount:1,
                nodeKeepgoing:true,
                )
        ]

        when:
        def xmlstr = JobsXMLCodec.encode(jobs3)
        then:
        assertNotNull xmlstr
        assertTrue xmlstr instanceof String

        when:
        def jobs=JobsXMLCodec.decode(xmlstr)
        then:
        assertNotNull jobs
        assertEquals "incorrect size",1,jobs.size()
        def job1=jobs[0]
        assertNotNull(job1.workflow)
        assertNotNull(job1.workflow.commands)
        assertEquals(1,job1.workflow.commands.size())
        def wfi=job1.workflow.commands[0]
        wfi.configuration==[adhocExecution:true, adhocLocalString: '#!/bin/bash\n\necho what is this monkey < test.out\n\necho this is a test\n\nexit 0', argString: 'elf biscuits']
        wfi.type==ScriptCommand.SCRIPT_COMMAND_TYPE

    }


}