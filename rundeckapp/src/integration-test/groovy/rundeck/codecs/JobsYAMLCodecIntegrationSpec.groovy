/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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

package rundeck.codecs

import grails.testing.mixin.integration.Integration
import org.yaml.snakeyaml.Yaml
import rundeck.CommandExec
import rundeck.JobExec
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Specification


@Integration
class JobsYAMLCodecIntegrationSpec extends Specification {

    def "binding test"() {
        given:
            def x = new CommandExec(adhocLocalString: '#!/bin/bash\n\necho test bash\n\necho tralaala \'something\'\n')
        expect:
            x.adhocLocalString == '#!/bin/bash\n\necho test bash\n\necho tralaala \'something\'\n'
    }

    def "basic encode test"() {
        given:
            def Yaml yaml = new Yaml()
            ScheduledExecution se = new ScheduledExecution(
                [
                    jobName        : 'test job 1',
                    description    : 'test descrip',
                    loglevel       : 'INFO',
                    project        : 'test1',
                    workflow       : new Workflow(
                        [keepgoing: false, threadcount: 1, commands: [new CommandExec(
                            [adhocRemoteString: 'test ' +
                                                'script',
                             description      : 'test1']
                        ),
                                                                      new CommandExec(
                                                                          [adhocLocalString:
                                                                               "#!/bin/bash\n\necho test " +
                                                                               "bash\n\necho tralaala " +
                                                                               "'something'\n",
                                                                           description     : 'test2']
                                                                      ),
                                                                      new CommandExec(
                                                                          [adhocFilepath: 'some file ' +
                                                                                          'path',
                                                                           description  : 'test3']
                                                                      ),
                                                                      new JobExec(
                                                                          [jobName               : 'another job',
                                                                           jobGroup              :
                                                                               'agroup', nodeStep: true, description:
                                                                               'test4']
                                                                      ),
                                                                      new CommandExec(
                                                                          [adhocFilepath: 'http://example' +
                                                                                          '.com/blah',
                                                                           description  : 'test5']
                                                                      ),
                        ]]
                    ),
                    options        : [new Option(
                        name: 'opt1',
                        description: "an opt",
                        defaultValue: "xyz",
                        enforced: true,
                        required: true,
                        valuesList: 'a,b'
                    )] as TreeSet,
                    nodeThreadcount: 1,
                    nodeKeepgoing  : true,
                    doNodedispatch : true,
                    nodeInclude    : "testhost1",
                    nodeExcludeName: "x1",
                    scheduled      : true,
                    seconds        : '*',
                    minute         : '0',
                    hour           : '2,15',
                    month          : '3',
                    dayOfMonth     : '?',
                    dayOfWeek      : '4',
                    year           : '2011',
                    uuid           : UUID.randomUUID().toString()
                ]
            )
            def jobs1 = [se]
        when:

            def ymlstr = JobsYAMLCodec.encode(jobs1)
        then:
            se.workflow.commands[1].adhocLocalString ==
            '#!/bin/bash\n\necho test bash\n\necho tralaala \'something\'\n'

            ymlstr != null
            ymlstr instanceof String

            ymlstr.contains("hour: '2,15'") //ensure that upgrade doesn't break the formatting of this scalar value
            def doc = yaml.load(ymlstr)
            doc != null
            doc.size() == 1
            doc[0].name == "test job 1"
            doc[0].uuid == jobs1[0].uuid
            doc[0].description == "test descrip"
            doc[0].loglevel == "INFO"
            doc[0].scheduleEnabled == true
            doc[0].executionEnabled == true
            doc[0].project == null
            doc[0].sequence != null
            !doc[0].sequence.keepgoing
            doc[0].sequence.strategy == "node-first"
            doc[0].sequence.commands != null
            doc[0].sequence.commands.size() == 5
            doc[0].sequence.commands.eachWithIndex { cmd, i ->
                cmd.description == "test${i + 1}".toString()
            }
            doc[0].sequence.commands[0].exec == "test script"
            doc[0].sequence.commands[1].script?.toString() ==
            "#!/bin/bash\n\necho test bash\n\necho tralaala 'something'\n"
            doc[0].sequence.commands[2].scriptfile == "some file path"
            doc[0].sequence.commands[3].jobref != null
            doc[0].sequence.commands[3].jobref.name == "another job"
            doc[0].sequence.commands[3].jobref.group == "agroup"
            doc[0].sequence.commands[3].jobref.nodeStep == 'true'

            doc[0].sequence.commands[4].scripturl == "http://example.com/blah"
            doc[0].options != null
            doc[0].options instanceof Collection
            doc[0].options[0] != null
            doc[0].options[0].name == "opt1"
            doc[0].options[0].description == "an opt"
            doc[0].options[0].value == "xyz"
            doc[0].options[0].enforced
            doc[0].options[0].required
            doc[0].options[0].values != null
            doc[0].options[0].values.size() == 2
            doc[0].options[0].values[0] == "a"
            doc[0].options[0].values[1] == "b"

            doc[0].nodefilters.dispatch.threadcount == "1"
            doc[0].nodefilters.dispatch.keepgoing
            doc[0].nodefilters.dispatch.excludePrecedence
            doc[0].nodefilters.filter != null
            doc[0].nodefilters.filter == "hostname: testhost1 !name: x1"
            doc[0].nodefilters.include == null
            doc[0].nodefilters.exclude == null

            doc[0].schedule != null
            doc[0].schedule.time != null
            doc[0].schedule.time.seconds == "*"
            doc[0].schedule.time.minute == "0"
            doc[0].schedule.time.hour == "2,15"
            doc[0].schedule.month == "3"
            doc[0].schedule.weekday.day == "4"
            doc[0].schedule.year == "2011"

    }

    def "basic encode strip uuid"() {
        given:
            def Yaml yaml = new Yaml()
            ScheduledExecution se = new ScheduledExecution(
                [
                    jobName        : 'test job 1',
                    description    : 'test descrip',
                    loglevel       : 'INFO',
                    project        : 'test1',
                    workflow       : new Workflow(
                        [keepgoing: false, threadcount: 1, commands: [new CommandExec(
                            [adhocRemoteString: 'test script',
                             description      : 'test1']
                        ),
                                                                      new CommandExec(
                                                                          [adhocLocalString                :
                                                                               "#!/bin/bash\n\necho test " +
                                                                               "bash\n\necho tralaala " +
                                                                               "'something'\n", description:
                                                                               'test2']
                                                                      ),
                                                                      new CommandExec(
                                                                          [adhocFilepath: 'some file path',
                                                                           description  : 'test3']
                                                                      ),
                                                                      new JobExec(
                                                                          [jobName               : 'another job',
                                                                           jobGroup              :
                                                                               'agroup', nodeStep: true, description:
                                                                               'test4']
                                                                      ),
                                                                      new CommandExec(
                                                                          [adhocFilepath: 'http://example' +
                                                                                          '.com/blah',
                                                                           description  : 'test5']
                                                                      ),
                        ]]
                    ),
                    options        : [new Option(
                        name: 'opt1',
                        description: "an opt",
                        defaultValue: "xyz",
                        enforced: true,
                        required: true,
                        valuesList: 'a,b'
                    )] as TreeSet,
                    nodeThreadcount: 1,
                    nodeKeepgoing  : true,
                    doNodedispatch : true,
                    nodeInclude    : "testhost1",
                    nodeExcludeName: "x1",
                    scheduled      : true,
                    seconds        : '*',
                    minute         : '0',
                    hour           : '2,15',
                    month          : '3',
                    dayOfMonth     : '?',
                    dayOfWeek      : '4',
                    year           : '2011',
                    uuid           : UUID.randomUUID().toString()
                ]
            )
            def jobs1 = [se]
        when:
            def ymlstr = JobsYAMLCodec.encodeStripUuid(jobs1)
        then:
            ymlstr != null
            ymlstr instanceof String


            def doc = yaml.load(ymlstr)
            doc != null
            doc.size() == 1
            doc[0].name == "test job 1"
            doc[0].uuid == null
            doc[0].id == null
            doc[0].description == "test descrip"
            doc[0].loglevel == "INFO"
            doc[0].scheduleEnabled == true
            doc[0].executionEnabled == true
            doc[0].project == null
            doc[0].sequence != null
            !doc[0].sequence.keepgoing
            doc[0].sequence.strategy == "node-first"
            doc[0].sequence.commands != null
            doc[0].sequence.commands.size() == 5
            doc[0].sequence.commands.eachWithIndex { cmd, i ->
                cmd.description == "test${i + 1}".toString()
            }
            doc[0].sequence.commands[0].exec == "test script"
            doc[0].sequence.commands[1].script == "#!/bin/bash\n\necho test bash\n\necho tralaala 'something'\n"
            doc[0].sequence.commands[2].scriptfile == "some file path"
            doc[0].sequence.commands[3].jobref != null
            doc[0].sequence.commands[3].jobref.name == "another job"
            doc[0].sequence.commands[3].jobref.group == "agroup"
            doc[0].sequence.commands[3].jobref.nodeStep == 'true'

            doc[0].sequence.commands[4].scripturl == "http://example.com/blah"
            doc[0].options != null
            doc[0].options instanceof Collection
            doc[0].options[0] != null
            doc[0].options[0].name == "opt1"
            doc[0].options[0].description == "an opt"
            doc[0].options[0].value == "xyz"
            doc[0].options[0].enforced
            doc[0].options[0].required
            doc[0].options[0].values != null
            doc[0].options[0].values.size() == 2
            doc[0].options[0].values[0] == "a"
            doc[0].options[0].values[1] == "b"

            doc[0].nodefilters.dispatch.threadcount == "1"
            doc[0].nodefilters.dispatch.keepgoing
            doc[0].nodefilters.dispatch.excludePrecedence
            doc[0].nodefilters.filter != null
            doc[0].nodefilters.filter == "hostname: testhost1 !name: x1"
            doc[0].nodefilters.include == null
            doc[0].nodefilters.exclude == null

            doc[0].schedule != null
            doc[0].schedule.time != null
            doc[0].schedule.time.seconds == "*"
            doc[0].schedule.time.minute == "0"
            doc[0].schedule.time.hour == "2,15"
            doc[0].schedule.month == "3"
            doc[0].schedule.weekday.day == "4"
            doc[0].schedule.year == "2011"

    }

    def "encode error handlers"() {
        given:

            def Yaml yaml = new Yaml()
            def errhandlers = [
                new CommandExec([adhocRemoteString: 'err exec']),
                new CommandExec([adhocLocalString: "err script", argString: 'err script args', keepgoingOnSuccess:
                    false]),
                new CommandExec([adhocFilepath: 'err file path', argString: 'err file args', keepgoingOnSuccess: true]),
                new JobExec(
                    [jobName: 'err job', jobGroup: 'err group', argString: 'err job args', keepgoingOnSuccess:
                        true]
                )
            ]
            ScheduledExecution se = new ScheduledExecution(
                [
                    jobName        : 'test job 1',
                    description    : 'test descrip',
                    loglevel       : 'INFO',
                    project        : 'test1',
                    workflow       : new Workflow(
                        [keepgoing: false, threadcount: 1, commands: [
                            new CommandExec([adhocRemoteString: 'test script', errorHandler: errhandlers[0]]),
                            new CommandExec(
                                [adhocLocalString: "#!/bin/bash\n\necho test bash\n\necho tralaala 'something'\n",
                                 errorHandler    :
                                     errhandlers[1]]
                            ),
                            new CommandExec([adhocFilepath: 'some file path', errorHandler: errhandlers[2]]),
                            new JobExec([jobName: 'another job', jobGroup: 'agroup', errorHandler: errhandlers[3]]),
                        ]]
                    ),
                    options        : [new Option(
                        name: 'opt1',
                        description: "an opt",
                        defaultValue: "xyz",
                        enforced: true,
                        required: true,
                        valuesList: 'a,b'
                    )] as TreeSet,
                    nodeThreadcount: 1,
                    nodeKeepgoing  : true,
                    doNodedispatch : true,
                    nodeInclude    : "testhost1",
                    nodeExcludeName: "x1",
                    scheduled      : true,
                    seconds        : '*',
                    minute         : '0',
                    hour           : '2',
                    month          : '3',
                    dayOfMonth     : '?',
                    dayOfWeek      : '4',
                    year           : '2011'
                ]
            )
            def jobs1 = [se]
        when:
            def ymlstr = JobsYAMLCodec.encode(jobs1)
        then:
            ymlstr != null
            ymlstr instanceof String


            def doc = yaml.load(ymlstr)
            doc != null
            doc.size() == 1
            doc[0].sequence != null
            !doc[0].sequence.keepgoing
            doc[0].sequence.strategy == "node-first"
            doc[0].sequence.commands != null
            doc[0].sequence.commands.size() == 4
            doc[0].sequence.commands[0].exec == "test script"
            doc[0].sequence.commands[1].script == "#!/bin/bash\n\necho test bash\n\necho tralaala 'something'\n"
            doc[0].sequence.commands[2].scriptfile == "some file path"
            doc[0].sequence.commands[3].jobref != null
            doc[0].sequence.commands[3].jobref.name == "another job"
            doc[0].sequence.commands[3].jobref.group == "agroup"

            //test handlers
            doc[0].sequence.commands[0].errorhandler != null
            doc[0].sequence.commands[0].errorhandler == [enabled:true, exec: 'err exec']

            doc[0].sequence.commands[1].errorhandler != null
            doc[0].sequence.commands[1].errorhandler == [script: 'err script', enabled:true, args: 'err script args']

            doc[0].sequence.commands[2].errorhandler != null
            doc[0].
                sequence.
                commands[2].
                errorhandler == [scriptfile: 'err file path', enabled:true, args: 'err file args', keepgoingOnSuccess: true]

            doc[0].sequence.commands[3].errorhandler != null
            doc[0].
                sequence.
                commands[3].
                errorhandler == [enabled:true, jobref: [name: 'err job', group: 'err group', args: 'err job args'], keepgoingOnSuccess: true]

    }

}
