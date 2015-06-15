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
        report.ctxProject=='test'
    }

    def "jobref from execution"(){
        given:
        def wf = new Workflow(commands: [new JobExec(jobName: "test job",jobGroup:"a group")])
        def exec = new Execution(
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
        report.ctxProject=='test'
    }

}
