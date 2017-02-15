package rundeck.codecs

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import rundeck.CommandExec
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Specification

/**
 * Created by greg on 5/17/16.
 */
@TestMixin(GrailsUnitTestMixin)
class JobsXMLCodecSpec extends Specification {
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
}
