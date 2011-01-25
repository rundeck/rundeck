import org.yaml.snakeyaml.Yaml

/*
* Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
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
*
*/

/*
* JobsYAMLCodecTests.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jan 24, 2011 10:36:06 AM
*
*/

public class JobsYAMLCodecTests extends GroovyTestCase {

    void testEncodeBasic() {
        def Yaml yaml = new Yaml()
        ScheduledExecution se = new ScheduledExecution([
            jobName: 'test job 1',
            description: 'test descrip',
            loglevel: 'INFO',
            project: 'test1',
            workflow: new Workflow([keepgoing: false, threadcount: 1, commands: [new CommandExec([adhocRemoteString: 'test script']),
                new CommandExec([adhocLocalString: 'test bash']),
                new CommandExec([adhocFilepath: 'some file path']),
                new JobExec([jobName: 'another job',jobGroup:'agroup']),
            ]]),
            options: [new Option(name: 'opt1', description: "an opt", defaultValue: "xyz", enforced: true, required: true, values: new TreeSet(["a", "b"]))] as TreeSet,
            nodeThreadcount: 1,
            nodeKeepgoing: true,
            doNodedispatch: true,
            nodeInclude: "testhost1",
            nodeExcludeName: "x1",
            scheduled:true,
            seconds:'*',
            minute:'0',
            hour: '2',
            month: '3',
            dayOfMonth:'?',
            dayOfWeek:'4',
            year:'2011'
        ])
        def jobs1 = [se]

        try {
            def ymlstr = JobsYAMLCodec.encode(jobs1)
            assertNotNull ymlstr
            assertTrue ymlstr instanceof String


            def doc = yaml.load(ymlstr)
            assertNotNull doc
            System.err.println("yaml: ${ymlstr}");
            System.err.println("doc: ${doc}");
            assertEquals "wrong number of jobs", 1, doc.size()
            assertEquals "wrong name", "test job 1", doc[0].name
            assertEquals "wrong description", "test descrip", doc[0].description
            assertEquals "wrong loglevel", "INFO", doc[0].loglevel
            assertEquals "incorrect context project", 'test1', doc[0].project
            assertNotNull "missing sequence", doc[0].sequence
            assertFalse "wrong wf keepgoing", doc[0].sequence.keepgoing
            assertEquals "wrong wf strategy", "node-first", doc[0].sequence.strategy
            assertNotNull "missing commands", doc[0].sequence.commands
            assertEquals "missing commands", 4,doc[0].sequence.commands.size()
            assertEquals "missing command exec", "test script", doc[0].sequence.commands[0].exec
            assertEquals "missing command script", "test bash", doc[0].sequence.commands[1].script
            assertEquals "missing command scriptfile", "some file path", doc[0].sequence.commands[2].scriptfile
            assertNotNull "missing command jobref", doc[0].sequence.commands[3].jobref
            assertEquals "missing command jobref.name", "another job", doc[0].sequence.commands[3].jobref.name
            assertEquals "missing command jobref.group", "agroup", doc[0].sequence.commands[3].jobref.group
            assertNotNull "missing options", doc[0].options
            assertNotNull "missing option opt1", doc[0].options.opt1
            assertEquals "missing option opt1", "an opt", doc[0].options.opt1.description
            assertEquals "missing option default", "xyz", doc[0].options.opt1.value
            assertTrue "missing option enforced", doc[0].options.opt1.enforced
            assertTrue "missing option required", doc[0].options.opt1.required
            assertNotNull "missing option values", doc[0].options.opt1.values
            assertEquals "wrong option values size", 2, doc[0].options.opt1.values.size()
            assertEquals "wrong option values[0]", "a", doc[0].options.opt1.values[0]
            assertEquals "wrong option values[1]", "b", doc[0].options.opt1.values[1]

            assertEquals "incorrect dispatch threadcount", 1, doc[0].nodefilters.dispatch.threadcount
            assertTrue "incorrect dispatch keepgoing", doc[0].nodefilters.dispatch.keepgoing
            assertTrue "incorrect dispatch excludePrecedence", doc[0].nodefilters.dispatch.excludePrecedence
            assertNotNull "missing nodefilters include", doc[0].nodefilters.include
            assertEquals "wrong nodefilters include hostname", "testhost1", doc[0].nodefilters.include.hostname
            assertEquals "missing nodefilters exclude name", "x1", doc[0].nodefilters.exclude.name

            assertNotNull "not scheduled",doc[0].schedule
            assertNotNull "not scheduled.time",doc[0].schedule.time
            assertEquals "not scheduled.time","*",doc[0].schedule.time.seconds
            assertEquals "not scheduled.time","0",doc[0].schedule.time.minute
            assertEquals "not scheduled.time","2",doc[0].schedule.time.hour
            assertEquals "not scheduled.time","3",doc[0].schedule.month
            assertEquals "not scheduled.time","4",doc[0].schedule.weekday.day
            assertEquals "not scheduled.time","2011",doc[0].schedule.year
        } catch (Exception e) {
            e.printStackTrace(System.err)
            fail "caught exception during encode or parse: " + e
        }

    }

    void testDecodeBasic() {
        def ymlstr1="""- id: null
  project: test1
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - exec: test script
    - script: A Monkey returns
  description: test descrip
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
        try{
            def list=JobsYAMLCodec.decode(ymlstr1)
            assertNotNull list
            assertEquals(1,list.size())
            def obj = list[0]
            assertTrue(obj instanceof ScheduledExecution)
            ScheduledExecution se = (ScheduledExecution) list[0]
            assertEquals "wrong name","test job 1",se.jobName
            assertEquals "wrong description","test descrip",se.description
            assertEquals "wrong groupPath","my group",se.groupPath
            assertEquals "wrong project","test1",se.project
            assertEquals "wrong loglevel","INFO",se.loglevel
            assertTrue "wrong doNodedispatch",se.doNodedispatch
            assertEquals "wrong nodeThreadcount",1,se.nodeThreadcount
            assertTrue "wrong nodeKeepgoing",se.nodeKeepgoing
            assertTrue "wrong nodeExcludePrecedence",se.nodeExcludePrecedence
            assertEquals "wrong nodeInclude","testhost1",se.nodeInclude
            assertEquals "wrong nodeExcludeName","x1",se.nodeExcludeName


            //schedule
            assertTrue "wrong scheduled",se.scheduled
            assertEquals "wrong seconds","9",se.seconds
            assertEquals "wrong minute","5",se.minute
            assertEquals "wrong minute","8",se.hour
            assertEquals "wrong minute","11",se.month
            assertEquals "wrong minute","0",se.dayOfWeek
            assertEquals "wrong minute","?",se.dayOfMonth
            assertEquals "wrong minute","2011",se.year

            //workflow
            assertNotNull "missing workflow",se.workflow
            assertNotNull "missing workflow",se.workflow.commands
            assertFalse "wrong workflow.keepgoing",se.workflow.keepgoing
            assertEquals "wrong workflow.strategy","node-first",se.workflow.strategy
            assertEquals "wrong workflow size",2,se.workflow.commands.size()
            assertEquals "wrong workflow item","test script",se.workflow.commands[0].adhocRemoteString
            assertEquals "wrong workflow item","A Monkey returns",se.workflow.commands[1].adhocLocalString

            //options
            assertNotNull "missing options",se.options
            assertEquals "wrong options size",1,se.options.size()
            def opt1=se.options.iterator().next()
            assertEquals "wrong option name","opt1",opt1.name
            assertEquals "wrong option description","an opt",opt1.description
            assertEquals "wrong option defaultValue","xyz",opt1.defaultValue
            assertTrue "wrong option name",opt1.enforced
            assertTrue "wrong option name",opt1.required
            assertNotNull "wrong option values",opt1.values
            assertEquals "wrong option values size",2,opt1.values.size()
            ArrayList valuesList = new ArrayList(opt1.values)
            assertEquals "wrong option values[0]",'a',valuesList[0]
            assertEquals "wrong option values[1]",'b',valuesList[1]

        } catch (Exception e) {
            e.printStackTrace(System.err)
            fail "caught exception during decode: " + e
        }

        def ymlstr2="""
-
  project: zamp
  loglevel: ERR
  sequence:
    keepgoing: true
    strategy: step-first
    commands:
    - exec: test script
      args: this is redic
    - script: A Monkey returns
      args: whatever
    - scriptfile: /path/to/file
      args: -whatever something -else
    - jobref:
        name: some job
        group: another group
        args: yankee doodle
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
               def list
               try{
                   list=JobsYAMLCodec.decode(ymlstr2)

               } catch (Exception e) {
                   e.printStackTrace(System.err)
                   fail "caught exception during decode: " + e
               }
               assertNotNull list
               assertEquals(1,list.size())
               def obj = list[0]
               assertTrue(obj instanceof ScheduledExecution)
               ScheduledExecution se = (ScheduledExecution) list[0]
               assertEquals "wrong name","test job 1",se.jobName
               assertEquals "wrong description","test descrip",se.description
               assertEquals "wrong groupPath","group/1/2/3",se.groupPath
               assertEquals "wrong project","zamp",se.project
               assertEquals "wrong loglevel","ERR",se.loglevel
               assertTrue "wrong doNodedispatch",se.doNodedispatch
               assertEquals "wrong nodeThreadcount",3,se.nodeThreadcount
               assertFalse "wrong nodeKeepgoing",se.nodeKeepgoing
               assertFalse "wrong nodeExcludePrecedence",se.nodeExcludePrecedence
               assertEquals "wrong nodeInclude",".*",se.nodeIncludeName
               assertEquals "wrong nodeExcludeName","shampoo.*",se.nodeExclude
               assertEquals "wrong nodeExcludeName","monkey",se.nodeExcludeTags
               assertEquals "wrong nodeExcludeName","unix",se.nodeExcludeOsFamily
               assertEquals "wrong nodeExcludeName","x86",se.nodeExcludeOsArch
               assertEquals "wrong nodeExcludeName","Linux",se.nodeExcludeOsName
               assertEquals "wrong nodeExcludeName","10.5.*",se.nodeExcludeOsVersion


               //schedule
               assertTrue "wrong scheduled",se.scheduled
               assertEquals "wrong seconds","0",se.seconds
               assertEquals "wrong minute","0,5,10,35",se.minute
               assertEquals "wrong minute","8/2",se.hour
               assertEquals "wrong minute","*",se.month
               assertEquals "wrong minute","?",se.dayOfWeek
               assertEquals "wrong minute","*",se.dayOfMonth
               assertEquals "wrong minute","2001,2010,2012",se.year

               //workflow
               assertNotNull "missing workflow",se.workflow
               assertNotNull "missing workflow",se.workflow.commands
               assertTrue "wrong workflow.keepgoing",se.workflow.keepgoing
               assertEquals "wrong workflow.strategy","step-first",se.workflow.strategy
               assertEquals "wrong workflow size",4,se.workflow.commands.size()
               assertEquals "wrong workflow item","test script",se.workflow.commands[0].adhocRemoteString
               assertEquals "wrong workflow item","this is redic",se.workflow.commands[0].argString
               assertEquals "wrong workflow item","A Monkey returns",se.workflow.commands[1].adhocLocalString
               assertEquals "wrong workflow item","whatever",se.workflow.commands[1].argString
               assertEquals "wrong workflow item","/path/to/file",se.workflow.commands[2].adhocFilepath
               assertEquals "wrong workflow item","-whatever something -else",se.workflow.commands[2].argString
               assertTrue "wrong exec type",se.workflow.commands[3] instanceof JobExec
               assertEquals "wrong workflow item","some job",se.workflow.commands[3].jobName
               assertEquals "wrong workflow item","another group",se.workflow.commands[3].jobGroup
               assertEquals "wrong workflow item","yankee doodle",se.workflow.commands[3].argString

               //options
               assertNotNull "missing options",se.options
               assertEquals "wrong options size",2,se.options.size()
               final Iterator iterator = se.options.iterator()
               def opt1=iterator.next()
               assertEquals "wrong option name","opt1",opt1.name
               assertEquals "wrong option description","an opt",opt1.description
               assertEquals "wrong option defaultValue","xyz",opt1.defaultValue
               assertTrue "wrong option name",opt1.enforced
               assertTrue "wrong option name",opt1.required
               assertNotNull "wrong option values",opt1.values
               assertEquals "wrong option values size",2,opt1.values.size()
               ArrayList valuesList = new ArrayList(opt1.values)
               assertEquals "wrong option values[0]",'a',valuesList[0]
               assertEquals "wrong option values[1]",'b',valuesList[1]
               def opt2=iterator.next()
               assertEquals "wrong option name","opt2",opt2.name
               assertEquals "wrong option description","whatever",opt2.description
               assertNull "wrong option defaultValue",opt2.defaultValue
               assertFalse "wrong option name",opt2.enforced!=null && opt2.enforced
               assertFalse "wrong option name",opt2.required!=null && opt2.required
               assertNull "wrong option values",opt2.values
               assertNotNull "missing valuesUrl ",opt2.valuesUrl
               assertEquals "missing valuesUrl ","http://something.com",opt2.valuesUrl.toExternalForm()
               assertEquals "wrong option regex","\\d+",opt2.regex

    }


}