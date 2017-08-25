package rundeck.codecs

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import rundeck.CommandExec
import rundeck.JobExec
import rundeck.Notification
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 5/17/16.
 */
@TestMixin(GrailsUnitTestMixin)
class JobsXMLCodecSpec extends Specification {
    @Unroll
    def "encode notification plugins are sorted"() {
        given:
        def XmlSlurper parser = new XmlSlurper()
        def jobs1 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [new CommandExec(
                                        [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese ' +
                                                '-particle']
                                )],
                                ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,
                        notifications: [
                                new Notification(
                                        eventTrigger: 'onsuccess',
                                        type: order[0],
                                        configuration: [c: 'c', z: 'z', a: 'a']
                                ),
                                new Notification(
                                        eventTrigger: 'onsuccess',
                                        type: order[1],
                                        configuration: [a: 'a', z: 'z', c: 'c']
                                ),
                                new Notification(
                                        eventTrigger: 'onsuccess',
                                        type: order[2],
                                        configuration: [z: 'z', a: 'a', c: 'c']
                                ),
                        ]
                )
        ]
        when:
        def xmlstr = JobsXMLCodec.encode(jobs1)

        then:
        null != xmlstr
        xmlstr instanceof String

        def doc = parser.parse(new StringReader(xmlstr))
        doc.name() == 'joblist'
        doc.job.size() == 1
        doc.job[0].name[0].text() == 'test job 1'
        doc.job[0].notification[0].onsuccess.size() == 1
        doc.job[0].notification[0].onsuccess[0].plugin.size() == 3
        def expectorder = ['aplugin', 'bplugin', 'zplugin']
        def conforder = ['a', 'c', 'z']
        doc.job[0].notification[0].onsuccess[0].plugin.collect { it.'@type' } == expectorder
        (0..2).each { i ->
            doc.job[0].notification[0].onsuccess[0].plugin[i].'@type' == expectorder[i]
            doc.job[0].notification[0].onsuccess[0].plugin[i].configuration[0].entry.collect { it.'@key' } == conforder
        }

        where:
        order                             | _
        ['aplugin', 'bplugin', 'zplugin'] | _
        ['aplugin', 'zplugin', 'bplugin'] | _
        ['zplugin', 'aplugin', 'bplugin'] | _
    }

    @Unroll
    def "encode email config is sorted"() {
        given:
        def XmlSlurper parser = new XmlSlurper()
        def jobs1 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [new CommandExec(
                                        [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese ' +
                                                '-particle']
                                )],
                                ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,
                        notifications: [
                                new Notification(
                                        eventTrigger: 'onsuccess',
                                        type: 'email',
                                        configuration: config
                                ),
                        ]
                )
        ]
        when:
        def xmlstr = JobsXMLCodec.encode(jobs1)

        then:
        null != xmlstr
        xmlstr instanceof String
        xmlstr.contains('''<email a='a' c='c' z='z' />''')

        where:
        config                   | _
        [c: 'c', z: 'z', a: 'a'] | _
        [z: 'z', a: 'a', c: 'c'] | _
        [a: 'a', z: 'z', c: 'c'] | _
    }

    @Unroll
    def "encode job ref intersection without filter"() {
        given:
        def XmlSlurper parser = new XmlSlurper()
        def jobs1 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [
                                        new CommandExec(
                                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey ' +
                                                        'cheese ' +
                                                        '-particle']
                                        ),
                                        new JobExec(
                                                jobName: 'ajob',
                                                jobGroup: 'a/group',
                                                nodeIntersect: nodeIntersect
                                        )],
                                ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,
                        filter: 'some nodes',
                        )
        ]
        when:
        def xmlstr = JobsXMLCodec.encode(jobs1)

        then:
        null != xmlstr
        xmlstr instanceof String
        def doc = parser.parse(new StringReader(xmlstr))
        doc.job[0].sequence[0].command[1].jobref[0].dispatch[0].nodeIntersect.text() == nodeIntersect.toString()

        where:
        nodeIntersect | _
        true          | _
        false         | _
    }

    @Unroll
    def "encode job ref with project"() {
        given:
        def XmlSlurper parser = new XmlSlurper()
        def jobs1 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [
                                        new JobExec(
                                                jobName: 'ajob',
                                                jobGroup: 'a/group',
                                                jobProject: 'projectB'
                                        )],
                                ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,
                        filter: 'some nodes',
                        )
        ]
        when:
        def xmlstr = JobsXMLCodec.encode(jobs1)

        then:
        null != xmlstr
        xmlstr instanceof String
        def doc = parser.parse(new StringReader(xmlstr))
        doc.job[0].sequence[0].command[0].jobref[0]."@project".text() == 'projectB'

        where:
        nodeIntersect | _
        true          | _
        false         | _
    }

    def "decode job ref intersection and filter"() {
        given:
        def xml = """<joblist>
  <job>
    <description>ddddd</description>
    <executionEnabled>true</executionEnabled>
    <loglevel>INFO</loglevel>
    <name>test job 1</name>
    <sequence keepgoing='false' strategy='teststrateg'>
      <command>
        <jobref name="ajob" group="some/group">
            <dispatch>
                <nodeIntersect>$input</nodeIntersect>
            </dispatch>
            $filterXml
        </jobref>
      </command>
    </sequence>

  </job>
</joblist>
""".toString()
        when:
        def result = JobsXMLCodec.decode(xml)

        then:
        result.size() == 1
        result[0].workflow.commands[0] != null
        result[0].workflow.commands[0] instanceof JobExec
        result[0].workflow.commands[0].jobName == 'ajob'
        result[0].workflow.commands[0].jobGroup == 'some/group'
        result[0].workflow.commands[0].nodeIntersect == input
        result[0].workflow.commands[0].nodeFilter == filter

        where:
        input | filterXml                                             | filter
        true  | ''                                                    | null
        true  | '<nodefilters/>'                                      | null
        true  | '<nodefilters/><nodefilters/>'                        | null
        true  | '<nodefilters></nodefilters>'                         | null
        true  | '<nodefilters>spurious</nodefilters>'                 | null
        true  | '<nodefilters><filter>afilter</filter></nodefilters>' | 'afilter'
        false | ''                                                    | null
        false | '<nodefilters/>'                                      | null
        false | '<nodefilters/><nodefilters/>'                        | null
        false | '<nodefilters></nodefilters>'                         | null
        false | '<nodefilters>spurious</nodefilters>'                 | null
        false | '<nodefilters><filter>afilter</filter></nodefilters>' | 'afilter'

    }

    @Unroll
    def "decode job ref with project"() {
        given:
        def xml = """<joblist>
  <job>
    <description>ddddd</description>
    <executionEnabled>true</executionEnabled>
    <loglevel>INFO</loglevel>
    <name>test job 1</name>
    <sequence keepgoing='false' strategy='teststrateg'>
      <command>
        <jobref name="ajob" group="some/group" $attr>
            $xmltext2
        </jobref>
        $xmltext
      </command>
    </sequence>

  </job>
</joblist>
""".toString()
        when:
        def result = JobsXMLCodec.decode(xml)

        then:
        result.size() == 1
        result[0].workflow.commands[0] != null
        result[0].workflow.commands[0] instanceof JobExec
        result[0].workflow.commands[0].jobName == 'ajob'
        result[0].workflow.commands[0].jobGroup == 'some/group'
        result[0].workflow.commands[0].jobProject == 'projectB'


        where:
        attr                 | xmltext                       | xmltext2
        'project="projectB"' | ''                            | ''
        ''                   | '<project>projectB</project>' | ''
        ''                   | ''                            | '<project>projectB</project>'
    }
    def "encode workflow strategy plugin"() {
        given:
        def XmlSlurper parser = new XmlSlurper()
        def jobs1 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [new CommandExec(
                                        [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                                )],
                                strategy: 'test',
                                pluginConfigMap: [WorkflowStrategy: ['test': [aproperty: 'b value']]]
                        ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true
                )
        ]
        when:
        def xmlstr = JobsXMLCodec.encode(jobs1)

        then:
        null != xmlstr
        xmlstr instanceof String

        def doc = parser.parse(new StringReader(xmlstr))
        doc.name() == 'joblist'
        doc.job.size()==1
        doc.job[0].name[0].text()=='test job 1'
        doc.job[0].sequence[0].pluginConfig.size()==1
        doc.job[0].sequence[0].pluginConfig[0].WorkflowStrategy.size()==1
        doc.job[0].sequence[0].pluginConfig[0].WorkflowStrategy[0].test.size()==1
        doc.job[0].sequence[0].pluginConfig[0].WorkflowStrategy[0].test[0].aproperty.size()==1
        doc.job[0].sequence[0].pluginConfig[0].WorkflowStrategy[0].test[0].aproperty.text()=='b value'
    }

    def "encode workflow strategy plugin empty config removed"() {
        given:
        def XmlSlurper parser = new XmlSlurper()
        def jobs1 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [new CommandExec(
                                        [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese ' +
                                                '-particle']
                                )],
                                strategy: 'test',
                                pluginConfigMap: [WorkflowStrategy: ['test': [:]]]
                        ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true
                )
        ]
        when:
        def xmlstr = JobsXMLCodec.encode(jobs1)

        then:
        null != xmlstr
        xmlstr instanceof String

        def doc = parser.parse(new StringReader(xmlstr))
        doc.name() == 'joblist'
        doc.job.size() == 1
        doc.job[0].name[0].text() == 'test job 1'
        doc.job[0].sequence[0].pluginConfig.size() == 0
    }

    def "encode workflow strategy plugin empty config removed other config remains"() {
        given:
        def XmlSlurper parser = new XmlSlurper()
        def jobs1 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [new CommandExec(
                                        [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese ' +
                                                '-particle']
                                )],
                                strategy: 'test',
                                pluginConfigMap: [WorkflowStrategy: ['test': [:]], OtherData: [a: 'b']]
                        ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true
                )
        ]
        when:
        def xmlstr = JobsXMLCodec.encode(jobs1)

        then:
        null != xmlstr
        xmlstr instanceof String

        def doc = parser.parse(new StringReader(xmlstr))
        doc.name() == 'joblist'
        doc.job.size() == 1
        doc.job[0].name[0].text() == 'test job 1'
        doc.job[0].sequence[0].pluginConfig.size()==1
        doc.job[0].sequence[0].pluginConfig[0].OtherData.size()==1
        doc.job[0].sequence[0].pluginConfig[0].OtherData[0].a.size()==1
        doc.job[0].sequence[0].pluginConfig[0].OtherData[0].a.text()=='b'
    }
    def "decode workflow strategy plugin"(){
        given:
        def xml = '''<joblist>
  <job>
    <description>ddddd</description>
    <executionEnabled>true</executionEnabled>

    <loglevel>INFO</loglevel>
    <name>test job 1</name>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='teststrateg'>
      <command>
        <exec>echo hi</exec>
      </command>
      <pluginConfig>
        <WorkflowStrategy>
          <teststrateg>
            <aproperty><![CDATA[multiline
data
inside]]></aproperty>
          </teststrateg>
        </WorkflowStrategy>
      </pluginConfig>
    </sequence>

  </job>
</joblist>'''

        when:
        def result = JobsXMLCodec.decode(xml)

        then:
        result.size()==1
        result[0].jobName=='test job 1'
        result[0].workflow.strategy=='teststrateg'
        result[0].workflow.pluginConfigMap == [WorkflowStrategy: [teststrateg: [aproperty: 'multiline\ndata\ninside']]]

    }

    def "encode step log filter plugin config single"() {
        given:
        def XmlSlurper parser = new XmlSlurper()
        def jobs1 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [new CommandExec(
                                        [adhocRemoteString               : 'test buddy', argString: '-delay 12 ' +
                                                '-monkey cheese ' +
                                                '-particle', pluginConfig:
                                                 [LogFilter: [
                                                         [type  : 'mask-passwords',
                                                          config: [
                                                                  color      : 'red',
                                                                  replacement: '[SECURE]'
                                                          ]]
                                                 ]]]
                                )],
                                strategy: 'test',

                                ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true
                )
        ]
        when:
        def xmlstr = JobsXMLCodec.encode(jobs1)

        then:
        null != xmlstr
        xmlstr instanceof String
        def doc = parser.parse(new StringReader(xmlstr))
        doc.name() == 'joblist'
        doc.job.size() == 1
        doc.job[0].name[0].text() == 'test job 1'
        doc.job[0].sequence[0].command.size() == 1
        doc.job[0].sequence[0].command[0].plugins.size() == 1
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter.size() == 1
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[0].'@type'.text() == 'mask-passwords'
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[0].config.size() == 1
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[0].config[0].color.size() == 1
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[0].config[0].color.text() == 'red'
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[0].config[0].replacement.size() == 1
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[0].config[0].replacement.text() == '[SECURE]'
    }

    def "encode step log filter plugin config single empty config"() {
        given:
        def XmlSlurper parser = new XmlSlurper()
        def jobs1 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [new CommandExec(
                                        [adhocRemoteString               : 'test buddy', argString: '-delay 12 ' +
                                                '-monkey cheese ' +
                                                '-particle', pluginConfig:
                                                 [LogFilter: [
                                                         [type  : 'mask-passwords',
                                                          config: [:]]
                                                 ]]]
                                )],
                                strategy: 'test',

                                ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true
                )
        ]
        when:
        def xmlstr = JobsXMLCodec.encode(jobs1)

        then:
        null != xmlstr
        xmlstr instanceof String
        def doc = parser.parse(new StringReader(xmlstr))
        doc.name() == 'joblist'
        doc.job.size() == 1
        doc.job[0].name[0].text() == 'test job 1'
        doc.job[0].sequence[0].command.size() == 1
        doc.job[0].sequence[0].command[0].plugins.size() == 1
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter.size() == 1
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[0].'@type'.text() == 'mask-passwords'
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[0].config.size() == 0

    }

    def "encode step log filter plugin config multi"() {
        given:
        def XmlSlurper parser = new XmlSlurper()
        def jobs1 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [new CommandExec(
                                        [adhocRemoteString               : 'test buddy', argString: '-delay 12 ' +
                                                '-monkey cheese ' +
                                                '-particle', pluginConfig:
                                                 [LogFilter: [
                                                         [type  : 'mask-passwords',
                                                          config: [
                                                                  color      : 'red',
                                                                  replacement: '[SECURE]'
                                                          ]],
                                                         [type  : 'key-value-data',
                                                          config: [
                                                                  regex    : 'something',
                                                                  debugOnly: 'true'
                                                          ]]
                                                 ]]]
                                )],
                                strategy: 'test',

                                ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true
                )
        ]
        when:
        def xmlstr = JobsXMLCodec.encode(jobs1)

        then:
        null != xmlstr
        xmlstr instanceof String
        def doc = parser.parse(new StringReader(xmlstr))
        doc.name() == 'joblist'
        doc.job.size() == 1
        doc.job[0].name[0].text() == 'test job 1'
        doc.job[0].sequence[0].command.size() == 1
        doc.job[0].sequence[0].command[0].plugins.size() == 1
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter.size() == 2
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[0].'@type'.text() == 'mask-passwords'
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[0].config.size() == 1
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[0].config[0].color.size() == 1
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[0].config[0].color.text() == 'red'
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[0].config[0].replacement.size() == 1
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[0].config[0].replacement.text() == '[SECURE]'
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[1].'@type'.text() == 'key-value-data'
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[1].config.size() == 1
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[1].config[0].regex.size() == 1
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[1].config[0].regex.text() == 'something'
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[1].config[0].debugOnly.size() == 1
        doc.job[0].sequence[0].command[0].plugins[0].LogFilter[1].config[0].debugOnly.text() == 'true'
    }


    def "decode step log filter plugin config single entry"() {
        given:
        def xml = """<joblist>
  <job>
    <description>ddddd</description>
    <executionEnabled>true</executionEnabled>

    <loglevel>INFO</loglevel>
    <name>test job 1</name>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='teststrateg'>
      <command>
        <exec>echo hi</exec>
        <plugins>
          <LogFilter type='mask-passwords'>
            $configxml
          </LogFilter>
        </plugins>
      </command>
      
    </sequence>

  </job>
</joblist>
""".toString()
        when:
        def result = JobsXMLCodec.decode(xml)

        then:
        result.size() == 1
        result[0].jobName == 'test job 1'
        result[0].workflow.commands[0].pluginConfig == [LogFilter: [expected]]
        result[0].workflow.commands[0].getPluginConfigForType('LogFilter') == [expected]
        result[0].workflow.commands[0].getPluginConfigListForType('LogFilter') == [expected]

        where:
        configxml                     | expected
        '<config>\n' +
                '            <color>red</color>\n' +
                '            <replacement>[SECURE]</replacement>\n' +
                '          </config>' | [type: 'mask-passwords', config: [color: 'red', replacement: '[SECURE]']]
        '<config></config>'           | [type: 'mask-passwords']
        ''                            | [type: 'mask-passwords']
    }

    def "decode step empty plugins config"() {
        given:
        def xml = """<joblist>
  <job>
    <description>ddddd</description>
    <executionEnabled>true</executionEnabled>

    <loglevel>INFO</loglevel>
    <name>test job 1</name>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='teststrateg'>
      <command>
        <exec>echo hi</exec>
        <plugins>
        </plugins>
      </command>
      
    </sequence>

  </job>
</joblist>
""".toString()
        when:
        def result = JobsXMLCodec.decode(xml)

        then:
        result.size() == 1
        result[0].jobName == 'test job 1'
        result[0].workflow.commands[0].pluginConfig == null
        result[0].workflow.commands[0].getPluginConfigForType('LogFilter') == null
        result[0].workflow.commands[0].getPluginConfigListForType('LogFilter') == null

    }


    def "decode step log filter plugin config multi entry"() {
        given:
        def xml = """<joblist>
  <job>
    <description>ddddd</description>
    <executionEnabled>true</executionEnabled>

    <loglevel>INFO</loglevel>
    <name>test job 1</name>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='teststrateg'>
      <command>
        <exec>echo hi</exec>
        <plugins>
          <LogFilter type='mask-passwords'>
            $configxml
          </LogFilter>
          <LogFilter type='key-value-data'>
              <config>
                <debugOnly>true</debugOnly>
                <regex>something</regex>
              </config>
            </LogFilter>
        </plugins>
      </command>
      
    </sequence>

  </job>
</joblist>
""".toString()
        when:
        def result = JobsXMLCodec.decode(xml)

        then:
        result.size() == 1
        result[0].jobName == 'test job 1'
        result[0].workflow.commands[0].pluginConfig ==
                [LogFilter: [expected,
                             [type  : 'key-value-data',
                              config: [regex: 'something', debugOnly: 'true']]
                ]]
        result[0].workflow.commands[0].getPluginConfigForType('LogFilter') ==
                [expected,
                 [type  : 'key-value-data',
                  config: [regex: 'something', debugOnly: 'true']]
                ]
        result[0].workflow.commands[0].getPluginConfigListForType('LogFilter') ==
                [expected,
                 [type  : 'key-value-data',
                  config: [regex: 'something', debugOnly: 'true']]
                ]
        where:
        configxml                     | expected
        '<config>\n' +
                '            <color>red</color>\n' +
                '            <replacement>[SECURE]</replacement>\n' +
                '          </config>' | [type: 'mask-passwords', config: [color: 'red', replacement: '[SECURE]']]
        '<config></config>'           | [type: 'mask-passwords']
        ''                            | [type: 'mask-passwords']

    }

    def "encode workflow global log filter plugin config single"() {
        given:
        def XmlSlurper parser = new XmlSlurper()
        def jobs1 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [new CommandExec(
                                        [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese ' +
                                                '-particle']
                                )],
                                strategy: 'test',
                                pluginConfigMap:
                                        [LogFilter: [
                                                [type  : 'mask-passwords',
                                                 config: [
                                                         color      : 'red',
                                                         replacement: '[SECURE]'
                                                 ]]
                                        ]]
                        ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true
                )
        ]
        when:
        def xmlstr = JobsXMLCodec.encode(jobs1)

        then:
        null != xmlstr
        xmlstr instanceof String

        def doc = parser.parse(new StringReader(xmlstr))
        doc.name() == 'joblist'
        doc.job.size() == 1
        doc.job[0].name[0].text() == 'test job 1'
        doc.job[0].sequence[0].pluginConfig.size() == 1
        doc.job[0].sequence[0].pluginConfig[0].LogFilter.size() == 1
        doc.job[0].sequence[0].pluginConfig[0].LogFilter[0].'@type'.text() == 'mask-passwords'
        doc.job[0].sequence[0].pluginConfig[0].LogFilter[0].config.size() == 1
        doc.job[0].sequence[0].pluginConfig[0].LogFilter[0].config[0].color.size() == 1
        doc.job[0].sequence[0].pluginConfig[0].LogFilter[0].config[0].color.text() == 'red'
        doc.job[0].sequence[0].pluginConfig[0].LogFilter[0].config[0].replacement.size() == 1
        doc.job[0].sequence[0].pluginConfig[0].LogFilter[0].config[0].replacement.text() == '[SECURE]'
    }

    def "encode workflow global log filter plugin config multi"() {
        given:
        def XmlSlurper parser = new XmlSlurper()
        def jobs1 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [new CommandExec(
                                        [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese ' +
                                                '-particle']
                                )],
                                strategy: 'test',
                                pluginConfigMap:
                                        [LogFilter: [
                                                [type  : 'mask-passwords',
                                                 config: [
                                                         color      : 'red',
                                                         replacement: '[SECURE]'
                                                 ]],
                                                [type  : 'key-value-data',
                                                 config: [
                                                         regex    : 'something',
                                                         debugOnly: 'true'
                                                 ]]
                                        ]]
                        ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true
                )
        ]
        when:
        def xmlstr = JobsXMLCodec.encode(jobs1)

        then:
        null != xmlstr
        xmlstr instanceof String
        def doc = parser.parse(new StringReader(xmlstr))
        doc.name() == 'joblist'
        doc.job.size() == 1
        doc.job[0].name[0].text() == 'test job 1'
        doc.job[0].sequence[0].pluginConfig.size() == 1
        doc.job[0].sequence[0].pluginConfig[0].LogFilter.size() == 2
//        doc.job[0].sequence[0].pluginConfig[0].LogFilter[0].'@type'.size() == 1
        doc.job[0].sequence[0].pluginConfig[0].LogFilter[0].'@type'.text() == 'mask-passwords'
        doc.job[0].sequence[0].pluginConfig[0].LogFilter[0].config.size() == 1
        doc.job[0].sequence[0].pluginConfig[0].LogFilter[0].config[0].color.size() == 1
        doc.job[0].sequence[0].pluginConfig[0].LogFilter[0].config[0].color.text() == 'red'
        doc.job[0].sequence[0].pluginConfig[0].LogFilter[0].config[0].replacement.size() == 1
        doc.job[0].sequence[0].pluginConfig[0].LogFilter[0].config[0].replacement.text() == '[SECURE]'
//        doc.job[0].sequence[0].pluginConfig[0].LogFilter[1].type.size() == 1
        doc.job[0].sequence[0].pluginConfig[0].LogFilter[1].'@type'.text() == 'key-value-data'
        doc.job[0].sequence[0].pluginConfig[0].LogFilter[1].config.size() == 1
        doc.job[0].sequence[0].pluginConfig[0].LogFilter[1].config[0].regex.size() == 1
        doc.job[0].sequence[0].pluginConfig[0].LogFilter[1].config[0].regex.text() == 'something'
        doc.job[0].sequence[0].pluginConfig[0].LogFilter[1].config[0].debugOnly.size() == 1
        doc.job[0].sequence[0].pluginConfig[0].LogFilter[1].config[0].debugOnly.text() == 'true'
    }

    def "decode workflow global log filter plugin config single entry"() {
        given:
        def xml = """<joblist>
  <job>
    <description>ddddd</description>
    <executionEnabled>true</executionEnabled>

    <loglevel>INFO</loglevel>
    <name>test job 1</name>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='teststrateg'>
      <command>
        <exec>echo hi</exec>
      </command>
      <pluginConfig>
        <LogFilter type='mask-passwords'>
          $configxml
        </LogFilter>
      </pluginConfig>
    </sequence>

  </job>
</joblist>
""".toString()
        when:
        def result = JobsXMLCodec.decode(xml)

        then:
        result.size()==1
        result[0].jobName=='test job 1'
        result[0].workflow.pluginConfigMap == [LogFilter: [expected]]
        result[0].workflow.getPluginConfigData('LogFilter') == [expected]
        result[0].workflow.getPluginConfigDataList('LogFilter') == [expected]

        where:
        configxml                     | expected
        '<config>\n' +
                '            <color>red</color>\n' +
                '            <replacement>[SECURE]</replacement>\n' +
                '          </config>' | [type: 'mask-passwords', config: [color: 'red', replacement: '[SECURE]']]
        '<config></config>'           | [type: 'mask-passwords']
        ''                            | [type: 'mask-passwords']

    }
    def "decode workflow global empty plugin config "() {
        given:
        def xml = """<joblist>
  <job>
    <description>ddddd</description>
    <executionEnabled>true</executionEnabled>

    <loglevel>INFO</loglevel>
    <name>test job 1</name>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='teststrateg'>
      <command>
        <exec>echo hi</exec>
      </command>
      <pluginConfig>
      </pluginConfig>
    </sequence>

  </job>
</joblist>
""".toString()
        when:
        def result = JobsXMLCodec.decode(xml)

        then:
        result.size()==1
        result[0].jobName=='test job 1'
        result[0].workflow.pluginConfigMap == null
        result[0].workflow.getPluginConfigData('LogFilter') == null
        result[0].workflow.getPluginConfigDataList('LogFilter') == null


    }

    def "decode workflow global log filter plugin config multi entry"() {
        given:
        def xml = """<joblist>
  <job>
    <description>ddddd</description>
    <executionEnabled>true</executionEnabled>

    <loglevel>INFO</loglevel>
    <name>test job 1</name>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='teststrateg'>
      <command>
        <exec>echo hi</exec>
      </command>
      <pluginConfig>
        <LogFilter type='mask-passwords'>
          $configxml
        </LogFilter>
        <LogFilter type='key-value-data'>
          <config>
            <debugOnly>true</debugOnly>
            <regex>something</regex>
          </config>
        </LogFilter>
      </pluginConfig>
    </sequence>

  </job>
</joblist>
""".toString()
        when:
        def result = JobsXMLCodec.decode(xml)

        then:
        result.size() == 1
        result[0].jobName == 'test job 1'
        result[0].workflow.pluginConfigMap ==
                [LogFilter: [expected,
                             [type  : 'key-value-data',
                              config: [
                                      regex    : 'something',
                                      debugOnly: 'true'
                              ]]]]
        result[0].workflow.getPluginConfigData('LogFilter') ==
                [expected,
                 [type  : 'key-value-data',
                  config: [
                          regex    : 'something',
                          debugOnly: 'true'
                  ]]]
        result[0].workflow.getPluginConfigDataList('LogFilter') ==
                [expected,
                 [type  : 'key-value-data',
                  config: [
                          regex    : 'something',
                          debugOnly: 'true'
                  ]]]

        where:
        configxml                     | expected
        '<config>\n' +
                '            <color>red</color>\n' +
                '            <replacement>[SECURE]</replacement>\n' +
                '          </config>' | [type: 'mask-passwords', config: [color: 'red', replacement: '[SECURE]']]
        '<config></config>'           | [type: 'mask-passwords']
        ''                            | [type: 'mask-passwords']
    }


    def "encode option type file"() {
        given:
        def XmlSlurper parser = new XmlSlurper()
        def jobs1 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [new CommandExec(
                                        [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese ' +
                                                '-particle']
                                )],
                                ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,
                        options: [
                                new Option(
                                        name: 'testop1',
                                        required: false,
                                        enforced: false,
                                        optionType: 'file',
                                        configData: '{"test":"value","test2":"data2"}'
                                )
                        ]
                )
        ]
        when:
        def xmlstr = JobsXMLCodec.encode(jobs1)

        then:
        null != xmlstr
        xmlstr instanceof String

        def doc = parser.parse(new StringReader(xmlstr))
        doc.name() == 'joblist'
        doc.job.size() == 1
        doc.job[0].name[0].text() == 'test job 1'
        doc.job[0].context.size() == 1
        doc.job[0].context[0].options.size() == 1
        doc.job[0].context[0].options[0].option.size() == 1
        doc.job[0].context[0].options[0].option[0].'@name' == 'testop1'
        doc.job[0].context[0].options[0].option[0].'@type' == 'file'
        doc.job[0].context[0].options[0].option[0].config.size() == 1
        doc.job[0].context[0].options[0].option[0].config[0].entry.size() == 2
        doc.job[0].context[0].options[0].option[0].config[0].entry[0].'@key' == 'test'
        doc.job[0].context[0].options[0].option[0].config[0].entry[0].'@value' == 'value'
        doc.job[0].context[0].options[0].option[0].config[0].entry[1].'@key' == 'test2'
        doc.job[0].context[0].options[0].option[0].config[0].entry[1].'@value' == 'data2'
    }

    @Unroll
    def "encode option multivalued all selected #mvas"() {
        given:
        def XmlSlurper parser = new XmlSlurper()
        def jobs1 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [new CommandExec(
                                        [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese ' +
                                                '-particle']
                                )],
                                ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,
                        options: [
                                new Option(
                                        name: 'testop1',
                                        required: false,
                                        enforced: false,
                                        multivalued: true,
                                        delimiter: ',',
                                        multivalueAllSelected: mvas
                                )
                        ]
                )
        ]
        when:
        def xmlstr = JobsXMLCodec.encode(jobs1)

        then:
        null != xmlstr
        xmlstr instanceof String

        def doc = parser.parse(new StringReader(xmlstr))
        doc.name() == 'joblist'
        doc.job.size() == 1
        doc.job[0].name[0].text() == 'test job 1'
        doc.job[0].context.size() == 1
        doc.job[0].context[0].options.size() == 1
        doc.job[0].context[0].options[0].option.size() == 1
        doc.job[0].context[0].options[0].option[0].'@name' == 'testop1'
        doc.job[0].context[0].options[0].option[0].'@multivalued' == 'true'
        doc.job[0].context[0].options[0].option[0].'@delimiter' == ','
        doc.job[0].context[0].options[0].option[0].'@multivalueAllSelected'.text() == res

        where:
        mvas  | res
        true  | 'true'
        false | ''
    }


    def "decode option file type"() {
        given:
        def xml = '''<joblist>
  <job>
    <description>ddddd</description>
    <executionEnabled>true</executionEnabled>
    <context>
        <options>
            <option name="testopt1" type="atype">
                <config>
                    <entry key="asdf" value="xyxy"/>
                    <entry key="monkey" value="donut"/>
                </config>
            </option>
        </options>
    </context>
    <loglevel>INFO</loglevel>
    <name>test job 1</name>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='teststrateg'>
      <command>
        <exec>echo hi</exec>
      </command>
    </sequence>

  </job>
</joblist>'''

        when:
        def result = JobsXMLCodec.decode(xml)

        then:
        result.size() == 1
        result[0].jobName == 'test job 1'
        result[0].options.size() == 1
        result[0].options[0].name == 'testopt1'
        result[0].options[0].optionType == 'atype'
        result[0].options[0].configMap == ['asdf': 'xyxy', 'monkey': 'donut']

    }

    @Unroll
    def "decode option multivalued all selected"() {
        given:
        def xml = """<joblist>
  <job>
    <description>ddddd</description>
    <executionEnabled>true</executionEnabled>
    <context>
        <options>
            <option name="testopt1" type="atype" multivalued="true" $text>
            </option>
        </options>
    </context>
    <loglevel>INFO</loglevel>
    <name>test job 1</name>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='teststrateg'>
      <command>
        <exec>echo hi</exec>
      </command>
    </sequence>

  </job>
</joblist>
"""
        when:
        def result = JobsXMLCodec.decode(xml.toString())

        then:
        result.size() == 1
        result[0].jobName == 'test job 1'
        result[0].options.size() == 1
        result[0].options[0].name == 'testopt1'
        result[0].options[0].multivalued
        result[0].options[0].multivalueAllSelected == resval

        where:
        text                            | resval
        'multivalueAllSelected="true"'  | true
        'multivalueAllSelected="false"' | false
        'multivalueAllSelected=""'      | false
        ''                              | false

    }


    @Unroll
    def "decode retry"() {
        given:
        def xml = """<joblist>
  <job>
    <description>ddddd</description>
    <executionEnabled>true</executionEnabled>
    """+text+"""
    <loglevel>INFO</loglevel>
    <name>test job 1</name>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='false' strategy='teststrateg'>
      <command>
        <exec>echo hi</exec>
      </command>
    </sequence>

  </job>
</joblist>
"""
        when:
        def result = JobsXMLCodec.decode(xml.toString())

        then:
        result.size() == 1
        result[0].jobName == 'test job 1'
        result[0].retry == retryVal
        result[0].retryDelay == delayVal

        where:
        text                            | retryVal | delayVal
        '<retry>2</retry>'              | '2'      | null
        '<retry delay="1h">2</retry>'   | '2'      | '1h'
        ''                              | null     | null

    }


    @Unroll
    def "encode with retry"() {
        given:
        def XmlSlurper parser = new XmlSlurper()
        def jobs1 = [
                new ScheduledExecution(
                        jobName: 'test job 1',
                        description: 'test descrip',
                        loglevel: 'INFO',
                        project: 'test1',
                        workflow: new Workflow(
                                keepgoing: true,
                                commands: [new CommandExec(
                                        [adhocRemoteString: 'test buddy', argString: ' -monkey cheese ' +
                                                '-particle']
                                )],
                        ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true,
                        retry:retry,
                        retryDelay: delay
                )
        ]
        when:
        def xmlstr = JobsXMLCodec.encode(jobs1)

        then:
        null != xmlstr
        xmlstr instanceof String
        if(delay){
            xmlstr.contains('delay='+delay)
        }else{
            !xmlstr.contains('delay=')
        }
        def doc = parser.parse(new StringReader(xmlstr))
        doc.name() == 'joblist'
        doc.job.size() == 1
        doc.job[0].name[0].text() == 'test job 1'
        doc.job[0].retry == retry

        where:
        retry  | delay
        '2'    | '1h'
        '2'    | null
    }
}
