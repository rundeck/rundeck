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

package rundeck.services

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.*
import spock.lang.Specification


@TestFor(MessagingService)
@Mock([Messaging, ScheduledExecution])
class MessagingServiceSpec extends Specification {

    private Map createJobParams(Map overrides = [:]) {
        [
                jobName       : 'blue',
                project       : 'AProject',
                groupPath     : 'some/where',
                description   : 'a job',
                argString     : '-a b -c d',
                workflow      : new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec([adhocRemoteString: 'test buddy'])]
                ),
                serverNodeUUID: null,
                scheduled     : true
        ] + overrides
    }

    def "generate server msg"(){
        given:
        def uuid = UUID.randomUUID().toString()

        when:
        def result = service.generateNodeMessage(uuid)

        then:
        result

    }

    def "generate job msg"(){
        given:
        def uuid = UUID.randomUUID().toString()
        def se = new ScheduledExecution(createJobParams(serverNodeUUID: uuid)).save()

        when:
        def result = service.generateJobMessage(se)

        then:
        result

    }


    def "get active nodes list"(){
        given:
        def uuid = UUID.randomUUID().toString()
        service.generateNodeMessage(UUID.randomUUID().toString())
        service.generateNodeMessage(UUID.randomUUID().toString())

        when:
        def result = service.getActiveNodes(uuid)

        then:
        result.size() == 3
    }


    def "get job messages list"(){
        given:
        def uuid = UUID.randomUUID().toString()
        def se = new ScheduledExecution(createJobParams(serverNodeUUID: uuid)).save()
        def seBeta = new ScheduledExecution(createJobParams(jobName: 'blue2', project: 'AProject2', serverNodeUUID: uuid)).save()
        def seCharlie = new ScheduledExecution(createJobParams(
                jobName: 'blue3', project: 'AProject2', serverNodeUUID: UUID.randomUUID().toString())).save()
        service.generateJobMessage(se)
        service.generateJobMessage(seBeta)
        service.generateJobMessage(seCharlie)

        when:
        def result = service.getJobMessages(uuid)

        then:
        result.size() == 2
    }
}
