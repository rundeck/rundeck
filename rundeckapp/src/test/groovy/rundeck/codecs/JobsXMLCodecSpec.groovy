package rundeck.codecs

import com.dtolabs.rundeck.plugins.ServiceNameConstants
import groovy.xml.MarkupBuilder
import rundeck.codecs.JobsXMLCodec
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

    def "encode exec lifecycle plugin "() {
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
                                    strategy: 'test'
                            ),
                            nodeThreadcount: 1,
                            nodeKeepgoing: true,
                            doNodedispatch: true,
                            pluginConfigMap: [
                                    (ServiceNameConstants.ExecutionLifecycle): [
                                            plug1: [a: 'b',b:'c'],
                                            plug2: [c:['d','e','f']],
                                            plug3: [g:['h/bingle':'i']]
                                    ]
                            ]
                    )
            ]
        when:
            def xmlstr = JobsXMLCodec.encode(jobs1)

        then:
            null != xmlstr
            xmlstr instanceof String
            System.err.println(xmlstr)
            def doc = parser.parse(new StringReader(xmlstr))
            doc.name() == 'joblist'
            doc.job.size() == 1
            doc.job[0].plugins.size() == 1
            doc.job[0].plugins[0].ExecutionLifecycle.size()==3
            doc.job[0].plugins[0].ExecutionLifecycle[0].'@type'.text()=='plug1'
            doc.job[0].plugins[0].ExecutionLifecycle[0].configuration.size()==1
            doc.job[0].plugins[0].ExecutionLifecycle[0].configuration[0].'@data'.text()=='true'
            doc.job[0].plugins[0].ExecutionLifecycle[0].configuration[0].map.size()==1
            doc.job[0].plugins[0].ExecutionLifecycle[0].configuration[0].map[0].value.size()==2
            doc.job[0].plugins[0].ExecutionLifecycle[0].configuration[0].map[0].value[0].'@key'.text()=='a'
            doc.job[0].plugins[0].ExecutionLifecycle[0].configuration[0].map[0].value[0].text()=='b'
            doc.job[0].plugins[0].ExecutionLifecycle[0].configuration[0].map[0].value[1].'@key'.text()=='b'
            doc.job[0].plugins[0].ExecutionLifecycle[0].configuration[0].map[0].value[1].text()=='c'


            doc.job[0].plugins[0].ExecutionLifecycle[1].'@type'.text()=='plug2'
            doc.job[0].plugins[0].ExecutionLifecycle[1].configuration.size()==1
            doc.job[0].plugins[0].ExecutionLifecycle[1].configuration[0].'@data'.text()=='true'
            doc.job[0].plugins[0].ExecutionLifecycle[1].configuration[0].map.size()==1
            doc.job[0].plugins[0].ExecutionLifecycle[1].configuration[0].map[0].list.size()==1
            doc.job[0].plugins[0].ExecutionLifecycle[1].configuration[0].map[0].list[0].'@key'.text()=='c'
            doc.job[0].plugins[0].ExecutionLifecycle[1].configuration[0].map[0].list[0].value.size()==3
            doc.job[0].plugins[0].ExecutionLifecycle[1].configuration[0].map[0].list[0].value[0].text()=='d'
            doc.job[0].plugins[0].ExecutionLifecycle[1].configuration[0].map[0].list[0].value[1].text()=='e'
            doc.job[0].plugins[0].ExecutionLifecycle[1].configuration[0].map[0].list[0].value[2].text()=='f'

            doc.job[0].plugins[0].ExecutionLifecycle[2].'@type'.text()=='plug3'
            doc.job[0].plugins[0].ExecutionLifecycle[2].configuration.size()==1
            doc.job[0].plugins[0].ExecutionLifecycle[2].configuration[0].map.size()==1
            doc.job[0].plugins[0].ExecutionLifecycle[2].configuration[0].map[0].map.size()==1
            doc.job[0].plugins[0].ExecutionLifecycle[2].configuration[0].map[0].map[0].'@key'.text()=='g'
            doc.job[0].plugins[0].ExecutionLifecycle[2].configuration[0].map[0].map[0].value.size()==1
            doc.job[0].plugins[0].ExecutionLifecycle[2].configuration[0].map[0].map[0].value[0].'@key'=='h/bingle'
            doc.job[0].plugins[0].ExecutionLifecycle[2].configuration[0].map[0].map[0].value[0].text()=='i'
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
    def "decode exec lifecycle plugin multiple"(){
        given:
        def xml = '''<joblist>
  <job>
    <description>test descrip</description>
    <dispatch>
      <excludePrecedence>true</excludePrecedence>
      <keepgoing>true</keepgoing>
      <rankOrder>ascending</rankOrder>
      <successOnEmptyNodeFilter>false</successOnEmptyNodeFilter>
      <threadcount>1</threadcount>
    </dispatch>
    <executionEnabled>true</executionEnabled>
    <loglevel>INFO</loglevel>
    <name>test job 1</name>
    <nodeFilterEditable>false</nodeFilterEditable>
    <nodefilters>
      <filter></filter>
    </nodefilters>
    <nodesSelectedByDefault>true</nodesSelectedByDefault>
    <plugins>
      <ExecutionLifecycle type='plug1'>
        <configuration data='true'>
          <map>
              <value key='a'>b</value>
              <value key='b'>c</value>
          </map>
        </configuration>
      </ExecutionLifecycle>
      <ExecutionLifecycle type='plug2'>
        <configuration data='true'>
          <map>
              <list key='c'>
                <value>d</value>
                <value>e</value>
                <value>f</value>
              </list>
          </map>
        </configuration>
      </ExecutionLifecycle>
      <ExecutionLifecycle type='plug3'>
        <configuration  data='true'>
          <map>
              <map key='g'>
                <value key='h/bingle'>i</value>
              </map>
          </map>
        </configuration>
      </ExecutionLifecycle>
    </plugins>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='true' strategy='test'>
      <command>
        <exec>test buddy</exec>
      </command>
    </sequence>
  </job>
</joblist>'''

        when:
        def result = JobsXMLCodec.decode(xml)

        then:
        result.size()==1
        result[0].jobName=='test job 1'
        result[0].pluginConfigMap==[
                (ServiceNameConstants.ExecutionLifecycle): [
                        plug1: [a: 'b',b:'c'],
                        plug2: [c:['d','e','f']],
                        plug3: [g:['h/bingle':'i']]
                ]
        ]

    }
    def "decode exec lifecycle plugin single"(){
        given:
        def xml = '''<joblist>
  <job>
    <description>test descrip</description>
    <dispatch>
      <excludePrecedence>true</excludePrecedence>
      <keepgoing>true</keepgoing>
      <rankOrder>ascending</rankOrder>
      <successOnEmptyNodeFilter>false</successOnEmptyNodeFilter>
      <threadcount>1</threadcount>
    </dispatch>
    <executionEnabled>true</executionEnabled>
    <loglevel>INFO</loglevel>
    <name>test job 1</name>
    <nodeFilterEditable>false</nodeFilterEditable>
    <nodefilters>
      <filter></filter>
    </nodefilters>
    <nodesSelectedByDefault>true</nodesSelectedByDefault>
    <plugins>
      <ExecutionLifecycle type='plug1'>
        <configuration data='true'>
          <map>
              <value key='a'>b</value>
              <value key='b'>c</value>
          </map>
        </configuration>
      </ExecutionLifecycle>
    </plugins>
    <scheduleEnabled>true</scheduleEnabled>
    <sequence keepgoing='true' strategy='test'>
      <command>
        <exec>test buddy</exec>
      </command>
    </sequence>
  </job>
</joblist>'''

        when:
        def result = JobsXMLCodec.decode(xml)

        then:
        result.size()==1
        result[0].jobName=='test job 1'
        result[0].pluginConfigMap==[
                (ServiceNameConstants.ExecutionLifecycle): [
                        plug1: [a: 'b',b:'c'],
                ]
        ]

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
    def "decode option multivalued "() {
        given:
        def xml = """<joblist>
  <job>
    <description>ddddd</description>
    <executionEnabled>true</executionEnabled>
    <context>
        <options>
            <option name="testopt1" type="atype" $text delimiter=",">
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
        result[0].options[0].multivalued == resval
        result[0].options[0].delimiter == delim

        where:
        text                     || resval || delim
        'multivalued="true"'     || true   || ','
        'multivalued="false"'    || null   || null
        'multivalued=""'         || null   || null
        'multivalued="asdfasdf"' || null   || null
        ''                       || null   || null

    }

    @Unroll
    def "decode option isDate "() {
        given:
        def xml = """<joblist>
  <job>
    <description>ddddd</description>
    <executionEnabled>true</executionEnabled>
    <context>
        <options>
            <option name="testopt1" type="atype" $text >
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
        result[0].options[0].isDate == resval

        where:
        text                || resval
        'isDate="true"'     || true
        'isDate="false"'    || false
        'isDate=""'         || false
        'isDate="asdfasdf"' || false
        ''                  || false

    }

    @Unroll
    def "decode option preserve order value"() {
        given:
        def xml = """<joblist>
  <job>
    <description>ddddd</description>
    <executionEnabled>true</executionEnabled>
    <context>
        <options $text>
            <option name="z" type="atype" >
            </option>
            <option name="a" type="atype" >
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
        result[0].options.size() == 2
        result[0].options[0].name == vals[0]
        result[0].options[0].sortIndex == indicies[0]
        result[0].options[1].name == vals[1]
        result[0].options[1].sortIndex == indicies[1]

        where:
        text                            | vals       | indicies
        'preserveOrder="true"'          | ['z', 'a'] | [0, 1]
        'preserveOrder="false"'         | ['a', 'z'] | [null, null]
        'preserveOrder="not a boolean"' | ['a', 'z'] | [null, null]
        ''                              | ['a', 'z'] | [null, null]

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
    def "decode multiple executions"() {
        given:
        def xml = """<joblist>
  <job>
    <description>ddddd</description>
    <executionEnabled>true</executionEnabled>
    """ + text + """
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
        result[0].multipleExecutions == expect

        where:
        text                                                     || expect
        '<multipleExecutions>false</multipleExecutions>'         || false
        ''                                                       || false
        '<multipleExecutions>not a boolean</multipleExecutions>' || false
        '<multipleExecutions>true</multipleExecutions>'          || true

    }


    @Unroll
    def "decode successOnEmptyNodeFilter"() {
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
    </sequence>
        <dispatch>
        """ + text + """
        </dispatch>

  </job>
</joblist>
"""
        when:
        def result = JobsXMLCodec.decode(xml.toString())

        then:
        result.size() == 1
        result[0].jobName == 'test job 1'
        result[0].successOnEmptyNodeFilter == expect

        where:
        text                                                                 || expect
        '<successOnEmptyNodeFilter>false</successOnEmptyNodeFilter>'         || false
        ''                                                                   || false
        '<successOnEmptyNodeFilter>not a boolean</successOnEmptyNodeFilter>' || false
        '<successOnEmptyNodeFilter>true</successOnEmptyNodeFilter>'          || true

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


    @Unroll
    def "encode strip uuid from jobref"() {
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
                                commands: [new JobExec(jobName: 'asdf', jobGroup: 'blee', uuid:'xxxxxxxxx')],
                        ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true
                )
        ]
        when:
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        JobsXMLCodec.encodeMapsWithBuilder(jobs1*.toMap(), xml, true, [:], 'uuid')
        def xmlstr = writer.toString()

        then:
        null != xmlstr
        xmlstr instanceof String
        xmlstr.contains("group='blee' name='asdf'")
        def doc = parser.parse(new StringReader(xmlstr))
        doc.name() == 'joblist'
        doc.job.size() == 1
        doc.job[0].name[0].text() == 'test job 1'
        doc.job[0].sequence.command.size()== 1
        doc.job[0].sequence.command[0].jobref.useName=='true'

    }


    @Unroll
    def "encode strip name from jobref"() {
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
                                commands: [new JobExec(jobName: 'asdf', jobGroup: 'blee', uuid:'xxxxxxxxx')],
                        ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true
                )
        ]
        when:
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        JobsXMLCodec.encodeMapsWithBuilder(jobs1*.toMap(), xml, true, [:], 'name')
        def xmlstr = writer.toString()

        then:
        null != xmlstr
        xmlstr instanceof String
        !xmlstr.contains("group='blee' name='asdf'")
        def doc = parser.parse(new StringReader(xmlstr))
        doc.name() == 'joblist'
        doc.job.size() == 1
        doc.job[0].name[0].text() == 'test job 1'
        doc.job[0].sequence.command.size()== 1
        doc.job[0].sequence.command[0].jobref.useName=='false'
        doc.job[0].sequence.command[0].jobref.uuid=='xxxxxxxxx'

    }

    @Unroll
    def "encode strip name from jobref without uuid available"() {
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
                                commands: [new JobExec(jobName: 'asdf', jobGroup: 'blee')],
                        ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true
                )
        ]
        when:
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        JobsXMLCodec.encodeMapsWithBuilder(jobs1*.toMap(), xml, true, [:], 'name')
        def xmlstr = writer.toString()

        then:
        null != xmlstr
        xmlstr instanceof String
        xmlstr.contains("group='blee' name='asdf'")
        def doc = parser.parse(new StringReader(xmlstr))
        doc.name() == 'joblist'
        doc.job.size() == 1
        doc.job[0].name[0].text() == 'test job 1'
        doc.job[0].sequence.command.size()== 1
    }

    @Unroll
    def "encode strip uuid from jobref without name available"() {
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
                                commands: [new JobExec(uuid:'xxxxxxxxx')],
                        ),
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        doNodedispatch: true
                )
        ]
        when:
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        JobsXMLCodec.encodeMapsWithBuilder(jobs1*.toMap(), xml, true, [:], 'uuid')
        def xmlstr = writer.toString()

        then:
        null != xmlstr
        xmlstr instanceof String
        def doc = parser.parse(new StringReader(xmlstr))
        doc.name() == 'joblist'
        doc.job.size() == 1
        doc.job[0].name[0].text() == 'test job 1'
        doc.job[0].sequence.command.size()== 1
        doc.job[0].sequence.command[0].jobref.useName!='true'
        doc.job[0].sequence.command[0].jobref.uuid=='xxxxxxxxx'

    }
}
