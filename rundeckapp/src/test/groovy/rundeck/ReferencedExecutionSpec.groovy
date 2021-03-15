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

package rundeck

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import grails.test.hibernate.HibernateSpec
import org.rundeck.app.authorization.AppAuthContextProcessor
import rundeck.services.FrameworkService
import rundeck.services.NotificationService
import rundeck.services.OrchestratorPluginService
import rundeck.services.PluginService
import rundeck.services.ScheduledExecutionService
import rundeck.services.feature.FeatureService

import static org.junit.Assert.*

/**
 * Created by greg on 10/21/15.
 */
class ReferencedExecutionSpec extends HibernateSpec
{
    List<Class> getDomainClasses() { [ScheduledExecution, Workflow, CommandExec, ReferencedExecution]}
    def "execution id list"(){
        given:
        def refTotal = 10
        def se = new ScheduledExecution(
                uuid: "000000",
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                refExecCount: refTotal,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def seb = new ScheduledExecution(
                jobName: 'test2',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                refExecCount: refTotal,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def exec = new Execution(
                user: "testuser",
                project: "project1",
                loglevel: 'WARN',
                status: 'FAILED',
                doNodedispatch: true,
                filter:'name: nodea',
                succeededNodeList:'fwnode',
                failedNodeList: 'nodec xyz,nodea',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                scheduledExecution: seb
        ).save()

        def re = new ReferencedExecution(scheduledExecution: se,execution: exec).save()

        when:
        List l = ReferencedExecution.executionIdList(se)

        then:
        l.size() == 1
        l == [[executionId:1, project:"project1"]]
    }

    def "execution id list with max result"(){
        given:
        def refTotal = 10
        def se = new ScheduledExecution(
                uuid: "000000",
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                refExecCount: refTotal,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def seb = new ScheduledExecution(
                jobName: 'test2',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                refExecCount: refTotal,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def exec = new Execution(
                user: "testuser",
                project: "project1",
                loglevel: 'WARN',
                status: 'FAILED',
                doNodedispatch: true,
                filter:'name: nodea',
                succeededNodeList:'fwnode',
                failedNodeList: 'nodec xyz,nodea',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                scheduledExecution: seb
        ).save()

        def exec2 = new Execution(
                user: "testuser",
                project: "project1",
                loglevel: 'WARN',
                status: 'FAILED',
                doNodedispatch: true,
                filter:'name: nodea',
                succeededNodeList:'fwnode',
                failedNodeList: 'nodec xyz,nodea',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                scheduledExecution: seb
        ).save()

        def re = new ReferencedExecution(scheduledExecution: se,execution: exec).save()
        def re2 = new ReferencedExecution(scheduledExecution: se,execution: exec2).save()

        def executionIdList = [[executionId: exec.id, project: exec.project], [executionId: exec2.id, project: exec2.project]]

        when:
        List l = ReferencedExecution.executionIdList(se, max)

        then:
        l.size() == sizeList
        (0..(sizeList-1)).each {
            l.get(it) == executionIdList.get(it)
        }

        where:
        max  | sizeList
        0    | 2
        1    | 1
        2    | 2
    }

    def "parent list"(){
        given:
        def refTotal = 10
        def se = new ScheduledExecution(
                uuid: "000000",
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                refExecCount: refTotal,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def seb = new ScheduledExecution(
                jobName: 'test2',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                refExecCount: refTotal,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def exec = new Execution(
                user: "testuser",
                project: "project1",
                loglevel: 'WARN',
                status: 'FAILED',
                doNodedispatch: true,
                filter:'name: nodea',
                succeededNodeList:'fwnode',
                failedNodeList: 'nodec xyz,nodea',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                scheduledExecution: seb
        ).save()

        def re = new ReferencedExecution(scheduledExecution: se,execution: exec).save()

        when:
        List l = ReferencedExecution.parentList(se)

        then:
        l.size() == 1
        l.getAt(0).equals(seb)
    }

    def "parent list with max result"(){
        given:
        def refTotal = 10
        def se = new ScheduledExecution(
                uuid: "000000",
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                refExecCount: refTotal,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def seb = new ScheduledExecution(
                jobName: 'test2',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                refExecCount: refTotal,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()

        def seb2 = new ScheduledExecution(
                jobName: 'test3',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                refExecCount: refTotal,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()
        def exec = new Execution(
                user: "testuser",
                project: "project1",
                loglevel: 'WARN',
                status: 'FAILED',
                doNodedispatch: true,
                filter:'name: nodea',
                succeededNodeList:'fwnode',
                failedNodeList: 'nodec xyz,nodea',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                scheduledExecution: seb
        ).save()

        def exec2 = new Execution(
                user: "testuser",
                project: "project1",
                loglevel: 'WARN',
                status: 'FAILED',
                doNodedispatch: true,
                filter:'name: nodea',
                succeededNodeList:'fwnode',
                failedNodeList: 'nodec xyz,nodea',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                scheduledExecution: seb2
        ).save()

        def re = new ReferencedExecution(scheduledExecution: se,execution: exec).save()
        def re2 = new ReferencedExecution(scheduledExecution: se,execution: exec2).save()

        when:
        List l = ReferencedExecution.parentList(se, max)

        then:
        l.size() == sizeList
        l*.toString() == result

        where:
        max  | sizeList | result
        0    | 2        | ["testgroup/test2 - null", "testgroup/test3 - null"]
        1    | 1        | ["testgroup/test2 - null"]
        2    | 2        | ["testgroup/test2 - null", "testgroup/test3 - null"]
    }
}
