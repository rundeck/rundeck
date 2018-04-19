/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

import org.yaml.snakeyaml.Yaml
import rundeck.CommandExec
import rundeck.JobExec
import rundeck.Notification
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Specification

/**
 * @author greg
 * @since 5/4/17
 */
class JobsYAMLCodecSpec extends Specification {
    def "canonical notifications"() {
        given:
        def Yaml yaml = new Yaml()
        ScheduledExecution se = new ScheduledExecution(
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
        def jobs1 = [se]
        when:

        def ymlstr = JobsYAMLCodec.encode(jobs1)
        then:
        ymlstr != null
        ymlstr instanceof String

        def doc = yaml.load(ymlstr)

        doc.size() == 1
        doc[0].name == 'test job 1'
        doc[0].notification.onsuccess.size() == 1
        doc[0].notification.onsuccess.plugin.size() == 3
        def expectorder = ['aplugin', 'bplugin', 'zplugin']
        def conforder = ['a', 'c', 'z']
        doc[0].notification.onsuccess.plugin.collect { it.type } == expectorder
        (0..2).each { i ->
            doc[0].notification.onsuccess.plugin[i].type == expectorder[i]
            doc[0].notification.onsuccess.plugin[i].configuration.keySet().toList() == conforder
        }

        where:
        order                             | _
        ['aplugin', 'bplugin', 'zplugin'] | _
        ['aplugin', 'zplugin', 'bplugin'] | _
        ['zplugin', 'aplugin', 'bplugin'] | _

    }
}
