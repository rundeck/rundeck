package rundeck.codecs

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import rundeck.CommandExec
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
}
