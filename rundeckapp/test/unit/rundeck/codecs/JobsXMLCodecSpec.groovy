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
}
