package rundeck
/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.junit.Test
import org.yaml.snakeyaml.Yaml
import rundeck.codecs.JobsYAMLCodec

import static org.junit.Assert.*
/*
* rundeck.JobsYAMLCodecTests.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jan 24, 2011 10:36:06 AM
*
*/

public class JobsYAMLCodecTests  {

    @Test
    void testEncodeBasicScheduleEnabled() {
        def Yaml yaml = new Yaml()
        ScheduledExecution se = new ScheduledExecution([
                jobName: 'test job 1',
                description: 'test descrip',
                loglevel: 'INFO',
                project: 'test1',
                workflow: new Workflow([keepgoing: false, threadcount: 1, commands: [new CommandExec([adhocRemoteString: 'test script',description: 'test1']),
                                                                                     new CommandExec([adhocLocalString: "#!/bin/bash\n\necho test bash\n\necho tralaala 'something'\n", description: 'test2']),
                                                                                     new CommandExec([adhocFilepath: 'some file path', description: 'test3']),
                                                                                     new JobExec([jobName: 'another job', jobGroup: 'agroup', nodeStep:true, description: 'test4']),
                                                                                     new CommandExec([adhocFilepath: 'http://example.com/blah', description: 'test5']),
                ]]),
                options: [new Option(name: 'opt1', description: "an opt", defaultValue: "xyz", enforced: true, required: true, valuesList:'a,b')] as TreeSet,
                nodeThreadcount: 1,
                nodeKeepgoing: true,
                doNodedispatch: true,
                nodeInclude: "testhost1",
                nodeExcludeName: "x1",
                scheduled: true,
                seconds: '*',
                minute: '0',
                hour: '2',
                month: '3',
                dayOfMonth: '?',
                dayOfWeek: '4',
                year: '2011',
                scheduleEnabled: true,
                executionEnabled: true
        ])
        def jobs1 = [se]
        def  ymlstr = JobsYAMLCodec.encode(jobs1)
        assertNotNull ymlstr
        assertTrue ymlstr instanceof String


        def doc = yaml.load(ymlstr)
        assertNotNull doc
        assertEquals "wrong number of jobs", 1, doc.size()
        assertEquals "wrong name", "test job 1", doc[0].name

        assertEquals "wrong scheduleEnabled", true, doc[0].scheduleEnabled
        assertEquals "wrong executionEnabled", true, doc[0].executionEnabled

    }
    @Test
    void testEncodeBasicScheduleDisabled() {
        def Yaml yaml = new Yaml()
        ScheduledExecution se = new ScheduledExecution([
                jobName: 'test job 1',
                description: 'test descrip',
                loglevel: 'INFO',
                project: 'test1',
                workflow: new Workflow([keepgoing: false, threadcount: 1, commands: [new CommandExec([adhocRemoteString: 'test script',description: 'test1']),
                                                                                     new CommandExec([adhocLocalString: "#!/bin/bash\n\necho test bash\n\necho tralaala 'something'\n", description: 'test2']),
                                                                                     new CommandExec([adhocFilepath: 'some file path', description: 'test3']),
                                                                                     new JobExec([jobName: 'another job', jobGroup: 'agroup', nodeStep:true, description: 'test4']),
                                                                                     new CommandExec([adhocFilepath: 'http://example.com/blah', description: 'test5']),
                ]]),
                options: [new Option(name: 'opt1', description: "an opt", defaultValue: "xyz", enforced: true, required: true, valuesList:'a,b')] as TreeSet,
                nodeThreadcount: 1,
                nodeKeepgoing: true,
                doNodedispatch: true,
                nodeInclude: "testhost1",
                nodeExcludeName: "x1",
                scheduled: true,
                seconds: '*',
                minute: '0',
                hour: '2',
                month: '3',
                dayOfMonth: '?',
                dayOfWeek: '4',
                year: '2011',
                scheduleEnabled: false,
                executionEnabled: false
        ])
        def jobs1 = [se]
        def  ymlstr = JobsYAMLCodec.encode(jobs1)
        assertNotNull ymlstr
        assertTrue ymlstr instanceof String


        def doc = yaml.load(ymlstr)
        assertNotNull doc
        assertEquals "wrong number of jobs", 1, doc.size()
        assertEquals "wrong name", "test job 1", doc[0].name
        assertEquals "wrong scheduleEnabled", false, doc[0].scheduleEnabled
        assertEquals "wrong executionEnabled", false, doc[0].executionEnabled

    }

    @Test
    void testEncodeTimeout() {
        def Yaml yaml = new Yaml()
        ScheduledExecution se = new ScheduledExecution([
            jobName: 'test job 1',
            description: 'test descrip',
            loglevel: 'INFO',
            project: 'test1',
            timeout: '120m',
            workflow: new Workflow([keepgoing: false, threadcount: 1, commands: [new CommandExec([adhocRemoteString: 'test script',description: 'test1']),
                new CommandExec([adhocLocalString: "#!/bin/bash\n\necho test bash\n\necho tralaala 'something'\n", description: 'test2']),
            ]]),
            nodeThreadcount: 1,
            nodeKeepgoing: true,
            doNodedispatch: true,
            nodeInclude: "testhost1",
            nodeExcludeName: "x1",
        ])
        def jobs1 = [se]
        def  ymlstr = JobsYAMLCodec.encode(jobs1)
        assertNotNull ymlstr
        assertTrue ymlstr instanceof String


        def doc = yaml.load(ymlstr)
        assertNotNull doc
        assertEquals "wrong number of jobs", 1, doc.size()
        assertEquals "wrong name", "test job 1", doc[0].name
        assertEquals "wrong timeout value", "120m", doc[0].timeout
    }
    @Test
    void testEncodeLoglimitNoStatus() {
        def Yaml yaml = new Yaml()
        ScheduledExecution se = new ScheduledExecution([
            jobName: 'test job 1',
            description: 'test descrip',
            loglevel: 'INFO',
            project: 'test1',
            workflow: new Workflow([keepgoing: false, threadcount: 1, commands: [new CommandExec([adhocRemoteString: 'test script',description: 'test1']),
                new CommandExec([adhocLocalString: "#!/bin/bash\n\necho test bash\n\necho tralaala 'something'\n", description: 'test2']),
            ]]),
                logOutputThreshold:'20MB',
                logOutputThresholdAction:'halt',
        ])
        def jobs1 = [se]
        def  ymlstr = JobsYAMLCodec.encode(jobs1)
        assertNotNull ymlstr
        assertTrue ymlstr instanceof String


        def doc = yaml.load(ymlstr)
        assertNotNull doc
        assertEquals "wrong number of jobs", 1, doc.size()
        assertEquals "wrong loglimit", "20MB", doc[0].loglimit
        assertEquals "wrong loglimitAction", "halt", doc[0].loglimitAction
        assertEquals "wrong loglimitAction", null, doc[0].loglimitStatus
    }
    @Test
    void testEncodeLoglimitWithStatus() {
        def Yaml yaml = new Yaml()
        ScheduledExecution se = new ScheduledExecution([
            jobName: 'test job 1',
            description: 'test descrip',
            loglevel: 'INFO',
            project: 'test1',
            workflow: new Workflow([keepgoing: false, threadcount: 1, commands: [new CommandExec([adhocRemoteString: 'test script',description: 'test1']),
                new CommandExec([adhocLocalString: "#!/bin/bash\n\necho test bash\n\necho tralaala 'something'\n", description: 'test2']),
            ]]),
                logOutputThreshold:'20MB',
                logOutputThresholdAction:'halt',
                logOutputThresholdStatus:'mystatus',
        ])
        def jobs1 = [se]
        def  ymlstr = JobsYAMLCodec.encode(jobs1)
        assertNotNull ymlstr
        assertTrue ymlstr instanceof String


        def doc = yaml.load(ymlstr)
        assertNotNull doc
        assertEquals "wrong number of jobs", 1, doc.size()
        assertEquals "wrong loglimit", "20MB", doc[0].loglimit
        assertEquals "wrong loglimitAction", "halt", doc[0].loglimitAction
        assertEquals "wrong loglimitAction", "mystatus", doc[0].loglimitStatus
    }
    @Test
    void testEncodeNotificationPlugin() {
        def Yaml yaml = new Yaml()
        ScheduledExecution se = new ScheduledExecution([
            jobName: 'test job 1',
            description: 'test descrip',
            loglevel: 'INFO',
            project: 'test1',
            workflow: new Workflow([keepgoing: false, threadcount: 1, commands: [
                new CommandExec([adhocFilepath: 'http://example.com/blah'])
            ]]),
            options: [new Option(name: 'opt1', description: "an opt", defaultValue: "xyz", enforced: true, required: true, valuesList:'a,b')] as TreeSet,
            nodeThreadcount: 1,
            nodeKeepgoing: true,
            doNodedispatch: true,
            nodeInclude: "testhost1",
            nodeExcludeName: "x1",
            scheduled: true,
            seconds: '*',
            minute: '0',
            hour: '2',
            month: '3',
            dayOfMonth: '?',
            dayOfWeek: '4',
            year: '2011',
                notifications:[
                    new Notification([eventTrigger:'onsuccess',type:'test1',configuration:["blah":"blee"]])
                ]
        ])
        def jobs1 = [se]
        def  ymlstr = JobsYAMLCodec.encode(jobs1)
        assertNotNull ymlstr
        assertTrue ymlstr instanceof String

        def doc = yaml.load(ymlstr)
        assertNotNull doc
        assertEquals(1,doc[0].notification.size())
        assertEquals(1,doc[0].notification.onsuccess.size())
        assertEquals([type:'test1', configuration:['blah':'blee']],doc[0].notification.onsuccess.plugin)

    }

    /**
     * Multiline string line endings converted to unix style
     */
    @Test
    void testMultilineWhitespaceEncode() {
        def out = JobsYAMLCodec.encodeMaps(
                [
                        [
                                a: 'b\nc\r\nd\re'
                        ]
                ],
        )

        assertEquals(
                '- a: |-\n' +
                        '    b\n' +
                        '    c\n' +
                        '    d\n' +
                        '    e\n',
                out
        )
    }

    /**
     * Multiline string in an array line endings converted to unix style
     */
    @Test
    void testMultilineWhitespaceEncodeArray() {
        def out = JobsYAMLCodec.encodeMaps(
                [
                        [
                                a: ['b\nc\r\nd\re']
                        ]
                ]
        )

        assertEquals(
                '- a:\n' +
                        '  - |-\n' +
                        '    b\n' +
                        '    c\n' +
                        '    d\n' +
                        '    e\n',
                out
        )
    }
    @Test
    void testEncodeScript() {
        def Yaml yaml = new Yaml()
        ScheduledExecution se = new ScheduledExecution([
            jobName: 'test job 1',
            description: 'test descrip',
            loglevel: 'INFO',
            project: 'test1',
            workflow: new Workflow([keepgoing: false, threadcount: 1, commands: [
                new CommandExec(
                    adhocRemoteString: 'abc\n123\rdef\r\nomg'
                )
            ]]),
        ])
        def jobs1 = [se]
        def  ymlstr = JobsYAMLCodec.encode(jobs1)
        assertNotNull ymlstr
        assertTrue ymlstr instanceof String


        def doc = yaml.load(ymlstr)
        assertNotNull doc
        assertEquals(1,doc[0].sequence.commands.size())
        assertEquals([exec:'abc\n' + '123\n' + 'def\n' + 'omg'], doc[0].sequence.commands[0])
    }
    @Test
    void testEncodeStepPlugin() {
        def Yaml yaml = new Yaml()
        ScheduledExecution se = new ScheduledExecution([
            jobName: 'test job 1',
            description: 'test descrip',
            loglevel: 'INFO',
            project: 'test1',
            workflow: new Workflow([keepgoing: false, threadcount: 1, commands: [
                new PluginStep(
                    type: 'monkey',
                    nodeStep: true,
                    configuration: [elf: 'hider']
                )
            ]]),
        ])
        def jobs1 = [se]
        def  ymlstr = JobsYAMLCodec.encode(jobs1)
        assertNotNull ymlstr
        assertTrue ymlstr instanceof String

        def doc = yaml.load(ymlstr)
        assertNotNull doc
        assertEquals(1,doc[0].sequence.commands.size())
        assertEquals([type:'monkey', nodeStep:true, configuration: [elf: 'hider']], doc[0].sequence.commands[0])
    }
    @Test
    void testEncodeStepPluginEmptyConfig() {
        def Yaml yaml = new Yaml()
        ScheduledExecution se = new ScheduledExecution([
            jobName: 'test job 1',
            description: 'test descrip',
            loglevel: 'INFO',
            project: 'test1',
            workflow: new Workflow([keepgoing: false, threadcount: 1, commands: [
                new PluginStep(
                    type: 'monkey',
                    nodeStep: true,
                    configuration: [:]
                )
            ]]),
        ])
        def jobs1 = [se]
        def  ymlstr = JobsYAMLCodec.encode(jobs1)
        assertNotNull ymlstr
        assertTrue ymlstr instanceof String

        def doc = yaml.load(ymlstr)
        assertNotNull doc
        assertEquals(1,doc[0].sequence.commands.size())
        assertEquals([type:'monkey', nodeStep:true], doc[0].sequence.commands[0])
    }
    @Test
    void testEncodeStepPluginNullConfig() {
        def Yaml yaml = new Yaml()
        ScheduledExecution se = new ScheduledExecution([
            jobName: 'test job 1',
            description: 'test descrip',
            loglevel: 'INFO',
            project: 'test1',
            workflow: new Workflow([keepgoing: false, threadcount: 1, commands: [
                new PluginStep(
                    type: 'monkey',
                    nodeStep: true,
                    configuration: null
                )
            ]]),
        ])
        def jobs1 = [se]
        def  ymlstr = JobsYAMLCodec.encode(jobs1)
        assertNotNull ymlstr
        assertTrue ymlstr instanceof String

        def doc = yaml.load(ymlstr)
        assertNotNull doc
        assertEquals(1,doc[0].sequence.commands.size())
        assertEquals([type:'monkey', nodeStep:true], doc[0].sequence.commands[0])
    }
    @Test
    void testDecodeCrontabString() {
            def ymlstr1 = """
- loglevel: INFO
  name: one
  description: Task one.
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - exec: echo foo
        one
  options: {}
  schedule:
    crontab: '13 23 5 9 3 ?'
"""
        def list = JobsYAMLCodec.decode(ymlstr1)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]
        //schedule
        assertTrue "wrong scheduled", se.scheduled
        assertEquals "13 23 5 9 3 ?", se.crontabString
        assertEquals "wrong seconds", "13", se.seconds
        assertEquals "wrong minute", "23", se.minute
        assertEquals "wrong minute", "5", se.hour
        assertEquals "wrong minute", "9", se.dayOfMonth
        assertEquals "wrong minute", "3", se.month
        assertEquals "wrong minute", "?", se.dayOfWeek
        assertEquals "wrong minute", "*", se.year

    }
    @Test
    void testDecodeCrontabString2(){
            def ymlstr1 = """
- loglevel: INFO
  name: one
  description: Task one.
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - exec: echo foo
        one
  options: {}
  schedule:
      crontab: '0 30 */6 ? Jan Mon *'
"""
        def list = JobsYAMLCodec.decode(ymlstr1)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]
        //schedule
        assertTrue "wrong scheduled", se.scheduled
        assertEquals "wrong crontabString", "0 30 */6 ? Jan Mon *", se.crontabString
        assertEquals "wrong seconds", "0", se.seconds
        assertEquals "wrong minute", "30", se.minute
        assertEquals "wrong minute", "*/6", se.hour
        assertEquals "wrong minute", "?", se.dayOfMonth
        assertEquals "wrong minute", "Jan", se.month
        assertEquals "wrong minute", "Mon", se.dayOfWeek
        assertEquals "wrong minute", "*", se.year

    }
    @Test
    void testDecodeSchedule(){
            def ymlstr1 = """
- schedule:
    time:
      hour: '5'
      minute: '23'
      seconds: '13'
    month: '3'
    year: '*/2'
    dayofmonth:
      day: '9'
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - exec: echo foo one
  description: Task one.
  name: one

"""
        def list = JobsYAMLCodec.decode(ymlstr1)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]
        //schedule
        assertTrue "wrong scheduled", se.scheduled
        assertNull "should be null crontabString", se.crontabString
        assertEquals "wrong seconds", "13", se.seconds
        assertEquals "wrong minute", "23", se.minute
        assertEquals "wrong minute", "5", se.hour
        assertEquals "wrong minute", "9", se.dayOfMonth
        assertEquals "wrong minute", "3", se.month
        assertEquals "wrong minute", "?", se.dayOfWeek
        assertEquals "wrong minute", "*/2", se.year

    }

    /**
     * Options defined in list format
     */
    @Test
    void testDecodeOptionsList() {
        def ymlstr2 = """
-
  project: zamp
  loglevel: ERR
  sequence:
    keepgoing: true
    strategy: step-first
    commands:
    - exec: test script
      args: this is redic # IGNORED for exec
    - script: A Monkey returns
      args: whatever
    - scriptfile: /path/to/file
      args: -whatever something -else
    - jobref:
        name: some job
        group: another group
        args: yankee doodle
        nodeStep: true
    - scripturl: http://example.com/path/to/file
      args: -blah bloo -blee
  description: test descrip
  name: test job 1
  group: group/1/2/3
  nodefilters:
    dispatch:
      threadcount: 3
      keepgoing: false
      excludePrecedence: false
    include:
      name: .*
    exclude:
      tags: monkey
      os-family: unix
      os-name: Linux
      os-version: 10.5.*
      os-arch: x86
      hostname: shampoo.*
  schedule:
    time:
      seconds: '0'
      hour: '8/2'
      minute: '0,5,10,35'
    month: '*'
    dayofmonth:
      day: '*'
    year: '2001,2010,2012'
  options:
    - name: opt2
      enforced: true
      required: true
      description: an opt
      value: xyz
      values:
      - a
      - b
    - name: opt1
      enforced: false
      required: false
      description: whatever
      regex: '\\d+'
      valuesUrl: http://something.com
"""
        def list = JobsYAMLCodec.decode(ymlstr2)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]

        //options
        assertNotNull "missing options", se.options
        assertEquals "wrong options size", 2, se.options.size()
        final Iterator iterator = se.options.iterator()
        def opt1 = iterator.next()
        assertEquals "wrong option name", "opt2", opt1.name
        assertEquals "wrong option description", "an opt", opt1.description
        assertEquals "wrong option defaultValue", "xyz", opt1.defaultValue
        assertTrue "wrong option name", opt1.enforced
        assertTrue "wrong option name", opt1.required
        assertNotNull "wrong option values", opt1.optionValues
        assertEquals "wrong option values size", 2, opt1.optionValues.size()
        ArrayList valuesList = new ArrayList(opt1.optionValues)
        assertEquals "wrong option values[0]", 'a', valuesList[0]
        assertEquals "wrong option values[1]", 'b', valuesList[1]
        def opt2 = iterator.next()
        assertEquals "wrong option name", "opt1", opt2.name
        assertEquals "wrong option description", "whatever", opt2.description
        assertNull "wrong option defaultValue", opt2.defaultValue
        assertFalse "wrong option name", opt2.enforced != null && opt2.enforced
        assertFalse "wrong option name", opt2.required != null && opt2.required
        assertNull "wrong option values", opt2.optionValues
        assertNotNull "missing valuesUrl ", opt2.realValuesUrl
        assertEquals "missing valuesUrl ", "http://something.com", opt2.realValuesUrl.toExternalForm()
        assertEquals "wrong option regex", "\\d+", opt2.regex
    }




    @Test void testDecodeLoglimit() {
        def ymlstr1 = """- id: null
  project: test1
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - exec: test script
      description: test1
  description: ''
  name: test job 1
  group: my group
  loglimit: '20MB'
  loglimitAction: 'halt'
"""
        def list = JobsYAMLCodec.decode(ymlstr1)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]
        assertEquals "wrong logOutputThreshold", "20MB", se.logOutputThreshold
        assertEquals "wrong logOutputThresholdAction", "halt", se.logOutputThresholdAction
        assertEquals "wrong logOutputThresholdAction", "failed", se.logOutputThresholdStatus

    }
    @Test void testDecodeLoglimitCustomStatus() {
        def ymlstr1 = """- id: null
  project: test1
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - exec: test script
      description: test1
  description: ''
  name: test job 1
  group: my group
  loglimit: '20MB'
  loglimitAction: 'halt'
  loglimitStatus: 'astatus'
"""
        def list = JobsYAMLCodec.decode(ymlstr1)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]
        assertEquals "wrong logOutputThreshold", "20MB", se.logOutputThreshold
        assertEquals "wrong logOutputThresholdAction", "halt", se.logOutputThresholdAction
        assertEquals "wrong logOutputThresholdAction", "astatus", se.logOutputThresholdStatus

    }
    @Test void testDecodeTimeout() {
            def ymlstr1 = """- id: null
  project: test1
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - exec: test script
      description: test1
    - script: A Monkey returns
      description: test2
    - type: plugin1
      nodeStep: false
      description: test3
    - type: plugin2
      nodeStep: true
      description: test4
  description: ''
  name: test job 1
  timeout: '7h'
  group: my group
  nodefilters:
    dispatch:
      threadcount: 1
      keepgoing: true
      excludePrecedence: true
    include:
      hostname: testhost1
    exclude:
      name: x1
  schedule:
    time:
      seconds: 9
      hour: 8
      minute: 5
    month: 11
    weekday:
      day: 0
    year: 2011
  options:
    opt1:
      enforced: true
      required: true
      description: an opt
      value: xyz
      values:
      - a
      - b
"""
            def list = JobsYAMLCodec.decode(ymlstr1)
            assertNotNull list
            assertEquals(1, list.size())
            def obj = list[0]
            assertTrue(obj instanceof ScheduledExecution)
            ScheduledExecution se = (ScheduledExecution) list[0]
            assertEquals "wrong name", "test job 1", se.jobName
            assertEquals "wrong timeout", "7h", se.timeout

    }




    @Test void testDecodeNotificationPlugin(){
        def ymlstr1 = """- id: myid
  project: test1
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - exec: test script
      errorhandler:
        exec: test err
  description: ''
  name: test job 1
  group: my group
  notification:
    onsuccess:
      plugin:
        type: test1
        configuration:
          a: b
          c: d
    onfailure:
      plugin:
        type: test2
        configuration:
          x: yz
"""
        def list = JobsYAMLCodec.decode(ymlstr1)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]


        assertNotNull se.notifications
        assertEquals 2,se.notifications.size()
        def n1=se.notifications.find{it.type=='test1'}
        assertNotNull(n1)
        assertEquals('test1',n1.type)
        assertEquals([a:'b',c:'d'],n1.configuration)
        def n2=se.notifications.find{it.type=='test2'}
        assertNotNull(n2)
        assertEquals('test2', n2.type)
        assertEquals([x:'yz'], n2.configuration)

    }
    @Test void testDecodeStepPlugin(){
        def ymlstr1 = """- project: test1
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - type: monkey
      nodeStep: true
      configuration:
        elf: hider
  description: test descrip
  name: test job 1
"""
        def list = JobsYAMLCodec.decode(ymlstr1)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]


        assertNotNull se.workflow.commands[0]
        assertTrue(se.workflow.commands[0] instanceof  PluginStep)
        PluginStep cmd1= se.workflow.commands[0]
        assertEquals('monkey',cmd1.type)
        assertEquals(true,cmd1.nodeStep)
        assertEquals([elf:'hider'],cmd1.configuration)
    }
    @Test void testDecodeStepPluginEmptyConfig(){
        def ymlstr1 = """- project: test1
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - type: monkey
      nodeStep: true
      configuration:
  description: test descrip
  name: test job 1
"""
        def list = JobsYAMLCodec.decode(ymlstr1)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]


        assertNotNull se.workflow.commands[0]
        assertTrue(se.workflow.commands[0] instanceof  PluginStep)
        PluginStep cmd1= se.workflow.commands[0]
        assertEquals('monkey',cmd1.type)
        assertEquals(true,cmd1.nodeStep)
        assertEquals(null,cmd1.configuration)
    }
    @Test void testDecodeStepPluginNullConfig(){
        def ymlstr1 = """- project: test1
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - type: monkey
      nodeStep: true
  description: test descrip
  name: test job 1
"""
        def list = JobsYAMLCodec.decode(ymlstr1)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]


        assertNotNull se.workflow.commands[0]
        assertTrue(se.workflow.commands[0] instanceof  PluginStep)
        PluginStep cmd1= se.workflow.commands[0]
        assertEquals('monkey',cmd1.type)
        assertEquals(true,cmd1.nodeStep)
        assertEquals(null,cmd1.configuration)
    }

    @Test void testDecodeJobrefBasic() {
        def ymlstr1 = """- project: test1
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - jobref:
        name: jobname
        group: jobgroup
  description: test descrip
  name: test job 1
"""
        def list = JobsYAMLCodec.decode(ymlstr1)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]


        assertNotNull se.workflow.commands[0]
        assertTrue(se.workflow.commands[0] instanceof JobExec)
        JobExec cmd1 = se.workflow.commands[0]
        assertEquals('jobname', cmd1.jobName)
        assertEquals('jobgroup', cmd1.jobGroup)
        assertEquals(false, cmd1.nodeStep)
        assertEquals(null, cmd1.nodeFilter)
        assertEquals(null, cmd1.nodeThreadcount)
        assertEquals(null, cmd1.nodeKeepgoing)
        assertEquals(null, cmd1.nodeRankAttribute)
        assertEquals(null, cmd1.nodeRankOrderAscending)
    }
    @Test void testDecodeJobrefNodeStep() {
        def ymlstr1 = """- project: test1
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - jobref:
        name: jobname
        group: jobgroup
        nodeStep: true
  description: test descrip
  name: test job 1
"""
        def list = JobsYAMLCodec.decode(ymlstr1)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]


        assertNotNull se.workflow.commands[0]
        assertTrue(se.workflow.commands[0] instanceof JobExec)
        JobExec cmd1 = se.workflow.commands[0]
        assertEquals('jobname', cmd1.jobName)
        assertEquals('jobgroup', cmd1.jobGroup)
        assertEquals(true, cmd1.nodeStep)
        assertEquals(null, cmd1.nodeFilter)
        assertEquals(null, cmd1.nodeThreadcount)
        assertEquals(null, cmd1.nodeKeepgoing)
        assertEquals(null, cmd1.nodeRankAttribute)
        assertEquals(null, cmd1.nodeRankOrderAscending)
    }
    @Test void testDecodeJobrefNodefilter() {
        def ymlstr1 = """- project: test1
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - jobref:
        name: jobname
        group: jobgroup
        nodefilters:
          filter: abc def
  description: test descrip
  name: test job 1
"""
        def list = JobsYAMLCodec.decode(ymlstr1)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]


        assertNotNull se.workflow.commands[0]
        assertTrue(se.workflow.commands[0] instanceof JobExec)
        JobExec cmd1 = se.workflow.commands[0]
        assertEquals('jobname', cmd1.jobName)
        assertEquals('jobgroup', cmd1.jobGroup)
        assertEquals('abc def', cmd1.nodeFilter)
        assertEquals(null, cmd1.nodeThreadcount)
        assertEquals(null, cmd1.nodeKeepgoing)
        assertEquals(null, cmd1.nodeRankAttribute)
        assertEquals(null, cmd1.nodeRankOrderAscending)
    }
    @Test void testDecodeJobrefNodefilter_threadcount() {
        def ymlstr1 = """- project: test1
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - jobref:
        name: jobname
        group: jobgroup
        nodefilters:
          dispatch:
            threadcount: 1
          filter: abc def
  description: test descrip
  name: test job 1
"""
        def list = JobsYAMLCodec.decode(ymlstr1)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]


        assertNotNull se.workflow.commands[0]
        assertTrue(se.workflow.commands[0] instanceof JobExec)
        JobExec cmd1 = se.workflow.commands[0]
        assertEquals('jobname', cmd1.jobName)
        assertEquals('jobgroup', cmd1.jobGroup)
        assertEquals('abc def', cmd1.nodeFilter)
        assertEquals(1, cmd1.nodeThreadcount)
        assertEquals(null, cmd1.nodeKeepgoing)
        assertEquals(null, cmd1.nodeRankAttribute)
        assertEquals(null, cmd1.nodeRankOrderAscending)
    }
    @Test void testDecodeJobrefNodefilter_keepgoing() {
        def ymlstr1 = """- project: test1
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - jobref:
        name: jobname
        group: jobgroup
        nodefilters:
          dispatch:
            threadcount: 1
            keepgoing: true
          filter: abc def
  description: test descrip
  name: test job 1
"""
        def list = JobsYAMLCodec.decode(ymlstr1)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]


        assertNotNull se.workflow.commands[0]
        assertTrue(se.workflow.commands[0] instanceof JobExec)
        JobExec cmd1 = se.workflow.commands[0]
        assertEquals('jobname', cmd1.jobName)
        assertEquals('jobgroup', cmd1.jobGroup)
        assertEquals('abc def', cmd1.nodeFilter)
        assertEquals(1, cmd1.nodeThreadcount)
        assertEquals(true, cmd1.nodeKeepgoing)
        assertEquals(null, cmd1.nodeRankAttribute)
        assertEquals(null, cmd1.nodeRankOrderAscending)
    }
    @Test void testDecodeJobrefNodefilter_keepgoingFalse() {
        def ymlstr1 = """- project: test1
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - jobref:
        name: jobname
        group: jobgroup
        nodefilters:
          dispatch:
            threadcount: 1
            keepgoing: false
          filter: abc def
  description: test descrip
  name: test job 1
"""
        def list = JobsYAMLCodec.decode(ymlstr1)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]


        assertNotNull se.workflow.commands[0]
        assertTrue(se.workflow.commands[0] instanceof JobExec)
        JobExec cmd1 = se.workflow.commands[0]
        assertEquals('jobname', cmd1.jobName)
        assertEquals('jobgroup', cmd1.jobGroup)
        assertEquals('abc def', cmd1.nodeFilter)
        assertEquals(1, cmd1.nodeThreadcount)
        assertEquals(false, cmd1.nodeKeepgoing)
        assertEquals(null, cmd1.nodeRankAttribute)
        assertEquals(null, cmd1.nodeRankOrderAscending)
    }
    @Test void testDecodeJobrefNodefilter_rankAttribute() {
        def ymlstr1 = """- project: test1
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - jobref:
        name: jobname
        group: jobgroup
        nodefilters:
          dispatch:
            threadcount: 1
            keepgoing: false
            rankAttribute: rank
          filter: abc def
  description: test descrip
  name: test job 1
"""
        def list = JobsYAMLCodec.decode(ymlstr1)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]


        assertNotNull se.workflow.commands[0]
        assertTrue(se.workflow.commands[0] instanceof JobExec)
        JobExec cmd1 = se.workflow.commands[0]
        assertEquals('jobname', cmd1.jobName)
        assertEquals('jobgroup', cmd1.jobGroup)
        assertEquals('abc def', cmd1.nodeFilter)
        assertEquals(1, cmd1.nodeThreadcount)
        assertEquals(false, cmd1.nodeKeepgoing)
        assertEquals('rank', cmd1.nodeRankAttribute)
        assertEquals(null, cmd1.nodeRankOrderAscending)
    }
    @Test void testDecodeJobrefNodefilter_rankOrder() {
        def ymlstr1 = """- project: test1
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - jobref:
        name: jobname
        group: jobgroup
        nodefilters:
          dispatch:
            threadcount: 1
            keepgoing: false
            rankAttribute: rank
            rankOrder: ascending
          filter: abc def
  description: test descrip
  name: test job 1
"""
        def list = JobsYAMLCodec.decode(ymlstr1)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]


        assertNotNull se.workflow.commands[0]
        assertTrue(se.workflow.commands[0] instanceof JobExec)
        JobExec cmd1 = se.workflow.commands[0]
        assertEquals('jobname', cmd1.jobName)
        assertEquals('jobgroup', cmd1.jobGroup)
        assertEquals('abc def', cmd1.nodeFilter)
        assertEquals(1, cmd1.nodeThreadcount)
        assertEquals(false, cmd1.nodeKeepgoing)
        assertEquals('rank', cmd1.nodeRankAttribute)
        assertEquals(true, cmd1.nodeRankOrderAscending)
    }
    @Test void testDecodeJobrefNodefilter_rankOrderDescending() {
        def ymlstr1 = """- project: test1
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - jobref:
        name: jobname
        group: jobgroup
        nodefilters:
          dispatch:
            threadcount: 1
            keepgoing: false
            rankAttribute: rank
            rankOrder: descending
          filter: abc def
  description: test descrip
  name: test job 1
"""
        def list = JobsYAMLCodec.decode(ymlstr1)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]


        assertNotNull se.workflow.commands[0]
        assertTrue(se.workflow.commands[0] instanceof JobExec)
        JobExec cmd1 = se.workflow.commands[0]
        assertEquals('jobname', cmd1.jobName)
        assertEquals('jobgroup', cmd1.jobGroup)
        assertEquals('abc def', cmd1.nodeFilter)
        assertEquals(1, cmd1.nodeThreadcount)
        assertEquals(false, cmd1.nodeKeepgoing)
        assertEquals('rank', cmd1.nodeRankAttribute)
        assertEquals(false, cmd1.nodeRankOrderAscending)
    }
    @Test void testShouldPassthruCrontabString() {
        def ymlstr2 = """
-
  project: zamp
  loglevel: ERR
  sequence:
    keepgoing: true
    strategy: step-first
    commands:
    - exec: test script
  description: test descrip
  name: test job 1
  group: group/1/2/3
  schedule:
    crontab: 0 0,5,10,35 8/2 * * ? 2001,2010,2012
"""
        def list = JobsYAMLCodec.decode(ymlstr2)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]

        //schedule
        assertTrue "wrong scheduled", se.scheduled
        assertEquals "wrong crontabstring", "0 0,5,10,35 8/2 * * ? 2001,2010,2012", se.crontabString

    }

    @Test void testNotificationThreshold() {
        def Yaml yaml = new Yaml()
        ScheduledExecution se = new ScheduledExecution(
                jobName:'test job 1',
                description:'test descrip',
                loglevel: 'INFO',
                project:'test1',
                timeout:'2h',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                options:[new Option([name:'threadCount',defaultValue:'30'])] as TreeSet,
                nodeThreadcountDynamic: "15",
                nodeKeepgoing:true,
                doNodedispatch:true,
                notifications: [
                        new Notification(eventTrigger: 'avgduration', type: 'email', content: 'test2@example.com')
                ],
                notifyAvgDurationThreshold: '30s'
        )

        def jobs1 = [se]
        def  ymlstr = JobsYAMLCodec.encode(jobs1)
        assertNotNull ymlstr
        assertTrue ymlstr instanceof String


        def doc = yaml.load(ymlstr)
        assertNotNull doc
        assertEquals "wrong number of jobs", 1, doc.size()
        assertEquals "wrong name", "test job 1", doc[0].name
        assertEquals "incorrect notification Threshold","30s",doc[0].notifyAvgDurationThreshold

    }

    @Test void testEncodeThreadCountFromOption() {
        def Yaml yaml = new Yaml()
        ScheduledExecution se = new ScheduledExecution(
                jobName:'test job 1',
                description:'test descrip',
                loglevel: 'INFO',
                project:'test1',
                timeout:'2h',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                options:[new Option([name:'threadCount',defaultValue:'30'])] as TreeSet,
                nodeThreadcountDynamic: "\${option.threadCount}",
                nodeKeepgoing:true,
                doNodedispatch:true
        )

        def jobs1 = [se]
        def  ymlstr = JobsYAMLCodec.encode(jobs1)
        assertNotNull ymlstr
        assertTrue ymlstr instanceof String


        def doc = yaml.load(ymlstr)
        assertNotNull doc
        assertEquals "wrong number of jobs", 1, doc.size()
        assertEquals "wrong name", "test job 1", doc[0].name
        assertEquals "incorrect dispatch threadcount","\${option.threadCount}",doc[0].nodefilters.dispatch.threadcount


    }

    @Test void testEncodeThreadCountFromValue() {
        def Yaml yaml = new Yaml()
        ScheduledExecution se = new ScheduledExecution(
                jobName:'test job 1',
                description:'test descrip',
                loglevel: 'INFO',
                project:'test1',
                timeout:'2h',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                options:[new Option([name:'threadCount',defaultValue:'30'])] as TreeSet,
                nodeThreadcountDynamic: "15",
                nodeKeepgoing:true,
                doNodedispatch:true
        )

        def jobs1 = [se]
        def  ymlstr = JobsYAMLCodec.encode(jobs1)
        assertNotNull ymlstr
        assertTrue ymlstr instanceof String


        def doc = yaml.load(ymlstr)
        assertNotNull doc
        assertEquals "wrong number of jobs", 1, doc.size()
        assertEquals "wrong name", "test job 1", doc[0].name
        assertEquals "incorrect dispatch threadcount","15",doc[0].nodefilters.dispatch.threadcount


    }

}
