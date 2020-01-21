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

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.yaml.snakeyaml.Yaml
import rundeck.*
import rundeck.codecs.JobsYAMLCodec
import static org.junit.Assert.*

/*
* JobsYAMLCodecTests.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jan 24, 2011 10:36:06 AM
*
*/

@TestMixin(GrailsUnitTestMixin)
public class JobsYAMLCodecTests  {

    void testEncodeBasic() {
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
            options: [new Option(name: 'opt1', description: "an opt", defaultValue: "xyz", enforced: true, required: true, values: new TreeSet(["a", "b"]))] as TreeSet,
            nodeThreadcount: 1,
            nodeKeepgoing: true,
            doNodedispatch: true,
            nodeInclude: "testhost1",
            nodeExcludeName: "x1",
            scheduled: true,
            seconds: '*',
            minute: '0',
            hour: '2,15',
            month: '3',
            dayOfMonth: '?',
            dayOfWeek: '4',
            year: '2011',
                uuid:UUID.randomUUID().toString()
        ])
        def jobs1 = [se]
        def  ymlstr = JobsYAMLCodec.encode(jobs1)
            assertNotNull ymlstr
            assertTrue ymlstr instanceof String

            ymlstr.contains("hour: '2,15'") //ensure that upgrade doesn't break the formatting of this scalar value
            def doc = yaml.load(ymlstr)
            assertNotNull doc
            assertEquals "wrong number of jobs", 1, doc.size()
            assertEquals "wrong name", "test job 1", doc[0].name
            assertEquals "wrong uuid", jobs1[0].uuid, doc[0].uuid
            assertEquals "wrong description", "test descrip", doc[0].description
            assertEquals "wrong loglevel", "INFO", doc[0].loglevel
            assertEquals "wrong scheduleEnabled", true, doc[0].scheduleEnabled
            assertEquals "wrong executionEnabled", true, doc[0].executionEnabled
            assertEquals "incorrect context project", null, doc[0].project
            assertNotNull "missing sequence", doc[0].sequence
            assertFalse "wrong wf keepgoing", doc[0].sequence.keepgoing
            assertEquals "wrong wf strategy", "node-first", doc[0].sequence.strategy
            assertNotNull "missing commands", doc[0].sequence.commands
            assertEquals "missing commands", 5, doc[0].sequence.commands.size()
            doc[0].sequence.commands.eachWithIndex{cmd,i->
                assertEquals "wrong desc at ${i}", "test${i+1}".toString(), cmd.description
            }
            assertEquals "missing command exec", "test script", doc[0].sequence.commands[0].exec
            assertEquals "missing command script", "#!/bin/bash\n\necho test bash\n\necho tralaala 'something'\n", doc[0].sequence.commands[1].script?.toString()
            assertEquals "missing command scriptfile", "some file path", doc[0].sequence.commands[2].scriptfile
            assertNotNull "missing command jobref", doc[0].sequence.commands[3].jobref
            assertEquals "missing command jobref.name", "another job", doc[0].sequence.commands[3].jobref.name
            assertEquals "missing command jobref.group", "agroup", doc[0].sequence.commands[3].jobref.group
            assertEquals "missing command jobref.group", 'true', doc[0].sequence.commands[3].jobref.nodeStep

            assertEquals "missing command scriptfile", "http://example.com/blah", doc[0].sequence.commands[4].scripturl
            assertNotNull "missing options", doc[0].options
            assertTrue ("wrong type", doc[0].options instanceof Collection)
            assertNotNull "missing option opt1", doc[0].options[0]
            assertEquals "wrong name", "opt1", doc[0].options[0].name
            assertEquals "missing option opt1", "an opt", doc[0].options[0].description
            assertEquals "missing option default", "xyz", doc[0].options[0].value
            assertTrue "missing option enforced", doc[0].options[0].enforced
            assertTrue "missing option required", doc[0].options[0].required
            assertNotNull "missing option values", doc[0].options[0].values
            assertEquals "wrong option values size", 2, doc[0].options[0].values.size()
            assertEquals "wrong option values[0]", "a", doc[0].options[0].values[0]
            assertEquals "wrong option values[1]", "b", doc[0].options[0].values[1]

            assertEquals "incorrect dispatch threadcount", "1", doc[0].nodefilters.dispatch.threadcount
            assertTrue "incorrect dispatch keepgoing", doc[0].nodefilters.dispatch.keepgoing
            assertTrue "incorrect dispatch excludePrecedence", doc[0].nodefilters.dispatch.excludePrecedence
            assertNotNull "missing nodefilters include", doc[0].nodefilters.filter
            assertEquals "wrong nodefilters include hostname", "hostname: testhost1 !name: x1", doc[0].nodefilters.filter
            assertEquals "missing nodefilters exclude name", null, doc[0].nodefilters.include
            assertEquals "missing nodefilters exclude name", null, doc[0].nodefilters.exclude

            assertNotNull "not scheduled", doc[0].schedule
            assertNotNull "not scheduled.time", doc[0].schedule.time
            assertEquals "not scheduled.time", "*", doc[0].schedule.time.seconds
            assertEquals "not scheduled.time", "0", doc[0].schedule.time.minute
            assertEquals "not scheduled.time", "2,15", doc[0].schedule.time.hour
            assertEquals "not scheduled.time", "3", doc[0].schedule.month
            assertEquals "not scheduled.time", "4", doc[0].schedule.weekday.day
            assertEquals "not scheduled.time", "2011", doc[0].schedule.year

    }
    void testEncodeBasicStripUuid() {
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
            options: [new Option(name: 'opt1', description: "an opt", defaultValue: "xyz", enforced: true, required: true, values: new TreeSet(["a", "b"]))] as TreeSet,
            nodeThreadcount: 1,
            nodeKeepgoing: true,
            doNodedispatch: true,
            nodeInclude: "testhost1",
            nodeExcludeName: "x1",
            scheduled: true,
            seconds: '*',
            minute: '0',
            hour: '2,15',
            month: '3',
            dayOfMonth: '?',
            dayOfWeek: '4',
            year: '2011',
            uuid:UUID.randomUUID().toString()
        ])
        def jobs1 = [se]
        def  ymlstr = JobsYAMLCodec.encodeStripUuid(jobs1)
            assertNotNull ymlstr
            assertTrue ymlstr instanceof String


            def doc = yaml.load(ymlstr)
            assertNotNull doc
            assertEquals "wrong number of jobs", 1, doc.size()
            assertEquals "wrong name", "test job 1", doc[0].name
        assertEquals "wrong uuid", null, doc[0].uuid
        assertEquals "wrong id", null, doc[0].id
            assertEquals "wrong description", "test descrip", doc[0].description
            assertEquals "wrong loglevel", "INFO", doc[0].loglevel
            assertEquals "wrong scheduleEnabled", true, doc[0].scheduleEnabled
            assertEquals "wrong executionEnabled", true, doc[0].executionEnabled
            assertEquals "incorrect context project", null, doc[0].project
            assertNotNull "missing sequence", doc[0].sequence
            assertFalse "wrong wf keepgoing", doc[0].sequence.keepgoing
            assertEquals "wrong wf strategy", "node-first", doc[0].sequence.strategy
            assertNotNull "missing commands", doc[0].sequence.commands
            assertEquals "missing commands", 5, doc[0].sequence.commands.size()
            doc[0].sequence.commands.eachWithIndex{cmd,i->
                assertEquals "wrong desc at ${i}", "test${i+1}".toString(), cmd.description
            }
            assertEquals "missing command exec", "test script", doc[0].sequence.commands[0].exec
            assertEquals "missing command script", "#!/bin/bash\n\necho test bash\n\necho tralaala 'something'\n", doc[0].sequence.commands[1].script
            assertEquals "missing command scriptfile", "some file path", doc[0].sequence.commands[2].scriptfile
            assertNotNull "missing command jobref", doc[0].sequence.commands[3].jobref
            assertEquals "missing command jobref.name", "another job", doc[0].sequence.commands[3].jobref.name
            assertEquals "missing command jobref.group", "agroup", doc[0].sequence.commands[3].jobref.group
            assertEquals "missing command jobref.group", 'true', doc[0].sequence.commands[3].jobref.nodeStep

            assertEquals "missing command scriptfile", "http://example.com/blah", doc[0].sequence.commands[4].scripturl
            assertNotNull "missing options", doc[0].options
            assertTrue ("wrong type", doc[0].options instanceof Collection)
            assertNotNull "missing option opt1", doc[0].options[0]
            assertEquals "wrong name", "opt1", doc[0].options[0].name
            assertEquals "missing option opt1", "an opt", doc[0].options[0].description
            assertEquals "missing option default", "xyz", doc[0].options[0].value
            assertTrue "missing option enforced", doc[0].options[0].enforced
            assertTrue "missing option required", doc[0].options[0].required
            assertNotNull "missing option values", doc[0].options[0].values
            assertEquals "wrong option values size", 2, doc[0].options[0].values.size()
            assertEquals "wrong option values[0]", "a", doc[0].options[0].values[0]
            assertEquals "wrong option values[1]", "b", doc[0].options[0].values[1]

            assertEquals "incorrect dispatch threadcount", "1", doc[0].nodefilters.dispatch.threadcount
            assertTrue "incorrect dispatch keepgoing", doc[0].nodefilters.dispatch.keepgoing
            assertTrue "incorrect dispatch excludePrecedence", doc[0].nodefilters.dispatch.excludePrecedence
            assertNotNull "missing nodefilters include", doc[0].nodefilters.filter
            assertEquals "wrong nodefilters include hostname", "hostname: testhost1 !name: x1", doc[0].nodefilters.filter
            assertEquals "missing nodefilters exclude name", null, doc[0].nodefilters.include
            assertEquals "missing nodefilters exclude name", null, doc[0].nodefilters.exclude

            assertNotNull "not scheduled", doc[0].schedule
            assertNotNull "not scheduled.time", doc[0].schedule.time
            assertEquals "not scheduled.time", "*", doc[0].schedule.time.seconds
            assertEquals "not scheduled.time", "0", doc[0].schedule.time.minute
            assertEquals "not scheduled.time", "2,15", doc[0].schedule.time.hour
            assertEquals "not scheduled.time", "3", doc[0].schedule.month
            assertEquals "not scheduled.time", "4", doc[0].schedule.weekday.day
            assertEquals "not scheduled.time", "2011", doc[0].schedule.year

    }

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
                options: [new Option(name: 'opt1', description: "an opt", defaultValue: "xyz", enforced: true, required: true, values: new TreeSet(["a", "b"]))] as TreeSet,
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
                options: [new Option(name: 'opt1', description: "an opt", defaultValue: "xyz", enforced: true, required: true, values: new TreeSet(["a", "b"]))] as TreeSet,
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
            options: [new Option(name: 'opt1', description: "an opt", defaultValue: "xyz", enforced: true, required: true, values: new TreeSet(["a", "b"]))] as TreeSet,
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
    void testEncodeErrorHandlers(){

        def Yaml yaml = new Yaml()
        def errhandlers=[
            new CommandExec([adhocRemoteString: 'err exec']),
            new CommandExec([adhocLocalString: "err script",argString:'err script args',keepgoingOnSuccess:false]),
            new CommandExec([adhocFilepath: 'err file path',argString: 'err file args', keepgoingOnSuccess: true]),
            new JobExec([jobName: 'err job', jobGroup: 'err group',argString: 'err job args', keepgoingOnSuccess: true])
        ]
        ScheduledExecution se = new ScheduledExecution([
            jobName: 'test job 1',
            description: 'test descrip',
            loglevel: 'INFO',
            project: 'test1',
            workflow: new Workflow([keepgoing: false, threadcount: 1, commands: [
                new CommandExec([adhocRemoteString: 'test script',errorHandler:errhandlers[0]]),
                new CommandExec([adhocLocalString: "#!/bin/bash\n\necho test bash\n\necho tralaala 'something'\n", errorHandler: errhandlers[1]]),
                new CommandExec([adhocFilepath: 'some file path', errorHandler: errhandlers[2]]),
                new JobExec([jobName: 'another job', jobGroup: 'agroup', errorHandler: errhandlers[3]]),
            ]]),
            options: [new Option(name: 'opt1', description: "an opt", defaultValue: "xyz", enforced: true, required: true, values: new TreeSet(["a", "b"]))] as TreeSet,
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
            year: '2011'
        ])
        def jobs1 = [se]
        def ymlstr = JobsYAMLCodec.encode(jobs1)
        assertNotNull ymlstr
        assertTrue ymlstr instanceof String


        def doc = yaml.load(ymlstr)
        assertNotNull doc
        assertEquals "wrong number of jobs", 1, doc.size()
        assertNotNull "missing sequence", doc[0].sequence
        assertFalse "wrong wf keepgoing", doc[0].sequence.keepgoing
        assertEquals "wrong wf strategy", "node-first", doc[0].sequence.strategy
        assertNotNull "missing commands", doc[0].sequence.commands
        assertEquals "missing commands", 4, doc[0].sequence.commands.size()
        assertEquals "missing command exec", "test script", doc[0].sequence.commands[0].exec
        assertEquals "missing command script", "#!/bin/bash\n\necho test bash\n\necho tralaala 'something'\n", doc[0].sequence.commands[1].script
        assertEquals "missing command scriptfile", "some file path", doc[0].sequence.commands[2].scriptfile
        assertNotNull "missing command jobref", doc[0].sequence.commands[3].jobref
        assertEquals "missing command jobref.name", "another job", doc[0].sequence.commands[3].jobref.name
        assertEquals "missing command jobref.group", "agroup", doc[0].sequence.commands[3].jobref.group

        //test handlers
        def ndx=0
        assertNotNull doc[0].sequence.commands[ndx].errorhandler
        assertEquals([exec:'err exec'], doc[0].sequence.commands[ndx].errorhandler)

        ndx++
        assertNotNull doc[0].sequence.commands[ndx].errorhandler
        assertEquals([script:'err script',args:'err script args'], doc[0].sequence.commands[ndx].errorhandler)

        ndx++
        assertNotNull doc[0].sequence.commands[ndx].errorhandler
        assertEquals([scriptfile: 'err file path', args: 'err file args', keepgoingOnSuccess:true], doc[0].sequence.commands[ndx].errorhandler)

        ndx++
        assertNotNull doc[0].sequence.commands[ndx].errorhandler
        assertEquals([jobref: [name: 'err job', group:'err group',args: 'err job args'], keepgoingOnSuccess: true], doc[0].sequence.commands[ndx].errorhandler)

    }

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


    void testDecodeBasic1() {
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
            assertEquals "wrong description", "", se.description
            assertEquals "wrong groupPath", "my group", se.groupPath
            assertEquals "wrong project", 'test1', se.project
            assertEquals "wrong loglevel", "INFO", se.loglevel

            assertEquals "wrong scheduleEnabled", true, se.scheduleEnabled
            assertEquals "wrong executionEnabled", true, se.executionEnabled

            assertTrue "wrong doNodedispatch", se.doNodedispatch
            assertEquals "wrong nodeThreadcount", 1, se.nodeThreadcount
            assertTrue "wrong nodeKeepgoing", se.nodeKeepgoing
            assertTrue "wrong nodeExcludePrecedence", se.nodeExcludePrecedence
            assertEquals "wrong nodeInclude", null, se.nodeInclude
            assertEquals "wrong nodeExcludeName", null, se.nodeExcludeName
            assertEquals "wrong nodeInclude", "hostname: testhost1 !name: x1", se.filter

            //schedule
            assertTrue "wrong scheduled", se.scheduled
            assertEquals "wrong seconds", "9", se.seconds
            assertEquals "wrong minute", "5", se.minute
            assertEquals "wrong minute", "8", se.hour
            assertEquals "wrong minute", "11", se.month
            assertEquals "wrong minute", "0", se.dayOfWeek
            assertEquals "wrong minute", "?", se.dayOfMonth
            assertEquals "wrong minute", "2011", se.year

            //workflow
            assertNotNull "missing workflow", se.workflow
            assertNotNull "missing workflow", se.workflow.commands
            assertFalse "wrong workflow.keepgoing", se.workflow.keepgoing
            assertEquals "wrong workflow.strategy", "node-first", se.workflow.strategy
            assertEquals "wrong workflow size", 4, se.workflow.commands.size()
            se.workflow.commands.eachWithIndex { def entry, int i ->
                assertEquals "Wrong description i ${i}","test${i+1}".toString(),entry.description
            }
            assertEquals "wrong workflow item", "test script", se.workflow.commands[0].adhocRemoteString
            assertTrue "wrong workflow item", se.workflow.commands[0].adhocExecution
            assertEquals "wrong workflow item", "A Monkey returns", se.workflow.commands[1].adhocLocalString
            assertTrue "wrong workflow item", se.workflow.commands[1].adhocExecution

            //options
            assertNotNull "missing options", se.options
            assertEquals "wrong options size", 1, se.options.size()
            def opt1 = se.options.iterator().next()
            assertEquals "wrong option name", "opt1", opt1.name
            assertEquals "wrong option description", "an opt", opt1.description
            assertEquals "wrong option defaultValue", "xyz", opt1.defaultValue
            assertTrue "wrong option name", opt1.enforced
            assertTrue "wrong option name", opt1.required
            assertNotNull "wrong option values", opt1.optionValues
            assertEquals "wrong option values size", 2, opt1.optionValues.size()
            ArrayList valuesList = new ArrayList(opt1.optionValues)
            assertEquals "wrong option values[0]", 'a', valuesList[0]
            assertEquals "wrong option values[1]", 'b', valuesList[1]

        }
    /**
     * Options defined in list format
     */
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
    void testDecodeBasic2() {
            def ymlstr1 = """- id: null
  project: test1
  loglevel: INFO
  scheduleEnabled: true
  executionEnabled: true
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
    dayofmonth:
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
            assertEquals "wrong description", "", se.description
            assertEquals "wrong groupPath", "my group", se.groupPath
            assertEquals "wrong project", 'test1', se.project
            assertEquals "wrong loglevel", "INFO", se.loglevel

            assertEquals "wrong scheduleEnabled", true, se.scheduleEnabled
            assertEquals "wrong executionEnabled", true, se.executionEnabled

            assertTrue "wrong doNodedispatch", se.doNodedispatch
            assertEquals "wrong nodeThreadcount", 1, se.nodeThreadcount
            assertTrue "wrong nodeKeepgoing", se.nodeKeepgoing
            assertTrue "wrong nodeExcludePrecedence", se.nodeExcludePrecedence
            assertEquals "wrong nodeInclude", null, se.nodeInclude
            assertEquals "wrong nodeExcludeName", null, se.nodeExcludeName
            assertEquals "wrong nodeInclude", "hostname: testhost1 !name: x1", se.filter

            //schedule
            assertTrue "wrong scheduled", se.scheduled
            assertEquals "wrong seconds", "9", se.seconds
            assertEquals "wrong minute", "5", se.minute
            assertEquals "wrong minute", "8", se.hour
            assertEquals "wrong minute", "11", se.month
            assertEquals "wrong minute", "?", se.dayOfWeek
            assertEquals "wrong minute", "0", se.dayOfMonth
            assertEquals "wrong minute", "2011", se.year

            //workflow
            assertNotNull "missing workflow", se.workflow
            assertNotNull "missing workflow", se.workflow.commands
            assertFalse "wrong workflow.keepgoing", se.workflow.keepgoing
            assertEquals "wrong workflow.strategy", "node-first", se.workflow.strategy
            assertEquals "wrong workflow size", 4, se.workflow.commands.size()
            se.workflow.commands.eachWithIndex { def entry, int i ->
                assertEquals "Wrong description i ${i}", "test${i + 1}".toString(), entry.description
            }
            assertEquals "wrong workflow item", "test script", se.workflow.commands[0].adhocRemoteString
            assertTrue "wrong workflow item", se.workflow.commands[0].adhocExecution
            assertEquals "wrong workflow item", "A Monkey returns", se.workflow.commands[1].adhocLocalString
            assertTrue "wrong workflow item", se.workflow.commands[1].adhocExecution

            //options
            assertNotNull "missing options", se.options
            assertEquals "wrong options size", 1, se.options.size()
            def opt1 = se.options.iterator().next()
            assertEquals "wrong option name", "opt1", opt1.name
            assertEquals "wrong option description", "an opt", opt1.description
            assertEquals "wrong option defaultValue", "xyz", opt1.defaultValue
            assertTrue "wrong option name", opt1.enforced
            assertTrue "wrong option name", opt1.required
            assertNotNull "wrong option values", opt1.optionValues
            assertEquals "wrong option values size", 2, opt1.optionValues.size()
            ArrayList valuesList = new ArrayList(opt1.optionValues)
            assertEquals "wrong option values[0]", 'a', valuesList[0]
            assertEquals "wrong option values[1]", 'b', valuesList[1]

        }

    void testDecodeBasic3() {
        def ymlstr2 = """
-
  project: zamp
  loglevel: ERR
  scheduleEnabled: false
  executionEnabled: false
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
    opt1:
      enforced: true
      required: true
      description: an opt
      value: xyz
      values:
      - a
      - b
    opt2:
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
        assertEquals "wrong name", "test job 1", se.jobName
        assertEquals "wrong description", "test descrip", se.description
        assertEquals "wrong groupPath", "group/1/2/3", se.groupPath
        assertEquals "wrong project", 'zamp', se.project
        assertEquals "wrong loglevel", "ERR", se.loglevel

        assertEquals "wrong scheduleEnabled", false, se.scheduleEnabled
        assertEquals "wrong executionEnabled", false, se.executionEnabled

        assertTrue "wrong doNodedispatch", se.doNodedispatch
        assertEquals "wrong nodeThreadcount", 3, se.nodeThreadcount
        assertFalse "wrong nodeKeepgoing", se.nodeKeepgoing
        assertFalse "wrong nodeExcludePrecedence", se.nodeExcludePrecedence
        assertEquals "wrong nodeInclude", null, se.nodeIncludeName
        assertEquals "wrong nodeExcludeName", null, se.nodeExclude
        assertEquals "wrong nodeExcludeName", null, se.nodeExcludeTags
        assertEquals "wrong nodeExcludeName", null, se.nodeExcludeOsFamily
        assertEquals "wrong nodeExcludeName", null, se.nodeExcludeOsArch
        assertEquals "wrong nodeExcludeName", null, se.nodeExcludeOsName
        assertEquals "wrong nodeExcludeName", null, se.nodeExcludeOsVersion

        assertEquals "wrong nodeInclude", "name: .* !tags: monkey !os-family: unix !os-name: Linux !os-version: 10.5.* !os-arch: x86 !hostname: shampoo.*", se.filter

        //schedule
        assertTrue "wrong scheduled", se.scheduled
        assertEquals "wrong seconds", "0", se.seconds
        assertEquals "wrong minute", "0,5,10,35", se.minute
        assertEquals "wrong minute", "8/2", se.hour
        assertEquals "wrong minute", "*", se.month
        assertEquals "wrong minute", "?", se.dayOfWeek
        assertEquals "wrong minute", "*", se.dayOfMonth
        assertEquals "wrong minute", "2001,2010,2012", se.year

        //workflow
        assertNotNull "missing workflow", se.workflow
        assertNotNull "missing workflow", se.workflow.commands
        assertTrue "wrong workflow.keepgoing", se.workflow.keepgoing
        assertEquals "wrong workflow.strategy", "step-first", se.workflow.strategy
        assertEquals "wrong workflow size", 5, se.workflow.commands.size()
        assertEquals "wrong workflow item", "test script", se.workflow.commands[0].adhocRemoteString
            //exec doesn't support arguments
        assertNull "wrong workflow item", se.workflow.commands[0].argString
        assertTrue "wrong workflow item", se.workflow.commands[0].adhocExecution
        assertEquals "wrong workflow item", "A Monkey returns", se.workflow.commands[1].adhocLocalString
        assertEquals "wrong workflow item", "whatever", se.workflow.commands[1].argString
        assertTrue "wrong workflow item", se.workflow.commands[1].adhocExecution
        assertEquals "wrong workflow item", "/path/to/file", se.workflow.commands[2].adhocFilepath
        assertEquals "wrong workflow item", "-whatever something -else", se.workflow.commands[2].argString
        assertTrue "wrong workflow item", se.workflow.commands[2].adhocExecution
        assertTrue "wrong exec type", se.workflow.commands[3] instanceof JobExec
        assertEquals "wrong workflow item", "some job", se.workflow.commands[3].jobName
        assertEquals "wrong workflow item", "another group", se.workflow.commands[3].jobGroup
        assertEquals "wrong workflow item", "yankee doodle", se.workflow.commands[3].argString
        assertEquals "wrong workflow item", true, se.workflow.commands[3].nodeStep
            assertTrue "wrong exec type", se.workflow.commands[4] instanceof CommandExec
            assertEquals "wrong workflow item", "http://example.com/path/to/file", se.workflow.commands[4].adhocFilepath
            assertEquals "wrong workflow item", "-blah bloo -blee", se.workflow.commands[4].argString
            assertTrue "wrong workflow item", se.workflow.commands[4].adhocExecution

        //options
        assertNotNull "missing options", se.options
        assertEquals "wrong options size", 2, se.options.size()
        final Iterator iterator = se.options.iterator()
        def opt1 = iterator.next()
        assertEquals "wrong option name", "opt1", opt1.name
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
        assertEquals "wrong option name", "opt2", opt2.name
        assertEquals "wrong option description", "whatever", opt2.description
        assertNull "wrong option defaultValue", opt2.defaultValue
        assertFalse "wrong option name", opt2.enforced != null && opt2.enforced
        assertFalse "wrong option name", opt2.required != null && opt2.required
        assertNull "wrong option values", opt2.optionValues
        assertNotNull "missing valuesUrl ", opt2.realValuesUrl
        assertEquals "missing valuesUrl ", "http://something.com", opt2.realValuesUrl.toExternalForm()
        assertEquals "wrong option regex", "\\d+", opt2.regex
        }



    void testDecodeLoglimit() {
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
    void testDecodeLoglimitCustomStatus() {
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
    void testDecodeTimeout() {
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
    void testDecodeBasicWithoutProject() {
            def ymlstr1 = """- id: null
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - exec: test script
    - script: A Monkey returns
  description: ''
  name: test job 1
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
            assertEquals "wrong description", "", se.description
            assertEquals "wrong groupPath", "my group", se.groupPath
            assertEquals "wrong project", null, se.project
            assertEquals "wrong loglevel", "INFO", se.loglevel
            assertTrue "wrong doNodedispatch", se.doNodedispatch
            assertEquals "wrong nodeThreadcount", 1, se.nodeThreadcount
            assertTrue "wrong nodeKeepgoing", se.nodeKeepgoing
            assertTrue "wrong nodeExcludePrecedence", se.nodeExcludePrecedence
            assertEquals "wrong nodeInclude", "hostname: testhost1 !name: x1", se.filter
            assertEquals "wrong nodeInclude", null, se.nodeInclude
            assertEquals "wrong nodeExcludeName", null, se.nodeExcludeName

            //schedule
            assertTrue "wrong scheduled", se.scheduled
            assertEquals "wrong seconds", "9", se.seconds
            assertEquals "wrong minute", "5", se.minute
            assertEquals "wrong minute", "8", se.hour
            assertEquals "wrong minute", "11", se.month
            assertEquals "wrong minute", "0", se.dayOfWeek
            assertEquals "wrong minute", "?", se.dayOfMonth
            assertEquals "wrong minute", "2011", se.year

            //workflow
            assertNotNull "missing workflow", se.workflow
            assertNotNull "missing workflow", se.workflow.commands
            assertFalse "wrong workflow.keepgoing", se.workflow.keepgoing
            assertEquals "wrong workflow.strategy", "node-first", se.workflow.strategy
            assertEquals "wrong workflow size", 2, se.workflow.commands.size()
            assertEquals "wrong workflow item", "test script", se.workflow.commands[0].adhocRemoteString
            assertTrue "wrong workflow item", se.workflow.commands[0].adhocExecution
            assertEquals "wrong workflow item", "A Monkey returns", se.workflow.commands[1].adhocLocalString
            assertTrue "wrong workflow item", se.workflow.commands[1].adhocExecution

            //options
            assertNotNull "missing options", se.options
            assertEquals "wrong options size", 1, se.options.size()
            def opt1 = se.options.iterator().next()
            assertEquals "wrong option name", "opt1", opt1.name
            assertEquals "wrong option description", "an opt", opt1.description
            assertEquals "wrong option defaultValue", "xyz", opt1.defaultValue
            assertTrue "wrong option name", opt1.enforced
            assertTrue "wrong option name", opt1.required
            assertNotNull "wrong option values", opt1.optionValues
            assertEquals "wrong option values size", 2, opt1.optionValues.size()
            ArrayList valuesList = new ArrayList(opt1.optionValues)
            assertEquals "wrong option values[0]", 'a', valuesList[0]
            assertEquals "wrong option values[1]", 'b', valuesList[1]

        }

        void testDecodeErrorHandlers(){
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
    - script: script string
      args: script args
      errorhandler:
        script: err script
        args: err script args
    - scriptfile: file path
      args: file args
      errorhandler:
        keepgoingOnSuccess: false
        scriptfile: err file
        args: err file args
    - jobref:
        name: job name
        group: job group
        args: job args
      errorhandler:
        keepgoingOnSuccess: true
        jobref:
          name: err job name
          group: err job group
          args: err job args
  description: ''
  name: test job 1
  group: my group
"""
        def list = JobsYAMLCodec.decode(ymlstr1)
        assertNotNull list
        assertEquals(1, list.size())
        def obj = list[0]
        assertTrue(obj instanceof ScheduledExecution)
        ScheduledExecution se = (ScheduledExecution) list[0]

        //workflow
        assertNotNull "missing workflow", se.workflow
        assertNotNull "missing workflow", se.workflow.commands
        assertEquals "wrong workflow size", 4, se.workflow.commands.size()

        def ndx=0
        assertEquals CommandExec.class, se.workflow.commands[ndx].class
        assertEquals "test script", se.workflow.commands[ndx].adhocRemoteString
        assertNull se.workflow.commands[ndx].adhocLocalString
        assertNull se.workflow.commands[ndx].adhocFilepath
        assertNull se.workflow.commands[ndx].argString

        assertNotNull se.workflow.commands[ndx].errorHandler
        assertEquals CommandExec.class,se.workflow.commands[ndx].errorHandler.class
        assertEquals "test err", se.workflow.commands[ndx].errorHandler.adhocRemoteString
        assertNull se.workflow.commands[ndx].errorHandler.adhocLocalString
        assertNull se.workflow.commands[ndx].errorHandler.adhocFilepath
        assertNull se.workflow.commands[ndx].errorHandler.argString
        assertFalse se.workflow.commands[ndx].errorHandler.keepgoingOnSuccess

        ndx++
        assertEquals CommandExec.class, se.workflow.commands[ndx].class
        assertEquals "script string", se.workflow.commands[ndx].adhocLocalString
        assertEquals "script args",se.workflow.commands[ndx].argString
        assertNull se.workflow.commands[ndx].adhocRemoteString
        assertNull se.workflow.commands[ndx].adhocFilepath

        assertNotNull se.workflow.commands[ndx].errorHandler
        assertEquals CommandExec.class, se.workflow.commands[ndx].errorHandler.class
        assertEquals "err script", se.workflow.commands[ndx].errorHandler.adhocLocalString
        assertEquals "err script args", se.workflow.commands[ndx].errorHandler.argString
        assertNull se.workflow.commands[ndx].errorHandler.adhocRemoteString
        assertNull se.workflow.commands[ndx].errorHandler.adhocFilepath
        assertFalse se.workflow.commands[ndx].errorHandler.keepgoingOnSuccess

        ndx++
        assertEquals CommandExec.class, se.workflow.commands[ndx].class
        assertEquals "file path", se.workflow.commands[ndx].adhocFilepath
        assertEquals "file args",se.workflow.commands[ndx].argString
        assertNull se.workflow.commands[ndx].adhocLocalString
        assertNull se.workflow.commands[ndx].adhocRemoteString

        assertNotNull se.workflow.commands[ndx].errorHandler
        assertEquals CommandExec.class, se.workflow.commands[ndx].errorHandler.class
        assertEquals "err file", se.workflow.commands[ndx].errorHandler.adhocFilepath
        assertEquals "err file args", se.workflow.commands[ndx].errorHandler.argString
        assertNull se.workflow.commands[ndx].errorHandler.adhocLocalString
        assertNull se.workflow.commands[ndx].errorHandler.adhocRemoteString
        assertFalse se.workflow.commands[ndx].errorHandler.keepgoingOnSuccess

        ndx++
        assertEquals JobExec.class, se.workflow.commands[ndx].class
        assertEquals "job name", se.workflow.commands[ndx].jobName
        assertEquals "job group", se.workflow.commands[ndx].jobGroup
        assertEquals "job args",se.workflow.commands[ndx].argString

        assertNotNull se.workflow.commands[ndx].errorHandler
        assertEquals JobExec.class, se.workflow.commands[ndx].errorHandler.class
        assertEquals "err job name", se.workflow.commands[ndx].errorHandler.jobName
        assertEquals "err job group", se.workflow.commands[ndx].errorHandler.jobGroup
        assertEquals "err job args", se.workflow.commands[ndx].errorHandler.argString
        assertTrue "err job keepgoing", se.workflow.commands[ndx].errorHandler.keepgoingOnSuccess


    }

    void testDecodeNotificationPlugin(){
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
    void testDecodeStepPlugin(){
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
    void testDecodeStepPluginEmptyConfig(){
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
    void testDecodeStepPluginNullConfig(){
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

    void testDecodeJobrefBasic() {
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
    void testDecodeJobrefNodeStep() {
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
    void testDecodeJobrefNodefilter() {
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
    void testDecodeJobrefNodefilter_threadcount() {
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
    void testDecodeJobrefNodefilter_keepgoing() {
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
    void testDecodeJobrefNodefilter_keepgoingFalse() {
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
    void testDecodeJobrefNodefilter_rankAttribute() {
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
    void testDecodeJobrefNodefilter_rankOrder() {
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
    void testDecodeJobrefNodefilter_rankOrderDescending() {
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
    void testShouldPassthruCrontabString() {
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
  
    void testNotificationThreshold() {
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

    void testEncodeThreadCountFromOption() {
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

    void testEncodeThreadCountFromValue() {
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
