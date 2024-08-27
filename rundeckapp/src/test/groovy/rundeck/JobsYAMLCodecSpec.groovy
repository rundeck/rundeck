package rundeck

import rundeck.codecs.JobsYAMLCodec
import spock.lang.Specification

class JobsYAMLCodecSpec extends Specification {


    def "testDecodeBasic1"() {
        given:
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
        when:
            def list = JobsYAMLCodec.decode(ymlstr1)
        then:
            list
            list.size() == 1
            def obj = list[0]
            obj instanceof ScheduledExecution
            ScheduledExecution se = (ScheduledExecution) list[0]
            se.jobName == "test job 1"
            se.description == ""
            se.groupPath == "my group"
            se.project == 'test1'
            se.loglevel == "INFO"

            se.scheduleEnabled == true
            se.executionEnabled == true

            se.doNodedispatch
            se.nodeThreadcount == 1
            se.nodeKeepgoing
            se.nodeExcludePrecedence
            se.nodeInclude == null
            se.nodeExcludeName == null
            se.filter == "hostname: testhost1 !name: x1"

            //schedule
            se.scheduled
            se.seconds == "9"
            se.minute == "5"
            se.hour == "8"
            se.month == "11"
            se.dayOfWeek == "0"
            se.dayOfMonth == "?"
            se.year == "2011"

            //workflow
            se.workflow
            se.workflow.commands
            !se.workflow.keepgoing
            se.workflow.strategy == "node-first"
            se.workflow.commands.size() == 4
            se.workflow.commands.eachWithIndex { def entry, int i ->
                entry.description == "test${i + 1}".toString()
            }
            se.workflow.commands[0].configuration.adhocRemoteString == "test script"
            se.workflow.commands[1].configuration.adhocLocalString == "A Monkey returns"

            //options
            se.options != null
            se.options.size() == 1
            def opt1 = se.options.iterator().next()
            opt1.name == "opt1"
            opt1.description == "an opt"
            opt1.defaultValue == "xyz"
            opt1.enforced
            opt1.required
            opt1.optionValues != null
            opt1.optionValues.size() == 2
            ArrayList valuesList = new ArrayList(opt1.optionValues)
            valuesList[0] == 'a'
            valuesList[1] == 'b'

    }

    def "testDecodeBasic2"() {
        given:
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
        when:
            def list = JobsYAMLCodec.decode(ymlstr1)
        then:
            list
            list.size() == 1
            def obj = list[0]
            obj instanceof ScheduledExecution
            ScheduledExecution se = (ScheduledExecution) list[0]
            se.jobName == "test job 1"
            se.description == ""
            se.groupPath == "my group"
            se.project == 'test1'
            se.loglevel == "INFO"

            se.scheduleEnabled == true
            se.executionEnabled == true

            se.doNodedispatch
            se.nodeThreadcount == 1
            se.nodeKeepgoing
            se.nodeExcludePrecedence
            se.nodeInclude == null
            se.nodeExcludeName == null
            se.filter == "hostname: testhost1 !name: x1"

            //schedule
            se.scheduled
            se.seconds == "9"
            se.minute == "5"
            se.hour == "8"
            se.month == "11"
            se.dayOfWeek == "?"
            se.dayOfMonth == "0"
            se.year == "2011"

            //workflow
            se.workflow != null
            se.workflow.commands != null
            !se.workflow.keepgoing
            se.workflow.strategy == "node-first"
            se.workflow.commands.size() == 4
            se.workflow.commands.eachWithIndex { def entry, int i ->
                entry.description == "test${i + 1}".toString()
            }
            se.workflow.commands[0].configuration.adhocRemoteString == "test script"

            se.workflow.commands[1].configuration.adhocLocalString == "A Monkey returns"

            //options
            se.options != null
            se.options.size() == 1
            def opt1 = se.options.iterator().next()
            opt1.name == "opt1"
            opt1.description == "an opt"
            opt1.defaultValue == "xyz"
            opt1.enforced
            opt1.required
            opt1.optionValues != null
            opt1.optionValues.size() == 2
            ArrayList valuesList = new ArrayList(opt1.optionValues)
            valuesList[0] == 'a'
            valuesList[1] == 'b'

    }

    def "testDecodeBasic3"() {
        given:
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
        when:
            def list = JobsYAMLCodec.decode(ymlstr2)
        then:
            list
            list.size() == 1
            def obj = list[0]
            obj instanceof ScheduledExecution
            ScheduledExecution se = (ScheduledExecution) list[0]
            se.jobName == "test job 1"
            se.description == "test descrip"
            se.groupPath == "group/1/2/3"
            se.project == 'zamp'
            se.loglevel == "ERR"

            se.scheduleEnabled == false
            se.executionEnabled == false

            se.doNodedispatch
            se.nodeThreadcount == 3
            !se.nodeKeepgoing
            !se.nodeExcludePrecedence
            se.nodeIncludeName == null
            se.nodeExclude == null
            se.nodeExcludeTags == null
            se.nodeExcludeOsFamily == null
            se.nodeExcludeOsArch == null
            se.nodeExcludeOsName == null
            se.nodeExcludeOsVersion == null


            se.filter ==
            "name: .* !tags: monkey !os-family: unix !os-name: Linux !os-version: 10.5.* !os-arch: x86 !hostname: " +
            "shampoo.*"


            //schedule
            se.scheduled
            se.seconds == "0"
            se.minute == "0,5,10,35"
            se.hour == "8/2"
            se.month == "*"
            se.dayOfWeek == "?"
            se.dayOfMonth == "*"
            se.year == "2001,2010,2012"

            //workflow
            se.workflow != null
            se.workflow.commands != null
            se.workflow.keepgoing
            se.workflow.strategy == "step-first"
            se.workflow.commands.size() == 5
            se.workflow.commands[0].configuration.adhocRemoteString == "test script"
            //exec doesn't support arguments
            se.workflow.commands[0].configuration.argString == null

            se.workflow.commands[1].configuration.adhocLocalString == "A Monkey returns"
            se.workflow.commands[1].configuration.argString == "whatever"

            se.workflow.commands[2].configuration.adhocFilepath == "/path/to/file"

            se.workflow.commands[2].configuration.argString == "-whatever something -else"

            se.workflow.commands[3] instanceof JobExec
            se.workflow.commands[3].jobName == "some job"
            se.workflow.commands[3].jobGroup == "another group"
            se.workflow.commands[3].argString == "yankee doodle"
            se.workflow.commands[3].nodeStep == true
            se.workflow.commands[4] instanceof PluginStep
            se.workflow.commands[4].configuration.adhocFilepath == "http://example.com/path/to/file"
            se.workflow.commands[4].configuration.argString == "-blah bloo -blee"


            //options
            se.options != null
            se.options.size() == 2
            final Iterator iterator = se.options.iterator()
            def opt1 = iterator.next()
            opt1.name == "opt1"
            opt1.description == "an opt"
            opt1.defaultValue == "xyz"
            opt1.enforced
            opt1.required
            opt1.optionValues != null
            opt1.optionValues.size() == 2
            ArrayList valuesList = new ArrayList(opt1.optionValues)
            valuesList[0] == 'a'
            valuesList[1] == 'b'
            def opt2 = iterator.next()
            opt2.name == "opt2"
            opt2.description == "whatever"
            opt2.defaultValue==null
            opt2.enforced==false
            opt2.required==false
            opt2.optionValues==null
            opt2.realValuesUrl != null
            opt2.realValuesUrl.toExternalForm() == "http://something.com"
            opt2.regex == "\\d+"
    }

    def "testDecodeBasicWithoutProject"() {
        given:
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
        when:
            def list = JobsYAMLCodec.decode(ymlstr1)
        then:
            list
            list.size() == 1
            def obj = list[0]
            obj instanceof ScheduledExecution
            ScheduledExecution se = (ScheduledExecution) list[0]
            se.jobName == "test job 1"
            se.description == ""
            se.groupPath == "my group"
            se.project == null
            se.loglevel == "INFO"
            se.doNodedispatch
            se.nodeThreadcount == 1
            se.nodeKeepgoing
            se.nodeExcludePrecedence
            se.filter == "hostname: testhost1 !name: x1"
            se.nodeInclude == null
            se.nodeExcludeName == null

            //schedule
            se.scheduled
            se.seconds == "9"
            se.minute == "5"
            se.hour == "8"
            se.month == "11"
            se.dayOfWeek == "0"
            se.dayOfMonth == "?"
            se.year == "2011"

            //workflow
            se.workflow != null
            se.workflow.commands != null
            !se.workflow.keepgoing
            se.workflow.strategy == "node-first"
            se.workflow.commands.size() == 2

            se.workflow.commands[0].configuration == [adhocExecution:true, adhocRemoteString: 'test script']
            se.workflow.commands[1].configuration == [adhocExecution:true, adhocLocalString: 'A Monkey returns']

            //options
            se.options != null
            se.options.size() == 1
            def opt1 = se.options.iterator().next()
            opt1.name == "opt1"
            opt1.description == "an opt"
            opt1.defaultValue == "xyz"
            opt1.enforced
            opt1.required
            opt1.optionValues != null
            opt1.optionValues.size() == 2
            ArrayList valuesList = new ArrayList(opt1.optionValues)
            valuesList[0] == 'a'
            valuesList[1] == 'b'

    }

    def "testDecodeErrorHandlers"() {
        given:
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
        when:
            def list = JobsYAMLCodec.decode(ymlstr1)
        then:
            list
            list.size() == 1
            def obj = list[0]
            obj instanceof ScheduledExecution
            ScheduledExecution se = (ScheduledExecution) list[0]

            //workflow
            se.workflow != null
            se.workflow.commands != null
            se.workflow.commands.size() == 4


            def cmd0 = se.workflow.commands[0]
            cmd0.class == PluginStep.class
            cmd0.configuration == [adhocExecution:true, adhocRemoteString: 'test script']

            cmd0.errorHandler
            cmd0.errorHandler.class == PluginStep.class
            cmd0.errorHandler.configuration == [adhocExecution:true, adhocRemoteString: 'test err']
            !cmd0.errorHandler.keepgoingOnSuccess


            def cmd1 = se.workflow.commands[1]
            cmd1.class == PluginStep.class
            cmd1.configuration == [adhocExecution:true, adhocLocalString: 'script string', argString: 'script args']

            cmd1.errorHandler
            cmd1.errorHandler.class == PluginStep.class
            cmd1.errorHandler.configuration == [adhocExecution:true, adhocLocalString: 'err script', argString: 'err script args']
            !cmd1.errorHandler.keepgoingOnSuccess


            def cmd2 = se.workflow.commands[2]
            cmd2.class == PluginStep.class
            cmd2.configuration == [adhocExecution:true, adhocFilepath: 'file path', expandTokenInScriptFile:false, argString: 'file args']

            cmd2.errorHandler
            cmd2.errorHandler.class == PluginStep.class
            cmd2.errorHandler.configuration == [adhocExecution:true, adhocFilepath: 'err file', expandTokenInScriptFile:false, argString: 'err file args']
            !cmd2.errorHandler.keepgoingOnSuccess


            def cmd3 = se.workflow.commands[3]
            cmd3.class == JobExec.class
            cmd3.jobName == "job name"
            cmd3.jobGroup == "job group"
            cmd3.argString == "job args"

            cmd3.errorHandler
            cmd3.errorHandler.class == JobExec.class
            cmd3.errorHandler.jobName == "err job name"
            cmd3.errorHandler.jobGroup == "err job group"
            cmd3.errorHandler.argString == "err job args"
            cmd3.errorHandler.keepgoingOnSuccess
    }
}