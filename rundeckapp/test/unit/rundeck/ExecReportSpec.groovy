package rundeck

import grails.test.mixin.Mock
import spock.lang.Specification

/**
 * Created by greg on 6/13/15.
 */
@Mock([Execution,Workflow,CommandExec,JobExec])
class ExecReportSpec extends Specification {

    def "adhoc from execution"(){
        given:
        def wf = new Workflow(commands: [new CommandExec(adhocRemoteString: "test exec")])
        def exec = new Execution(
                dateStarted: new Date(),
                dateCompleted: new Date(),
                failedNodeList: null,
                succeededNodeList: null,
                workflow: wf,
                project: "test",
                user: "user",
                status: 'true'
        ).save(flush:true)


        when:
        def report = ExecReport.fromExec(exec)

        then:
        exec!=null
        report.jcExecId==exec.id.toString()
        report.author=='user'
        report.adhocExecution
        report.adhocScript=='test exec'
        report.ctxProject=='test'
        report.status=='succeed'
    }
    def "adhoc from execution, succeeded"(){
        given:
        def wf = new Workflow(commands: [new CommandExec(adhocRemoteString: "test exec")])
        def exec = new Execution(
                dateStarted: new Date(),
                dateCompleted: new Date(),
                failedNodeList: null,
                succeededNodeList: null,
                workflow: wf,
                project: "test",
                user: "user",
                status: 'succeeded'
        ).save(flush:true)


        when:
        def report = ExecReport.fromExec(exec)

        then:
        exec!=null
        report.jcExecId==exec.id.toString()
        report.author=='user'
        report.adhocExecution
        report.adhocScript=='test exec'
        report.ctxProject=='test'
        report.status=='succeed'
    }

    def "jobref from execution"(){
        given:
        def wf = new Workflow(commands: [new JobExec(jobName: "test job",jobGroup:"a group")])
        def exec = new Execution(
                dateStarted: new Date(),
                dateCompleted: new Date(),
                failedNodeList: null,
                succeededNodeList: null,
                workflow: wf,
                project: "test",
                user: "user",
                status: 'succeeded'
        ).save(flush:true)


        when:
        def report = ExecReport.fromExec(exec)

        then:
        exec!=null
        report.jcExecId==exec.id.toString()
        report.author=='user'
        report.adhocExecution
        report.adhocScript==null
        report.ctxProject=='test'
        report.status=='succeed'
    }

    def "node list split"(String succeeded, String failed, String result){
        given:
        def wf = new Workflow(commands: [new CommandExec(adhocRemoteString: "test exec")])
        def exec = new Execution(
                dateStarted: new Date(),
                dateCompleted: new Date(),
                failedNodeList: failed,
                succeededNodeList: succeeded,
                workflow: wf,
                project: "test",
                user: "user",
                status: 'true'
        ).save(flush:true)


        when:
        def report = ExecReport.fromExec(exec)

        then:
        exec!=null
        report.jcExecId==exec.id.toString()
        report.author=='user'
        report.node==result
        report.adhocExecution
        report.adhocScript=='test exec'
        report.ctxProject=='test'
        report.status=='succeed'

        where:
        succeeded | failed | result
        null | null | "0/0/0"
        "node1" | null | "1/0/1"
        "node1,node2" | null | "2/0/2"
        null | "node1" | "0/1/1"
        null | "node1,node2" | "0/2/2"
        "node1" | "node2" | "1/1/2"
        "node1,node2" | "node3" | "2/1/3"
        "node1" | "node2,node3" | "1/2/3"
    }

    def "no commands"(){
        given:
        def wf = new Workflow()
        def exec = new Execution(
                dateStarted: new Date(),
                dateCompleted: new Date(),
                failedNodeList: null,
                succeededNodeList: null,
                workflow: wf,
                project: "test",
                user: "user",
                status: 'true'
        ).save(flush:true)


        when:
        def report = ExecReport.fromExec(exec)

        then:
        exec!=null
        report.jcExecId==exec.id.toString()
        report.author=='user'
        report.adhocExecution
        report.adhocScript==null
        report.ctxProject=='test'
        report.status=='succeed'
        report.title=='[0 steps]'
    }
}
