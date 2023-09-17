package rundeck

import grails.test.hibernate.HibernateSpec
import testhelper.RundeckHibernateSpec

class ReferencedExecutionSpec extends RundeckHibernateSpec
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

        def re = new ReferencedExecution(jobUuid: "000000",execution: exec).save()

        when:
        List l = ReferencedExecution.executionProjectList(se.uuid)

        then:
        l.size() == 1
        l == ["project1"]
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
                uuid: UUID.randomUUID().toString(),
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

        def re = new ReferencedExecution(jobUuid: "000000",execution: exec).save()
        def re2 = new ReferencedExecution(jobUuid: "000000",execution: exec2).save()

        def executionIdList = [[executionId: exec.id, project: exec.project], [executionId: exec2.id, project: exec2.project]]

        when:
        List l = ReferencedExecution.executionProjectList(se.uuid, max)

        then:
        l.size() == sizeList
        l == ["project1"]

        where:
        max  | sizeList
        0    | 1
        1    | 1
        2    | 1
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

        def re = new ReferencedExecution(jobUuid: "000000",execution: exec).save()

        when:
        List l = ReferencedExecution.parentJobSummaries(se.uuid)

        then:
        l.size() == 1
        l[0].uuid == seb.uuid
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

        def re = new ReferencedExecution(jobUuid: "000000",execution: exec).save()
        def re2 = new ReferencedExecution(jobUuid: "000000",execution: exec2).save()

        when:
        List l = ReferencedExecution.parentJobSummaries(se.uuid, max)

        then:
        l.size() == sizeList
        l.collect {s-> "${s.groupPath}/${s.jobName} - null".toString()} == result

        where:
        max  | sizeList | result
        0    | 2        | ["testgroup/test2 - null", "testgroup/test3 - null"]
        1    | 1        | ["testgroup/test2 - null"]
        2    | 2        | ["testgroup/test2 - null", "testgroup/test3 - null"]
    }
}
