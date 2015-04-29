import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.ExecutionService
import rundeck.services.ExecutionServiceException
import rundeck.services.FrameworkService
import rundeck.services.ReportService
import spock.lang.Specification

/**
 * Created by greg on 2/17/15.
 */
@TestFor(ExecutionService)
@Mock([Execution,ScheduledExecution,Workflow,CommandExec])
class ExecutionServiceSpec extends Specification {

    void "retry execution otherwise running"() {

        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                retry:'1'
        )
        job.save()
        def exec = new Execution(
                    scheduledExecution: job,
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user:'userB',
                    project: 'AProject'
        ).save()
        def exec2 = new Execution(
                    scheduledExecution: job,
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user:'user',
                    project: 'AProject'
        ).save()
        service.frameworkService=Stub(FrameworkService){
            getServerUUID() >> null
        }
        when:
        Execution e2=service.createExecution(job,"user1",['extra.option.test':'12'],true,exec2.id)

        then:
        ExecutionServiceException e=thrown()
        e.code=='conflict'
        e.message ==~ /.*is currently being executed.*/
    }
    void "retry execution new execution"() {

        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                retry:'1'
        )
        job.save()
        def exec = new Execution(
                    scheduledExecution: job,
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user:'user',
                    project: 'AProject'
        ).save()
        service.frameworkService=Stub(FrameworkService){
            getServerUUID() >> null
        }
        when:
        Execution e2=service.createExecution(job,"user1",['extra.option.test':'12'],true,exec.id)

        then:
        e2!=null
    }

    def "log execution state"(String statusString, String resultStatus, boolean issuccess,boolean iscancelled,boolean istimedout,boolean willretry) {
        given:
        def params = [:]
        service.reportService = Stub(ReportService) {
            reportExecutionResult(_) >> { args ->
                params = args[0]
            }
        }
        when:
        service.logExecution(
                null,
                'test1',
                'user1',
                issuccess,
                statusString,
                11,
                new Date(),
                'abc',
                'job1',
                'blah',
                iscancelled,
                istimedout,
                willretry,
                '1/1/1',
                null
        )

        then:
        params.jcExecId == 11
        params.jcJobId == 'abc'
        params.reportId == 'job1'
        params.adhocExecution == false
        params.ctxProject == 'test1'
        params.author == 'user1'
        params.title == 'blah'
        params.status == resultStatus
        params.node == '1/1/1'
        params.message == "Job status ${statusString}"
        params.dateStarted != null
        params.dateCompleted != null

        where:
        statusString   | resultStatus | issuccess | iscancelled | istimedout | willretry
        'succeeded'    | 'succeed'    | true      | false       | false      | false
        'true'         | 'succeed'    | true      | false       | false      | false
        'custom'       | 'other'      | false     | false       | false      | false
        'other status' | 'other'      | false     | false       | false      | false
        'false'        | 'fail'       | false     | false       | false      | false
        'failed'       | 'fail'       | false     | false       | false      | false
        'failed'       | 'cancel'     | false     | true        | false      | false
        'failed'       | 'timeout'    | false     | false       | true       | false
        'failed'       | 'retry'      | false     | false       | false      | true
    }
}
